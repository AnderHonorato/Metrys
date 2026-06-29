# scripts/treinar_modelo.py
# Criado por Anderson Honorato
# Fine-tuning do Qwen3-0.6B com LoRA (via transformers + PEFT) para CPU
# NUNCA para sozinho - so voce pode cancelar com CTRL+C

import os, sys, time, signal, json
from dotenv import load_dotenv
import torch
from datasets import load_dataset
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    TrainerCallback,
)
from peft import LoraConfig, get_peft_model, PeftModel, TaskType
from trl import SFTTrainer, SFTConfig

load_dotenv()

MODELO_BASE = os.getenv('MODELO_STUDENT', 'Qwen/Qwen3-0.6B')
DATASET_FINAL = 'datasets/dataset_final.jsonl'
DIR_SAIDA = 'modelo_treinado'

USA_GPU = torch.cuda.is_available()
TIPO_PROC = 'GPU' if USA_GPU else 'CPU'
print(f'Hardware detectado: {TIPO_PROC}')

os.makedirs(DIR_SAIDA, exist_ok=True)

# --- Carrega o modelo ---
print(f'Carregando modelo base: {MODELO_BASE}')
tokenizer = AutoTokenizer.from_pretrained(
    MODELO_BASE,
    trust_remote_code=True,
)
tokenizer.pad_token = tokenizer.eos_token

modelo = AutoModelForCausalLM.from_pretrained(
    MODELO_BASE,
    dtype=torch.float32 if not USA_GPU else torch.float16,
    device_map='auto' if USA_GPU else None,
    trust_remote_code=True,
    low_cpu_mem_usage=True,
)

print(f'Modelo carregado. Parametros: {sum(p.numel() for p in modelo.parameters()):,}')

# --- Configura LoRA ---
lora_config = LoraConfig(
    task_type=TaskType.CAUSAL_LM,
    r=16,
    lora_alpha=32,
    lora_dropout=0.05,
    bias='none',
    target_modules=['q_proj', 'k_proj', 'v_proj', 'o_proj',
                    'gate_proj', 'up_proj', 'down_proj'],
)

def encontrar_ultimo_checkpoint(dir_saida):
    if not os.path.isdir(dir_saida):
        return None
    checkpoints = []
    for nome in os.listdir(dir_saida):
        if nome.startswith('checkpoint-') and nome.split('-')[1].isdigit():
            checkpoints.append((int(nome.split('-')[1]), nome))
    if checkpoints:
        checkpoints.sort(reverse=True)
        return os.path.join(dir_saida, checkpoints[0][1])
    return None

checkpoint = encontrar_ultimo_checkpoint(DIR_SAIDA)

if checkpoint:
    print(f'Checkpoint encontrado: {checkpoint} - carregando adaptador...')
    modelo = PeftModel.from_pretrained(modelo, checkpoint, is_trainable=True)
else:
    modelo = get_peft_model(modelo, lora_config)

modelo.print_trainable_parameters()

# --- Carrega dataset ---
if not os.path.exists(DATASET_FINAL):
    print(f'ERRO: {DATASET_FINAL} nao encontrado. Rode mesclar_datasets.py primeiro.')
    sys.exit(1)

dataset_bruto = load_dataset('json', data_files=DATASET_FINAL, split='train')

def formatar_exemplo(exemplo):
    texto = tokenizer.apply_chat_template(
        exemplo['messages'],
        tokenize=False,
        add_generation_prompt=False,
    )
    return {'text': texto}

dataset = dataset_bruto.map(formatar_exemplo, remove_columns=['messages'])
print(f'Dataset carregado: {len(dataset)} exemplos')
print(f'Exemplo formatado:\n{dataset[0]["text"][:300]}...')

# --- Configura treinamento ---
batch_size = 1
gradient_accumulation_steps = 8 if not USA_GPU else 4

args_treino = SFTConfig(
    output_dir=DIR_SAIDA,
    num_train_epochs=2,
    per_device_train_batch_size=batch_size,
    gradient_accumulation_steps=gradient_accumulation_steps,
    learning_rate=2e-4,
    fp16=USA_GPU,
    bf16=False,
    logging_steps=10,
    save_steps=50,
    save_total_limit=3,
    warmup_steps=50,
    lr_scheduler_type='cosine',
    report_to='none',
    dataloader_num_workers=0,
    use_cpu=not USA_GPU,
    optim='adamw_torch',
)

treinador = SFTTrainer(
    model=modelo,
    args=args_treino,
    train_dataset=dataset,
    processing_class=tokenizer,
    formatting_func=lambda x: x['text'],
)

# --- Loop de treinamento com reinicio automatico ---
def treinar_com_reinicio():
    tentativa = 1
    while True:
        try:
            chk = encontrar_ultimo_checkpoint(DIR_SAIDA)
            if chk:
                print(f'\n[Tentativa {tentativa}] Retomando do checkpoint: {chk}')
                treinador.train(resume_from_checkpoint=chk)
            else:
                print(f'\n[Tentativa {tentativa}] Iniciando treinamento do zero...')
                treinador.train()
            print('Treinamento concluido com sucesso!')
            break
        except KeyboardInterrupt:
            print('\nTreinamento cancelado pelo usuario. Progresso salvo.')
            sys.exit(0)
        except Exception as e:
            print(f'\nERRO na tentativa {tentativa}: {e}')
            print('Aguardando 30 segundos e reiniciando...')
            tentativa += 1
            time.sleep(30)

treinar_com_reinicio()

# --- Salva modelo final ---
print('Salvando modelo final (merge LoRA)...')
modelo = modelo.merge_and_unload()
modelo.save_pretrained(DIR_SAIDA)
tokenizer.save_pretrained(DIR_SAIDA)
print(f'Modelo salvo em: {DIR_SAIDA}/')
