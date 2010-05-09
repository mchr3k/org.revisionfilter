package com.mountainminds.eclemma.internal.core.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.mountainminds.eclemma.core.CoverageTools;
import com.mountainminds.eclemma.core.ICoverageSession;
import com.mountainminds.eclemma.core.IInstrumentation;
import com.mountainminds.eclemma.core.JavaProjectKit;
import com.mountainminds.eclemma.core.analysis.IJavaCoverageListener;
import com.mountainminds.eclemma.core.launching.JavaApplicationLauncher;
import com.vladium.emma.data.ClassDescriptor;
import com.vladium.emma.data.DataFactory;
import com.vladium.emma.data.ICoverageData;
import com.vladium.emma.data.IMergeable;
import com.vladium.emma.data.IMetaData;
import com.vladium.emma.data.MethodDescriptor;
import com.vladium.emma.data.ICoverageData.DataHolder;

public class MethodMatcherTest extends TestCase {

  private JavaProjectKit javaProject;

  protected void setUp() throws Exception {
    super.setUp();
    CoverageTools.getSessionManager().removeAllSessions();
  }

  private TestData setupProject(String xiClassName, String xiLongName)
      throws Exception {
    // Setup and build the project
    javaProject = new JavaProjectKit(xiClassName);
    javaProject.enableJava5();
    final IPackageFragmentRoot root = javaProject.createSourceFolder("src");
    javaProject.createCompilationUnit(root, "testdata/src", xiLongName
        + ".java");
    JavaProjectKit.waitForBuild();
    javaProject.assertNoErrors();

    // Move the launch file into place
    File launchFileTarget = new File("../../junit-workspace/" + xiClassName
        + "/" + xiClassName + ".launch");

    // Copy the launch file into place
    String launchString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
        + "<launchConfiguration type=\"org.eclipse.jdt.launching.localJavaApplication\">"
        + "<listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\">"
        + "<listEntry value=\"/testdata/src/"
        + xiLongName
        + ".java\"/>"
        + "</listAttribute>"
        + "<listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_TYPES\">"
        + "<listEntry value=\"1\"/>"
        + "</listAttribute>"
        + "<stringAttribute key=\"org.eclipse.jdt.launching.MAIN_TYPE\" value=\""
        + xiLongName.replace('/', '.')
        + "\"/>"
        + "<stringAttribute key=\"org.eclipse.jdt.launching.PROJECT_ATTR\" value=\""
        + xiClassName + "\"/>" + "</launchConfiguration>";
    StringReader in = new StringReader(launchString);
    FileWriter out = new FileWriter(launchFileTarget);
    int c;
    while ((c = in.read()) != -1)
      out.write(c);
    in.close();
    out.close();
    assertTrue(launchFileTarget.getCanonicalPath(), launchFileTarget.exists());

    // Refresh the project to pickup the launch file
    javaProject.project.refreshLocal(IResource.DEPTH_INFINITE,
        new NullProgressMonitor());

    // Attach coverage listener
    CoverageListener listener = new CoverageListener();
    CoverageTools.addJavaCoverageListener(listener);

    // Launch the program in coverage mode
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    IPath path = new Path(xiClassName + ".launch");
    IFile file = javaProject.project.getFile(path);
    ILaunchConfiguration launchConfig = launchManager
        .getLaunchConfiguration(file);
    JavaApplicationLauncher javaLauncher = new JavaApplicationLauncher();
    javaLauncher.setInitializationData(new Config(), null, null);
    ILaunch launch = javaLauncher.getLaunch(launchConfig, "");
    javaLauncher.launch(launchConfig, "", launch, new NullProgressMonitor());

    // Get the coverage session
    listener.waitForCoverageChanged();
    ICoverageSession[] sessions = CoverageTools.getSessionManager()
        .getSessions();
    assertEquals(1, sessions.length);
    ICoverageSession session = sessions[0];

    // Get the instrumentation data
    assertEquals(1, session.getCoverageDataFiles().length);
    IInstrumentation[] instrumentations = session.getInstrumentations();
    assertEquals(1, instrumentations.length);
    IInstrumentation instrumentation = instrumentations[0];

    // Get the coverage data
    File f = session.getCoverageDataFiles()[0].toFile();
    assertTrue(f.exists());
    IMergeable data = DataFactory.load(f)[DataFactory.TYPE_COVERAGEDATA];
    final ICoverageData coveragedata = (ICoverageData) data;
    assertNotNull(coveragedata);

    // Get the metadata
    IPath metadatafile = instrumentation.getMetaDataFile();
    File metadataf = metadatafile.toFile();
    assertTrue(metadataf.exists());
    IMetaData metadata = (IMetaData) DataFactory.load(metadataf)[DataFactory.TYPE_METADATA];
    assertNotNull(metadata);

    // Extract the type descriptors from the metadata
    final Map descriptors = new HashMap();
    for (Iterator i = metadata.iterator(); i.hasNext();) {
      ClassDescriptor cd = (ClassDescriptor) i.next();
      descriptors.put(cd.getClassVMName(), cd);
    }

    return new TestData(descriptors, instrumentation, coveragedata);
  }

