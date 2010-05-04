package gb.svnfilter.junit;

import gb.svnfilter.junit.actions.RunJUnitSelectedTestsAction;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

/**
 * Viewer for which classes call the code file in the active editor
 */
public class CallingTestsView extends ViewPart implements ISelectionListener {
  private TreeViewer viewer;
  private CalledFile treeRoot = null;
  private final RunJUnitSelectedTestsAction runAction;

  /**
   * Tree object hierarchy:
   * TreeObject
   * -> TreeParent
   */

  /**
   * Tree objects
   */
  public class CallingTest {

    /**
     * Name of this element
     */
    protected final String name;
    private CalledFile parent;
    private IType type;

    /**
     * cTor
     * @param name
     */
    public CallingTest(String name) {
      this.name = name;
    }

    /**
     * @return name of this node
     */
    public String getName() {
      return name;
    }

    /**
     * @param parent Parent of this node
     */
    public void setParent(CalledFile parent) {
      this.parent = parent;
    }

    /**
     * @return Parent of this node
     */
    public CalledFile getParent() {
      return parent;
    }

    @Override
    public String toString() {
      return getName();
    }

    /**
     * @return Resource associated with this node
     */
    protected IResource getResouce() {
      if (this.type != null)
      {
        return type.getResource();
      }
      return null;
    }

    /**
     * @param type Type associated with this node
     */
    protected void setType(IType type) {
      this.type = type;
    }

    /**
     * @return Type associated with this node
     */
    public IType getType() {
      return type;
    }
  }

  /**
   * Tree object used for the top level java element
   */
  public class CalledFile extends CallingTest {
    private final ArrayList<CallingTest> children = new ArrayList<CallingTest>();
    private final IJavaElement thisJavaElement;

    /**
     * cTor
     * @param xiEditJavaElement
     */
    public CalledFile(IJavaElement xiEditJavaElement) {
      super(xiEditJavaElement.getElementName());
      thisJavaElement = xiEditJavaElement;
      Map<String, Set<String>> coverageData = JCallDBLoader.loadJCallDB(thisJavaElement.getJavaProject().getProject());
      if (coverageData != null)
      {
        if (thisJavaElement.getElementType() == IJavaElement.COMPILATION_UNIT)
        {
          ICompilationUnit compUnit = (ICompilationUnit)thisJavaElement;
          String fullPath = getFullPath(compUnit);
          Set<String> fileCallers = coverageData.get(fullPath);
          if (fileCallers != null)
          {
            IJavaProject jproj = compUnit.getJavaProject();
            Set<String> sortedFileCallers = new TreeSet<String>(fileCallers);
            for (String caller : sortedFileCallers)
            {
              CallingTest child = new CallingTest(caller);
              try {
                IType childType = jproj.findType(caller);
                child.setType(childType);
              } catch (JavaModelException e) {
                // Throw away
              }
              children.add(child);
            }
          }
        }
      }
      else
      {
        CallingTest child = new CallingTest("No coverage db: test.jcalldb");
        children.add(child);
      }
    }

    // TODO : This probably breaks for file not in a package!
    private String getFullPath(ICompilationUnit xiCompUnit) {
      String path = null;
      try {
        IPackageDeclaration[] packDecls = xiCompUnit.getPackageDeclarations();
        String packageDecl = packDecls[0].getElementName();
        String packagePath = packageDecl.replace(".", "/");
        String fileName = thisJavaElement.getElementName();
        path = packagePath + "/" + fileName;
      } catch (JavaModelException e) {
        // Throw away
      }
      return path;
    }

    /**
     * Special cTor for blank top level elements
     * @param xiName
     */
    public CalledFile(String xiName) {
      super(xiName);
      thisJavaElement = null;
      CallingTest child = new CallingTest("Not a java file in a java project");
      children.add(child);
    }

