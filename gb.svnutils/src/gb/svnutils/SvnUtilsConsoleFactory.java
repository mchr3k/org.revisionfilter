package gb.svnutils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;

/**
 * SVN Utils Console Factory
 */
public class SvnUtilsConsoleFactory implements IConsoleFactory {

  private static IOConsole sConsole = new IOConsole("SVN Utils Console", null, null, false);
  private static boolean sEnabled = false;

  public void openConsole()
  {
    showConsole();
  }

  /**
   * @return The console instance
   */
  public static synchronized IOConsole getConsole() {
    if (sConsole == null)
    {
      showConsole();
    }
    return sConsole;
  }

  /**
   * Show the console
   */
  public static synchronized void showConsole() {
    IConsole console = sConsole;
    if (console != null) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      IConsole[] existing = manager.getConsoles();
      boolean exists = false;
      for (int i = 0; i < existing.length; ++i) {
        if(console == existing[i])
          exists = true;
      }
      if(! exists)
        manager.addConsoles(new IConsole[] {console});
      manager.showConsoleView(console);
    }
  }

  /**
   * Close the console
   */
  public static synchronized void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    IConsole console = sConsole;
    if (console != null) {
      manager.removeConsoles(new IConsole[] {console});
    }
  }

  /**
   * @param xiMessage Line to output
   */
  public static synchronized void outputLine(String xiMessage)
  {
    if (sEnabled)
    {
      IOConsole lConsole = getConsole();
      Writer lWriter = new OutputStreamWriter(lConsole.newOutputStream());
      try
      {
        lWriter.write(xiMessage + "\n");
        lWriter.flush();
        lWriter.close();
      } catch (IOException e)
      {
        // Throw away
      }
    }
  }

  /**
   * @param b True if output should be enabled
   */
  public static synchronized void setEnabled(boolean b)
  {
    sEnabled = b;
  }

  /**
   * @return True if the console is enabled
   */
  public static synchronized boolean isEnabled()
  {
    return sEnabled;
  }
}