  private static class TestData {
    public TestData(Map descriptors, IInstrumentation instrumentation,
        ICoverageData coveragedata) {
      this.descriptors = descriptors;
      this.instrumentation = instrumentation;
      this.coveragedata = coveragedata;
    }

    public final Map descriptors;
    public final IInstrumentation instrumentation;
    public final ICoverageData coveragedata;
  }

  public void testImplicitConstructor() throws Exception {
    final String fullName = "methodmatcher/ImplicitConstructor";
    final TestData testData = setupProject("ImplicitConstructor", fullName);

    // Visit the types
    IPackageFragmentRoot[] roots = testData.instrumentation.getClassFiles()
        .getPackageFragmentRoots();
    TypeTraverser jep = new TypeTraverser(roots);
    jep.process(new ITypeVisitor() {

      public void visit(IType type, String binaryname) {
        try {
          // Test the name of the type
          assertEquals(fullName, binaryname);

          // Test the number of child IJavaElements
          assertEquals(1, type.getChildren().length);

          // Get the Type Descriptor
          ClassDescriptor classDesc = (ClassDescriptor) testData.descriptors
              .remove(binaryname);
          assertNotNull(classDesc);

          // Get the coveragedata
          DataHolder data = testData.coveragedata.getCoverage(classDesc);
          assertNotNull(data);
          boolean[][] covered = data.m_coverage;

          // Extract the method descriptors
          MethodDescriptor[] methods = classDesc.getMethods();
          assertEquals(2, methods.length);

          // Construct a matcher
          MethodMatcher matcher = new MethodMatcher(type, covered, methods);

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("main", element.getElementName());
            assertEquals("main", mdescriptor.getName());
          }

          assertFalse(matcher.methodMatched());

        } catch (JavaModelException e) {
          throw new RuntimeException(e);
        }

      }

      public void done() {
        // Do Nothing
      }
    }, new NullProgressMonitor());
  }

  public void testExplicitConstructor() throws Exception {
    final String fullName = "methodmatcher/ExplicitConstructor";
    final TestData testData = setupProject("ExplicitConstructor", fullName);

    // Visit the types
    IPackageFragmentRoot[] roots = testData.instrumentation.getClassFiles()
        .getPackageFragmentRoots();
    TypeTraverser jep = new TypeTraverser(roots);
    jep.process(new ITypeVisitor() {

      public void visit(IType type, String binaryname) {
        try {
          // Test the name of the type
          assertEquals(fullName, binaryname);

          // Test the number of child IJavaElements
          assertEquals(2, type.getChildren().length);

          // Get the Type Descriptor
          ClassDescriptor classDesc = (ClassDescriptor) testData.descriptors
              .remove(binaryname);
          assertNotNull(classDesc);

          // Get the coveragedata
          DataHolder data = testData.coveragedata.getCoverage(classDesc);
          assertNotNull(data);
          boolean[][] covered = data.m_coverage;

          // Extract the method descriptors
          MethodDescriptor[] methods = classDesc.getMethods();
          assertEquals(2, methods.length);

          // Construct a matcher
          MethodMatcher matcher = new MethodMatcher(type, covered, methods);

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("ExplicitConstructor", element.getElementName());
            assertEquals("<init>", mdescriptor.getName());
          }

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("main", element.getElementName());
            assertEquals("main", mdescriptor.getName());
          }

          assertFalse(matcher.methodMatched());

        } catch (JavaModelException e) {
          throw new RuntimeException(e);
        }

      }

      public void done() {
        // Do Nothing
      }
    }, new NullProgressMonitor());
  }

  public void testStaticInit() throws Exception {
    final String fullName = "methodmatcher/StaticInit";
    final TestData testData = setupProject("StaticInit", fullName);

    // Visit the types
    IPackageFragmentRoot[] roots = testData.instrumentation.getClassFiles()
        .getPackageFragmentRoots();
    TypeTraverser jep = new TypeTraverser(roots);
    jep.process(new ITypeVisitor() {

      public void visit(IType type, String binaryname) {
        try {
          // Test the name of the type
          assertEquals(fullName, binaryname);

          // Test the number of child IJavaElements
          assertEquals(2, type.getChildren().length);

          // Get the Type Descriptor
          ClassDescriptor classDesc = (ClassDescriptor) testData.descriptors
              .remove(binaryname);
          assertNotNull(classDesc);

          // Get the coveragedata
          DataHolder data = testData.coveragedata.getCoverage(classDesc);
          assertNotNull(data);
          boolean[][] covered = data.m_coverage;

          // Extract the method descriptors
          MethodDescriptor[] methods = classDesc.getMethods();
          assertEquals(3, methods.length);

          // Construct a matcher
          MethodMatcher matcher = new MethodMatcher(type, covered, methods);

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("", element.getElementName());
            assertTrue(element.toString(), element.toString().startsWith(
                "<static initializer #1>"));
            assertEquals("<clinit>", mdescriptor.getName());
          }

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("main", element.getElementName());
            assertEquals("main", mdescriptor.getName());
          }

          assertFalse(matcher.methodMatched());

        } catch (JavaModelException e) {
          throw new RuntimeException(e);
        }

      }

      public void done() {
        // Do Nothing
      }
    }, new NullProgressMonitor());
  }

  public void testComplexTest() throws Exception {
    final String fullName = "methodmatcher/ComplexTest";
    final TestData testData = setupProject("ComplexTest", fullName);

    // Visit the types
    IPackageFragmentRoot[] roots = testData.instrumentation.getClassFiles()
        .getPackageFragmentRoots();
    TypeTraverser jep = new TypeTraverser(roots);
    jep.process(new ITypeVisitor() {

      public void visit(IType type, String binaryname) {
        try {
          // Test the name of the type
          assertEquals(fullName, binaryname);

          // Test the number of child IJavaElements
          assertEquals(7, type.getChildren().length);

          // Get the Type Descriptor
          ClassDescriptor classDesc = (ClassDescriptor) testData.descriptors
              .remove(binaryname);
          assertNotNull(classDesc);

          // Get the coveragedata
          DataHolder data = testData.coveragedata.getCoverage(classDesc);
          assertNotNull(data);
          boolean[][] covered = data.m_coverage;

          // Extract the method descriptors
          MethodDescriptor[] methods = classDesc.getMethods();
          assertEquals(4, methods.length);

          // Construct a matcher
          MethodMatcher matcher = new MethodMatcher(type, covered, methods);

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("", element.getElementName());
            assertTrue(element.toString(), element.toString().startsWith(
                "<static initializer #1>"));
            assertEquals("<clinit>", mdescriptor.getName());
          }

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("ComplexTest", element.getElementName());
            assertEquals("<init>", mdescriptor.getName());
          }

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("instanceMethod", element.getElementName());
            assertEquals("instanceMethod", mdescriptor.getName());
          }

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("main", element.getElementName());
            assertEquals("main", mdescriptor.getName());
          }

          assertFalse(matcher.methodMatched());

        } catch (JavaModelException e) {
          throw new RuntimeException(e);
        }

      }

      public void done() {
        // Do Nothing
      }
    }, new NullProgressMonitor());
  }

  public void testMultipleStaticInit() throws Exception {
    final String fullName = "methodmatcher/MultipleStaticInit";
    final TestData testData = setupProject("MultipleStaticInit", fullName);

    // Visit the types
    IPackageFragmentRoot[] roots = testData.instrumentation.getClassFiles()
        .getPackageFragmentRoots();
    TypeTraverser jep = new TypeTraverser(roots);
    jep.process(new ITypeVisitor() {

      public void visit(IType type, String binaryname) {
        try {
          // Test the name of the type
          assertEquals(fullName, binaryname);

          // Test the number of child IJavaElements
          assertEquals(4, type.getChildren().length);

          // Get the Type Descriptor
          ClassDescriptor classDesc = (ClassDescriptor) testData.descriptors
              .remove(binaryname);
          assertNotNull(classDesc);

          // Get the coveragedata
          DataHolder data = testData.coveragedata.getCoverage(classDesc);
          assertNotNull(data);
          boolean[][] covered = data.m_coverage;

          // Extract the method descriptors
          MethodDescriptor[] methods = classDesc.getMethods();
          assertEquals(3, methods.length);

          // Construct a matcher
          MethodMatcher matcher = new MethodMatcher(type, covered, methods);

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("", element.getElementName());
            assertTrue(element.toString(), element.toString().startsWith(
                "<static initializer #1>"));
            assertEquals("<clinit>", mdescriptor.getName());
          }

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("main", element.getElementName());
            assertEquals("main", mdescriptor.getName());
          }

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("", element.getElementName());
            assertTrue(element.toString(), element.toString().startsWith(
                "<static initializer #2>"));
            assertEquals("<clinit>", mdescriptor.getName());
          }

          assertFalse(matcher.methodMatched());

        } catch (JavaModelException e) {
          throw new RuntimeException(e);
        }

      }

      public void done() {
        // Do Nothing
      }
    }, new NullProgressMonitor());
  }

  public void testStaticVarInit() throws Exception {
    final String fullName = "methodmatcher/StaticVarInit";
    final TestData testData = setupProject("StaticVarInit", fullName);

    // Visit the types
    IPackageFragmentRoot[] roots = testData.instrumentation.getClassFiles()
        .getPackageFragmentRoots();
    TypeTraverser jep = new TypeTraverser(roots);
    jep.process(new ITypeVisitor() {

      public void visit(IType type, String binaryname) {
        try {
          // Test the name of the type
          assertEquals(fullName, binaryname);

          // Test the number of child IJavaElements
          assertEquals(2, type.getChildren().length);

          // Get the Type Descriptor
          ClassDescriptor classDesc = (ClassDescriptor) testData.descriptors
              .remove(binaryname);
          assertNotNull(classDesc);

          // Get the coveragedata
          DataHolder data = testData.coveragedata.getCoverage(classDesc);
          assertNotNull(data);
          boolean[][] covered = data.m_coverage;

          // Extract the method descriptors
          MethodDescriptor[] methods = classDesc.getMethods();
          assertEquals(3, methods.length);

          // Construct a matcher
          MethodMatcher matcher = new MethodMatcher(type, covered, methods);

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("main", element.getElementName());
            assertEquals("main", mdescriptor.getName());
          }

          assertFalse(matcher.methodMatched());

        } catch (JavaModelException e) {
          throw new RuntimeException(e);
        }

      }

      public void done() {
        // Do Nothing
      }
    }, new NullProgressMonitor());
  }

  public void testInstanceInit() throws Exception {
    final String fullName = "methodmatcher/InstanceInit";
    final TestData testData = setupProject("InstanceInit", fullName);

    // Visit the types
    IPackageFragmentRoot[] roots = testData.instrumentation.getClassFiles()
        .getPackageFragmentRoots();
    TypeTraverser jep = new TypeTraverser(roots);
    jep.process(new ITypeVisitor() {

      public void visit(IType type, String binaryname) {
        try {
          // Test the name of the type
          assertEquals(fullName, binaryname);

          // Test the number of child IJavaElements
          assertEquals(3, type.getChildren().length);

          // Get the Type Descriptor
          ClassDescriptor classDesc = (ClassDescriptor) testData.descriptors
              .remove(binaryname);
          assertNotNull(classDesc);

          // Get the coveragedata
          DataHolder data = testData.coveragedata.getCoverage(classDesc);
          assertNotNull(data);
          boolean[][] covered = data.m_coverage;

          // Extract the method descriptors
          MethodDescriptor[] methods = classDesc.getMethods();
          assertEquals(2, methods.length);

          // Construct a matcher
          MethodMatcher matcher = new MethodMatcher(type, covered, methods);

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("InstanceInit", element.getElementName());
            assertEquals("<init>", mdescriptor.getName());
          }

          assertTrue(matcher.methodMatched());
          {
            // Get selected objects
            IJavaElement element = matcher.getMatchedElement();
            MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
            assertEquals("main", element.getElementName());
            assertEquals("main", mdescriptor.getName());
          }

          assertFalse(matcher.methodMatched());

        } catch (JavaModelException e) {
          throw new RuntimeException(e);
        }

      }

      public void done() {
        // Do Nothing
      }
    }, new NullProgressMonitor());
  }

  protected void tearDown() throws Exception {
    if (javaProject != null) {
      javaProject.destroy();
    }
    super.tearDown();
  }

  private class CoverageListener implements IJavaCoverageListener {
    private boolean coverageChanged = false;

    public synchronized void coverageChanged() {
      coverageChanged = true;
      this.notifyAll();
    }

    public synchronized void waitForCoverageChanged()
        throws InterruptedException {
      while (!coverageChanged) {
        this.wait();
      }
    }
  }

  private class Config implements IConfigurationElement {
    public Object createExecutableExtension(String propertyName)
        throws CoreException {
      // Do nothing
      return null;
    }

    public String getAttribute(String name)
        throws InvalidRegistryObjectException {
      return "org.eclipse.jdt.launching.localJavaApplication";
    }

    public String getAttributeAsIs(String name)
        throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public String[] getAttributeNames() throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public IConfigurationElement[] getChildren()
        throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public IConfigurationElement[] getChildren(String name)
        throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public IExtension getDeclaringExtension()
        throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public String getName() throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public String getNamespace() throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public String getNamespaceIdentifier()
        throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public Object getParent() throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public String getValue() throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public String getValueAsIs() throws InvalidRegistryObjectException {
      // Do nothing
      return null;
    }

    public boolean isValid() {
      // Do nothing
      return false;
    }

  }
}
