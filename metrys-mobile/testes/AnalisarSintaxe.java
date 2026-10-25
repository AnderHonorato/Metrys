import java.util.*;import java.io.*;import javax.tools.*;import com.sun.source.util.*;
public class AnalisarSintaxe{
 public static void main(String[]args)throws Exception{
  JavaCompiler comp=ToolProvider.getSystemJavaCompiler();DiagnosticCollector<JavaFileObject> diags=new DiagnosticCollector<>();StandardJavaFileManager gerente=comp.getStandardFileManager(diags,null,null);
  List<File> arquivos=new ArrayList<>();for(String caminho:args)arquivos.add(new File(caminho));
  JavacTask job=(JavacTask)comp.getTask(null,gerente,diags,Arrays.asList("-proc:none"),null,gerente.getJavaFileObjectsFromFiles(arquivos));job.parse();
  int erros=0;for(Diagnostic<? extends JavaFileObject> d:diags.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR){System.out.println(d);erros++;}
  System.out.println("Arquivos Java analisados: "+arquivos.size()+", erros de sintaxe: "+erros);if(erros>0)System.exit(1);
 }
}
