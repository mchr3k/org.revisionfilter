package org.revisionfilter.utils.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.ui.TeamImages;

public class ToggleAction extends Action
{
  private boolean enableState;
  private ImageDescriptor enableDescriptor;
  private ImageDescriptor disableDescriptor;

  /**
   * cTor
   */
  public ToggleAction()
  {
    this.setText("Toggle SVN Utils Console"); //$NON-NLS-1$
    enableDescriptor = TeamImages
        .getImageDescriptor("elcl16/participant_rem.gif"); //$NON-NLS-1$
    disableDescriptor = TeamImages
        .getImageDescriptor("dlcl16/participant_rem.gif"); //$NON-NLS-1$
    this.setDisabledImageDescriptor(disableDescriptor);
    enableState = !RevisionFilterConsoleFactory.isEnabled();
    run();
    this.setEnabled(true);
  }

  @Override
  public void run()
  {
    if (enableState)
    {
      this.setImageDescriptor(disableDescriptor);
      RevisionFilterConsoleFactory.setEnabled(false);
      setToolTipText("Enable SVN Utils Console"); //$NON-NLS-1$
    }
    else
    {
      this.setImageDescriptor(enableDescriptor);
      RevisionFilterConsoleFactory.setEnabled(true);
      setToolTipText("Disable SVN Utils Console"); //$NON-NLS-1$
    }
    enableState = !enableState;
  }
}