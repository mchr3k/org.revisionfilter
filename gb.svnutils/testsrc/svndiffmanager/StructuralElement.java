package svndiffmanager;

public class StructuralElement
{
  public Object mAnonObjField = new Object()
  {
    public String toString()
    {
      return anonMethod();
    }

    private String anonMethod()
    {
      return "anonToString";
    };
  };

  public enum EnumElement
  {
    ONE,
    TWO,
  }

  public void normalMethod()
  {
    System.out.println("method");

    class InnerMethodClass
    {
      public void innerMethodClassMethod()
      {
        System.out.println("foo");
      }
    }
  }

  public void parentMethod()
  {
    Object anonMethodObj = new Object()
    {
      public String toString()
      {
        return anonParentMethod();
      }

      private String anonParentMethod()
      {
        return "anonToString";
      };
    };
    System.out.println(anonMethodObj);
  }

  public static class InnerClass
  {
    public void innerClassMethod()
    {
      System.out.println("innerClass");
    }

    enum InnerEnum
    {
      ELEMENT;

      public static class InnerEnumClass
      {
        public void innerEnumClassMethod()
        {
          System.out.println("innerEnumClassMethod");
        }
      }
    }
  }
}
