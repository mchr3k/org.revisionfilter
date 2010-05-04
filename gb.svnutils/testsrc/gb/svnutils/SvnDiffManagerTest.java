package gb.svnutils;

import java.io.InputStream;
import java.util.Stack;

import junit.framework.TestCase;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.swt.graphics.Image;

/**
 * UTs for SvnDiffManager
 */
public class SvnDiffManagerTest extends TestCase
{
  public void testGetStructuralElements() throws Exception
  {
    final String fullName = "svndiffmanager/StructuralElement";
    JavaProjectKit jproject = setupProject("testsrc", "StructuralElement", fullName);
    IType type = jproject.javaProject.findType("svndiffmanager.StructuralElement");
    assertNotNull(type);

    String[] expectedResults = new String[]
    {
        // The following list is of the form
        // - Expected element name
        // - Comma seperated structural element names
        "mAnonObjField",
        "StructuralElement,mAnonObjField",
        "",
        "StructuralElement,mAnonObjField",
        "toString",
        "StructuralElement,mAnonObjField",
        "anonMethod",
        "StructuralElement,mAnonObjField",
        "EnumElement",
        "StructuralElement,EnumElement",
        "ONE",
        "StructuralElement,EnumElement,ONE",
        "TWO",
        "StructuralElement,EnumElement,TWO",
        "normalMethod",
        "StructuralElement,normalMethod",
        "InnerMethodClass",
        "StructuralElement,normalMethod",
        "innerMethodClassMethod",
        "StructuralElement,normalMethod",
        "parentMethod",
        "StructuralElement,parentMethod",
        "",
        "StructuralElement,parentMethod",
        "toString",
        "StructuralElement,parentMethod",
        "anonParentMethod",
        "StructuralElement,parentMethod",
        "InnerClass",
        "StructuralElement,InnerClass",
        "innerClassMethod",
        "StructuralElement,InnerClass,innerClassMethod",
        "InnerEnum",
        "StructuralElement,InnerClass,InnerEnum",
        "ELEMENT",
        "StructuralElement,InnerClass,InnerEnum,ELEMENT",
        "InnerEnumClass",
        "StructuralElement,InnerClass,InnerEnum,InnerEnumClass",
        "innerEnumClassMethod",
        "StructuralElement,InnerClass,InnerEnum,InnerEnumClass,innerEnumClassMethod",
    };

    int ii = 0;
    SvnDiffManager diffManager = new SvnDiffManager();
    traverseAndCheckStructuralElements(type, diffManager, expectedResults, ii);
  }

  private int traverseAndCheckStructuralElements(IParent typeP, SvnDiffManager diffManager, String[] expectedResults, int ii) throws Exception
  {
    for (IJavaElement child : typeP.getChildren())
    {
      assertEquals(ii + ":" + expectedResults[ii] + " => " + child.getElementName(), expectedResults[ii], child.getElementName());
      ii++;
      Stack<IJavaElement> structuralChildren = diffManager.getStructuralElements(child);
      String str = toStringChildren(structuralChildren);
      assertEquals(ii + ":" + expectedResults[ii] + " => " + str, expectedResults[ii], str);
      ii++;
      if (child instanceof IParent)
      {
        ii = traverseAndCheckStructuralElements((IParent)child, diffManager, expectedResults, ii);
      }
    }
    return ii;
  }