    /**
     * @return Children of this object
     */
    public CallingTest[] getChildren() {
      return children.toArray(new CallingTest[children.size()]);
    }

    /**
     * @return Whether this node has children
     */
    public boolean hasChildren() {
      return children.size() > 0;
    }

  }

  /**
   * Content provider for this view
   */
  class ViewContentProvider implements ITreeContentProvider {

    @SuppressWarnings("unused")
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
      // Do nothing
    }

    public void dispose() {
      // Do nothing
    }

    public Object[] getElements(Object parent) {
      if (parent.equals(getViewSite())) {
        if (treeRoot != null)
        {
          return new Object[] {treeRoot};
        }
        return new Object[0];
      }

      return getChildren(parent);
    }

    public Object getParent(Object child) {
      if (child instanceof CallingTest) {
        return ((CallingTest) child).getParent();
      }

      return null;
    }

    public Object[] getChildren(Object parent) {

      if (parent instanceof CalledFile)
      {
        return ((CalledFile) parent).getChildren();
      }

      return new Object[0];
    }

    public boolean hasChildren(Object parent) {
      if (parent instanceof CalledFile)
        return ((CalledFile) parent).hasChildren();
      return false;
    }

  }

  /**
   * Label provider
   */
  class ViewLabelProvider extends LabelProvider {
    @Override
    public Image getImage(Object obj) {
      String imageKey = ISharedImages.IMG_OBJS_CLASS;

      if (obj instanceof CalledFile)
        imageKey = ISharedImages.IMG_OBJS_CUNIT;
      return JavaUI.getSharedImages().getImage(imageKey);
    }

  }

  /**
   * cTor
   */
  public CallingTestsView() {
    runAction = new RunJUnitSelectedTestsAction();
  }

  @Override
  public void createPartControl(Composite parent) {
    getSite().getPage().addSelectionListener(this);
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    viewer.setContentProvider(new ViewContentProvider());
    viewer.setLabelProvider(new ViewLabelProvider());
    viewer.setInput(getViewSite());
    hookDoubleClickAction();

    // Setup Context Menu
    MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
    viewer.getControl().setMenu(menuMgr.createContextMenu(viewer.getControl()));
    menuMgr.add(runAction);
    viewer.addSelectionChangedListener(runAction);
  }

  private void hookDoubleClickAction() {
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        ISelection selection = event.getSelection();
        Object obj = ((IStructuredSelection) selection).getFirstElement();
        if (!(obj instanceof CallingTest)) {
          return;
        }
        CallingTest tempObj = (CallingTest) obj;
        IResource tempObjRes = tempObj.getResouce();
        if (tempObjRes != null)
        {
          IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(
                                                     tempObjRes.getFullPath());
          IWorkbenchPage dpage = CallingTestsView.this.getViewSite()
              .getWorkbenchWindow().getActivePage();
          if (dpage != null) {
            try {
              IDE.openEditor(dpage, ifile, true);
            } catch (Exception e) {
              // Do nothing
            }
          }
        }
      }
    });
  }

  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  @Override
  public void selectionChanged(IWorkbenchPart xiPart,
                               @SuppressWarnings("unused") ISelection xiSelection) {
    if (xiPart instanceof IEditorPart)
    {
      IEditorPart editPart = (IEditorPart)xiPart;
      IEditorInput editInput = editPart.getEditorInput();
      IJavaElement editJavaElement = JavaUI.getEditorInputJavaElement(editInput);
      if (editJavaElement != null)
      {
        treeRoot = new CalledFile(editJavaElement);
        IJavaProject project = editJavaElement.getJavaProject();
        runAction.projectChanged(project);
      }
      else
      {
        treeRoot = new CalledFile(editInput.getName());
      }
      // Refresh once to pickup the new root
      viewer.refresh();

      // Expand the new root then refresh again
      viewer.setExpandedState(treeRoot, true);
      viewer.refresh(treeRoot);
    }
  }

}
