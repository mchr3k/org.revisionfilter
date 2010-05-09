package org.revisionfilter.utils.console;

import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

public class PageParticipant implements IConsolePageParticipant
{
  private RemoveAction consoleRemoveAction;
  private ToggleAction consoleToggleAction;

  @Override
  public void activated()
  {
    // Do nothing
  }

  @Override
  public void deactivated()
  {
    // Do nothing
  }

  @Override
  public void dispose()
  {
    this.consoleRemoveAction = null;
    this.consoleToggleAction = null;
  }

  @Override
  public void init(IPageBookViewPage page, IConsole console)
  {
    if (console == RevisionFilterConsoleFactory.getConsole())
    {
      this.consoleRemoveAction = new RemoveAction();
      this.consoleToggleAction = new ToggleAction();
      IActionBars bars = page.getSite().getActionBars();
      bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
      bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, new Separator());
      bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleToggleAction);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Class adapter)
  {
    return null;
  }
}
