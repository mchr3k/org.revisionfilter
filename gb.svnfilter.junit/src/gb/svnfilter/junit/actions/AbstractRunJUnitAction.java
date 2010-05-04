package gb.svnfilter.junit.actions;

import gb.svnfilter.junit.Activator;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Common base class containing code for running a set of tests.
 */
public class AbstractRunJUnitAction extends Action
                                    implements ISelectionChangedListener {

  /**
   * Current Selection
   */
  protected ISelection currentSelection;

  @Override
  public void selectionChanged(SelectionChangedEvent xiEvent) {
    currentSelection = xiEvent.getSelection();
  }

  /**
   * @param selectedTypes Run the selected test types.
   */
  protected void runTests(IJavaProject javaProject,
                          Set<IType> selectedTypes)
  {
    if (!selectedTypes.isEmpty())
    {
      IJavaElement element = null;
      if (selectedTypes.size() == 1)
      {
        element = selectedTypes.iterator().next();
      }
      else
      {
        // Generate a test file
        try {
          final IPackageFragmentRoot root = createSourceFolder(javaProject, "gentestsrc");
          String srcData =
          "package temp.generatedtests;\n" +
          "import junit.framework.Test;\n" +
          "import junit.framework.TestCase;\n" +
          "import junit.framework.TestSuite;\n" +
          "/**\n" +
          " * WARNING: This is an auto-generated source file.\n" +
          " */\n" +
          "public class SelectedTests extends TestCase {\n" +
          "  /**\n" +
          "   * @return A suite which runs a particular set of other suites.\n" +
          "   */\n" +
          "  public static Test suite()\n" +
          "  {\n" +
          "    TestSuite suite = new TestSuite(\"temp.generatedtests.SelectedTests\");\n";

          for (IType type : selectedTypes)
          {
            srcData += "    suite.addTest(new TestSuite(" + type.getFullyQualifiedName() + ".class));\n";
          }

          srcData +=
          "    return suite;\n" +
          "  }\n" +
          "}\n";
          Reader source = new StringReader(srcData);
          element = createCompilationUnit(javaProject, root, "temp.generatedtests", "SelectedTests.java", source);
        } catch (Exception e) {
          IStatus status = new Status(IStatus.ERROR,
                                      Activator.PLUGIN_ID,
                                      "Exception when generating test class.",
                                      e);
          StatusManager.getManager().handle(status);
        }
      }

      IStructuredSelection generatedSelection = new StructuredSelection(element);
      new JUnitLaunchShortcut().launch(generatedSelection, ILaunchManager.RUN_MODE);
    }
  }

  private IPackageFragmentRoot createSourceFolder(IJavaProject javaProject, String foldername)
      throws CoreException {
    IFolder folder = javaProject.getProject().getFolder(foldername);
    if (!folder.exists())
    {
      folder.create(false, true, new NullProgressMonitor());
    }
    IPackageFragmentRoot packageRoot = javaProject.findPackageFragmentRoot(folder.getFullPath());
    if (packageRoot == null)
    {
      packageRoot = javaProject.getPackageFragmentRoot(folder);
      addClassPathEntry(javaProject, JavaCore.newSourceEntry(packageRoot.getPath()));
    }
    return packageRoot;
  }

  private void addClassPathEntry(IJavaProject javaProject, IClasspathEntry entry) throws CoreException {
    IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
    IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
    System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
    newEntries[oldEntries.length] = entry;
    javaProject.setRawClasspath(newEntries, null);
  }

  private ICompilationUnit createCompilationUnit(
      IJavaProject javaProject,
      IPackageFragmentRoot fragmentRoot,
      String packagePath,
      String name,
      Reader source)
      throws CoreException, IOException {
    IPackageFragment searchFrag = fragmentRoot.getPackageFragment(packagePath);
    IPackageFragment fragment = javaProject.findPackageFragment(searchFrag.getPath());
    if (fragment == null)
    {
      fragment = fragmentRoot.createPackageFragment(packagePath, false, null);
    }
    StringBuffer sb = new StringBuffer();
    int c;
    while ((c = source.read()) != -1)
      sb.append((char) c);
    source.close();
    ICompilationUnit compUnit = fragment.getCompilationUnit(name);
    if (!compUnit.exists())
    {
      return fragment.createCompilationUnit(name, sb.toString(), false, null);
    }

    IBuffer compBuffer = compUnit.getBuffer();
    compBuffer.setContents(sb.toString());
    compUnit.save(new NullProgressMonitor(), true);
    compUnit.commitWorkingCopy(true, new NullProgressMonitor());
    compUnit.close();
    return compUnit;
  }
}
