package svndiffmanager;

public class StructuralDiff1 {
  
  enum ExistingEnum
  {
    EXISTINGELEMENT;
    
    public static class ExistingEnumClass
    {
      public void existingEnumClassMethod()
      {
        System.out.println("test");
      }
    }
  }
  
  public void existingMethod()
  {
    Object anonObject = new Object()
    {
      private String existing()
      {
        return "existing";
      }
      
      @Override
      public String toString() {
        return existing() + "foo";
      }
    };
  }
}
