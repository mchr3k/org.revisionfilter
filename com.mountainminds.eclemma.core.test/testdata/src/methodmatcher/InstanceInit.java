package methodmatcher;

public class InstanceInit{

  public String mString = new String("String");

  public InstanceInit() {
    System.out.println("Test");    
  }

  public static void main(String[] args) {
    System.out.println("Test");
  }  
  
}
