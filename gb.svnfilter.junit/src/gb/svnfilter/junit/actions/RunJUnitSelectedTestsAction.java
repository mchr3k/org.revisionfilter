package gb.svnfilter.junit.actions;

import gb.svnfilter.junit.CallingTestsView.CallingTest;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Run the selected tests.
 */
@SuppressWarnings("restriction")
public class RunJUnitSelectedTestsAction extends AbstractRunJUnitAction
                                 implements ISelectionChangedListener {

  /**
   * cTor
   */
  public RunJUnitSelectedTestsAction() {
    setText("Run Selected Test(s)");
    setToolTipText("Run Selected Test(s)");
    setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/julaunch.gif"));
    setEnabled(true);
  }

  private ISelection selection = null;
  private IJavaProject project = null;

  @Override
  public void run() {
    ISelection lSelection = selection;
    if ((lSelection != null) &&
        !lSelection.isEmpty() &&
        (lSelection instanceof IStructuredSelection))
    {
      // Extract selected tests
      Set<IType> selectedTypes = new HashSet<IType>();
      IStructuredSelection structSel = (IStructuredSelection)lSelection;
      for (Object selItem : structSel.toList())
      {
        if (selItem instanceof CallingTest)
        {
          CallingTest selTest = (CallingTest)selItem;
          IType selType = selTest.getType();
          if (selType != null)
          {
            selectedTypes.add(selType);
          }
        }
      }

      runTests(project, selectedTypes);
    }
  }


  /**
   * @param xiProject The currently selected project.
   */
  public void projectChanged(IJavaProject xiProject) {
    project = xiProject;
  }

}
