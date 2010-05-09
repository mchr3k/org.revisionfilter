package org.revisionfilter.utils.console;

import org.eclipse.jface.action.Action;
import org.eclipse.team.ui.TeamImages;

public class RemoveAction extends Action
{

  /**
   * cTor
   */
  public RemoveAction()
  {
    this.setText("Close SVN Utils Console"); //$NON-NLS-1$
    setToolTipText("Close SVN Utils Console"); //$NON-NLS-1$
    setImageDescriptor(TeamImages.getImageDescriptor("elcl16/rem_co.gif")); //$NON-NLS-1$
    setDisabledImageDescriptor(TeamImages
        .getImageDescriptor("dlcl16/rem_co.gif")); //$NON-NLS-1$
  }

  @Override
  public void run()
  {
    RevisionFilterConsoleFactory.closeConsole();
  }
}