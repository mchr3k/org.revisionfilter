package gb.svnutils.actions;

import gb.svnutils.SvnUtilsConsoleFactory;

import org.eclipse.jface.action.Action;
import org.eclipse.team.ui.TeamImages;


/**
 * Console remove action.
 */
public class ConsoleRemoveAction extends Action {

  /**
   * cTor
   */
  public ConsoleRemoveAction() {
    this.setText("Close SVN Utils Console"); //$NON-NLS-1$
    setToolTipText("Close SVN Utils Console"); //$NON-NLS-1$
    setImageDescriptor(TeamImages.getImageDescriptor("elcl16/rem_co.gif")); //$NON-NLS-1$
    setDisabledImageDescriptor(TeamImages.getImageDescriptor("dlcl16/rem_co.gif")); //$NON-NLS-1$
  }

  @Override
  public void run() {
    SvnUtilsConsoleFactory.closeConsole();
  }
}
