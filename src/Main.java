import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.HashMap;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
      try{
          var procesador =new ProcesadorDeAnotacoes();
          procesador.procesar(new ClasseAnotada());
          ArrayList<CsvEscritor> escritores = new ArrayList<>();
          escritores.add(new CsvEscritor("Ramon",28,"Produtor"));
          escritores.add(new CsvEscritor("Adalberto",28,"Produtor"));
          escritores.add(new CsvEscritor("Gilmar",28,"Produtor"));
          escritores.add(new CsvEscritor("Gilmar2222",28,"Produtor"));

          for(var c:escritores){
              System.out.println(procesador.escreverScv(c));
          }
          var injetor =new InjetorDeDependencias(new ContainerObjeto());
          injetor.printMetodosFabrica();
          System.out.println("///------------------------///////");d
          injetor.printSingletomMetodos();
          /**pegando um procesador e prosesando o csv 3*/
          var objeto=(ProcesadorDeAnotacoes)injetor.get(ProcesadorDeAnotacoes.class);
          System.out.println(objeto.escreverScv(escritores.get(2)));
          var pessoa =(Pessoa)injetor.get(Pessoa.class);
          pessoa.printNome();
          System.out.println("///----------------------------/////////");
          var pesosa2=(Pessoa2)injetor.get(Pessoa2.class);
          System.out.println("pesosa 2 aponta para esse endereco "+pesosa2);
          pesosa2.printNome();
      }catch (Exception e){
       e.printStackTrace();
      }
    }
}
@Csv
class CsvEscritor{
    @Dado
    public String nome;
    @Dado
    public Integer idade;
    @Dado
    public String categoria;

    public CsvEscritor(String nome, Integer idade, String categoria) {
        this.nome = nome;
        this.idade = idade;
        this.categoria = categoria;
    }
}
@Container
class ContainerObjeto{
    @Fabrica(fabricar = String.class)
    public String nome(){
        return "ramon";
    }
    @Singleton(classe = ProcesadorDeAnotacoes.class)
    public ProcesadorDeAnotacoes procesador(){
        return new ProcesadorDeAnotacoes();
    }

    @Singleton(classe = Pessoa.class)
    public Pessoa fabricaPessoa(String n){
        return new Pessoa(n);
    }

}
@Injecao(injetar = String.class)
class Pessoa{
    String nome;
    @InjecaoConstrutor
    public Pessoa(String nome){
        this.nome=nome;
    }
    public void printNome(){
        System.out.println("Ola eu me chama "+this.nome+" eu sou uma pessoa ");
    }
}
@Injecao(injetar = String.class)
class Pessoa2{
    String nome;
    @InjecaoConstrutor
    public Pessoa2(String nome){
        this.nome=nome;
    }
    public void printNome(){
        System.out.println("Ola eu me chama "+this.nome+" eu sou uma pessoa ");
    }
}
class ProcesadorDeAnotacoes{
    public void procesar(Object o)throws Exception{
        var classe =o.getClass();
        var metodos=classe.getDeclaredMethods();
        for(var m : metodos){
            if(m.isAnnotationPresent(PrintHelo.class)){
                System.out.println("Hola metodo ....");
                m.invoke(o);
            }

        }
    }

    public String escreverScv(Object csv) throws IllegalAccessException {
        var clase =csv.getClass();
        if(!clase.isAnnotationPresent(Csv.class)){
            throw new RuntimeException("Objeto nao e reconhecido como um objeto CSV ");

        }
        var variaveis=clase.getDeclaredFields();
        StringBuilder sb = new StringBuilder();

        for(int i=0;i< variaveis.length;i++){
            if(variaveis[i].isAnnotationPresent(Dado.class)){
                var variavel =variaveis[i].get(csv);
                sb.append(variavel);
                if(i!=(variaveis.length-1)){
                    sb.append(";");
                }
            }
        }
       return  sb.toString();
    }

}

class InjetorDeDependencias{
     private HashMap<Class<?>,Object> Singleton = new HashMap<>();

