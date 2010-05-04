package svndiffmanager;

public class StructuralDiff1 {

  enum NewEnum
  {
    ELEMENT
  }

  enum ExistingEnum
  {
    EXISTINGELEMENT,
    NEWELEMENT;

    public static class ExistingEnumClass
    {
      public void existingEnumClassMethod()
      {
        System.out.println("test");
      }

      public void newEnumClassMethod()
      {
        System.out.println("test");
      }
    }

    public static class NewEnumClass
    {
      public void newnewEnumClassMethod()
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
        return newHelper();
      }

      private String newHelper() {
        return existing() + "test";
      }
    };
  }

}
