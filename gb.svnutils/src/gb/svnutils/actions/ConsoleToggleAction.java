package gb.svnutils.actions;

import gb.svnutils.SvnUtilsConsoleFactory;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.ui.TeamImages;

/**
 * Console toggle action.
 */
public class ConsoleToggleAction extends Action {
  private boolean enableState;
  private ImageDescriptor enableDescriptor;
  private ImageDescriptor disableDescriptor;

  /**
   * cTor
   */
  public ConsoleToggleAction() {
    this.setText("Toggle SVN Utils Console"); //$NON-NLS-1$
    enableDescriptor = TeamImages.getImageDescriptor("elcl16/participant_rem.gif"); //$NON-NLS-1$
    disableDescriptor = TeamImages.getImageDescriptor("dlcl16/participant_rem.gif"); //$NON-NLS-1$
    this.setDisabledImageDescriptor(disableDescriptor);
    enableState = !SvnUtilsConsoleFactory.isEnabled();
    run();
    this.setEnabled(true);
  }

  @Override
  public void run() {
    if (enableState)
    {
      this.setImageDescriptor(disableDescriptor);
      SvnUtilsConsoleFactory.setEnabled(false);
      setToolTipText("Enable SVN Utils Console"); //$NON-NLS-1$
    }
    else
    {
      this.setImageDescriptor(enableDescriptor);
      SvnUtilsConsoleFactory.setEnabled(true);
      setToolTipText("Disable SVN Utils Console"); //$NON-NLS-1$
    }
    enableState = !enableState;
  }
}
