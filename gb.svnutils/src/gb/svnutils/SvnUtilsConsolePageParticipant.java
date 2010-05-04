package gb.svnutils;

import gb.svnutils.actions.ConsoleRemoveAction;
import gb.svnutils.actions.ConsoleToggleAction;

import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Console page participant
 */
public class SvnUtilsConsolePageParticipant implements IConsolePageParticipant {

  private ConsoleRemoveAction consoleRemoveAction;
  private ConsoleToggleAction consoleToggleAction;

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
    if (console == SvnUtilsConsoleFactory.getConsole())
    {
      this.consoleRemoveAction = new ConsoleRemoveAction();
      this.consoleToggleAction = new ConsoleToggleAction();
      IActionBars bars = page.getSite().getActionBars();
      bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
      bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, new Separator());
      bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleToggleAction);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(@SuppressWarnings("unused") Class adapter)
  {
    return null;
  }

}