  public void testStructuralDiff() throws Exception
  {
    // Setup project
    String lFolder1 = "testsrc";
    String lFolder2 = "testdata";
    String lClassName1 = "StructuralDiff1";
    String lFilename2 = "StructuralDiff2.java";
    String fullName = "svndiffmanager/" + lClassName1;
    JavaProjectKit javaProject = setupProject(lFolder1, lClassName1, fullName);
    IType type = javaProject.javaProject.findType("svndiffmanager.StructuralDiff1");
    {
      IFile testFile = javaProject.project.getFile(lFilename2);
      InputStream source = javaProject.openTestResource(new Path(lFolder2).append(lFilename2));
      testFile.create(source, true, new NullProgressMonitor());
    }

    // Get resources to compare
    final IResource res1 = type.getResource();
    IFile file1 = (IFile)res1;
    assertNotNull(file1);
    final IResource res2 = javaProject.project.findMember(lFilename2);
    IFile file2 = (IFile)res2;
    assertNotNull(file2);

    // Convert to diff input
    DiffInput input1 = new DiffInput(res1, file1);
    DiffInput input2 = new DiffInput(res2, file2);

    // Run structural diff
    DiffNode structDiff = SvnDiffManager.doStructuralDiff(input1, input2);
    assertNotNull(structDiff);

    // Create a diff manager
    SvnDiffManager manager = new SvnDiffManager();

    String[] expectedResults = new String[]
    {
        "NewEnum -> true",
        "ELEMENT -> true",
        "ExistingEnum -> true",
        "EXISTINGELEMENT -> false",
        "NEWELEMENT -> true",
        "ExistingEnumClass -> true",
        "existingEnumClassMethod -> false",
        "newEnumClassMethod -> true",
        "NewEnumClass -> true",
        "newnewEnumClassMethod -> true",
        "existingMethod -> true",
        " -> true",
        // Structural diff does not expand elements within fields, methods or
        // initializers therefore, every child of a dirty field, method or
        // initializer appears structurally dirty
        "existing -> true",
        "toString -> true",
        "newHelper -> true",
    };

    // Traverse selected type and check results
    traverseAndCheckStructuralDiff(type, structDiff, manager, expectedResults, 0);
  }

  private int traverseAndCheckStructuralDiff(IParent xiType, DiffNode xiStructDiff, SvnDiffManager xiManager, String[] xiExpectedResults, int ii) throws Exception
  {
    for (IJavaElement child : xiType.getChildren())
    {
      String result = xiExpectedResults[ii];
      String[] resultParts = result.split(" -> ");
      assertEquals(failMessage(ii, resultParts[0], child.getElementName()),
                   resultParts[0], child.getElementName());
      Stack<IJavaElement> possibleElements = xiManager.getStructuralElements(child);
      boolean isDirty = xiManager.isStructurallyDifferent(possibleElements, xiStructDiff);
      assertEquals(failMessage(ii, resultParts[1], Boolean.toString(isDirty)),
                   resultParts[1], Boolean.toString(isDirty));

      ii++;

      if (child instanceof IParent)
      {
        ii = traverseAndCheckStructuralDiff((IParent)child, xiStructDiff, xiManager, xiExpectedResults, ii);
      }
    }
    return ii;
  }

  private String failMessage(int xiIi, String xiString, String xiElementName) {
    return xiIi + " : expected : " + xiString + ", got : " + xiElementName;
  }

  private class DiffInput implements IStreamContentAccessor, ITypedElement
  {
    public DiffInput(IResource xiRes, IFile xiFile) {
      super();
      res = xiRes;
      file = xiFile;
    }

    final IResource res;
    final IFile file;

    @Override
    public String getType() {
      String name = res.getName();
      return name.substring(name.lastIndexOf('.') + 1);
    }

    @Override
    public String getName() {
      return res.getName();
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public InputStream getContents() throws CoreException {
      return file.getContents();
    }
  }

  private String toStringChildren(Stack<IJavaElement> xiStructuralChildren) {
    String ret = "";
    while (!xiStructuralChildren.empty())
    {
      IJavaElement element = xiStructuralChildren.pop();
      ret += element.getElementName();
      if (!xiStructuralChildren.isEmpty())
      {
        ret += ",";
      }
    }
    return ret;
  }

  private JavaProjectKit setupProject(String xiFolder, String xiClassName, String xiLongName)
      throws Exception
  {
    // Setup and build the project
    JavaProjectKit javaProject = new JavaProjectKit(xiClassName);
    javaProject.enableJava5();
    final IPackageFragmentRoot root = javaProject.createSourceFolder("src");
    javaProject.createCompilationUnit(root, xiFolder, xiLongName + ".java");
    JavaProjectKit.waitForBuild();
    javaProject.assertNoErrors();
    return javaProject;
  }
}