     private HashMap<Class<?>, Method> fabricas = new HashMap<>();
     Object container;
    public InjetorDeDependencias(Object o) throws InvocationTargetException, IllegalAccessException {
        var clase =o.getClass();
        if(clase.isAnnotationPresent(Container.class)){
         container =o;
         var metodos =clase.getDeclaredMethods();
          for(var m : metodos){
              if(m.isAnnotationPresent(Fabrica.class)){
                  var anotacao=m.getAnnotation(Fabrica.class);
                  fabricas.put(anotacao.fabricar(),m);
              }else if (m.isAnnotationPresent(Singleton.class)) {
                  fabricas.put(m.getAnnotation(Singleton.class).classe(), m);
              }
          }



        }
        else throw new RuntimeException("nao e posivel usar esse comtainer");
    }

    public Object get(Class<?> classe){
        try{
            if(!this.Singleton.containsKey(classe)){
                var fabrica= this.fabricas.get(classe);
                if(fabrica!=null){

                    var metodosDeClarados=fabrica.getParameterTypes();
                    Object objeto;
                    if(metodosDeClarados.length==0) objeto =fabrica.invoke(container);
                    else {
                        var metododprodusidos=new Object[metodosDeClarados.length];
                        for (int i=0;i<metodosDeClarados.length;i++){
                            metododprodusidos[i]=this.fabricas.get(metodosDeClarados[i]).invoke(container);
                        }
                        objeto=fabrica.invoke(container,metododprodusidos);
                    }if(fabrica.isAnnotationPresent(Singleton.class)){

                        this.Singleton.put(classe,objeto);
                    }
                    return objeto;
                }else {
                   if(classe.isAnnotationPresent(Injecao.class)){
                       var comstrutoree= classe.getDeclaredConstructors();
                       for(var c:comstrutoree){
                           if(c.isAnnotationPresent(InjecaoConstrutor.class)){
                               var paramentros=c.getGenericParameterTypes();
                               if(paramentros.length==0) return c.newInstance();
                               var paramentrosConstruidos=new Object[paramentros.length];
                               for(int i=0;i<paramentros.length;i++){
                                   paramentrosConstruidos[i]=get((Class<?>) paramentros[i]);
                               }
                               return c.newInstance(paramentrosConstruidos);
                           }

                       }
                   }
                       throw  new RuntimeException("Nao foi posivel comstruir a classe pois ela nao e Marcada com a anotacao @Inject e ela nao posui comstrutores vasios");




                }



            }
            return this.Singleton.get(classe);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public void printMetodosFabrica(){
        try{
            var classe =container.getClass();
            var metodos =classe.getDeclaredMethods();
            for(var m : metodos){
                if(m.isAnnotationPresent(Fabrica.class)){
                    var fabrica =m.getAnnotation(Fabrica.class);
                    System.out.println("metodo fabrica "+m.getName()+" fabrica "+fabrica.fabricar().getName());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        System.out.println("ouve um erro ao listar os metodos fabrica");
        }
    }

    public void printSingletomMetodos(){
        try{
            var classe =container.getClass();
            var metodos =classe.getDeclaredMethods();
            for(var m : metodos){
                if(m.isAnnotationPresent(Singleton.class)){
                    var fabrica =m.getAnnotation(Singleton.class);
                    System.out.println("metodo singleton "+m.getName()+" fabrica "+fabrica.classe().getName());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("ouve um erro ao listar os metodos fabrica");
        }
    }

}
class ClasseAnotada{
    @PrintHelo
    public void meuMetodo(){

        System.out.println("mew metodo esta procesandao ....");
    }
}
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface PrintHelo{}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Csv{

}
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Dado{}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Container{}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Produtor{
    Class<?> classe() default  Void.class;

}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Injecao{
    Class<?> injetar()  ;
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
@interface InjecaoConstrutor{

}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Singleton{
    Class<?> classe() ;
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Fabrica{
   Class<?> fabricar() ;
}