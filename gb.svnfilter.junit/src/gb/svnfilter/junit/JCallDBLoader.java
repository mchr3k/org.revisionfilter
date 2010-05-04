package gb.svnfilter.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Resource manager for JCallDB files
 */
public class JCallDBLoader {

  private static final Map<IProject,Map<String, Set<String>>> loadedJCallDB =
                    new ConcurrentHashMap<IProject, Map<String,Set<String>>>();

  /**
   * @param proj
   * @return The latest JCallDB, loaded from the filesystem if necessary.
   */
  @SuppressWarnings("unchecked")
  public static synchronized Map<String, Set<String>> loadJCallDB(IProject proj)
  {
    Map<String, Set<String>> coverageMap = loadedJCallDB.get(proj);
    if (coverageMap == null)
    {
      IFile jcallDB = proj.getFile("test.jcalldb");
      String jcallDBPath = jcallDB.getLocation().toOSString();
      try
      {
        ObjectInputStream objIn =
             new ObjectInputStream(new FileInputStream(new File(jcallDBPath)));
        coverageMap = (Map<String, Set<String>>)objIn.readObject();
        loadedJCallDB.put(proj, coverageMap);
        listenForChanges(proj);
      } catch (Exception e)
      {
        IStatus status = new Status(IStatus.ERROR,
            Activator.PLUGIN_ID,
            "Failed to load coverage DB: ", e);
        StatusManager.getManager().handle(status);
      }
    }
    return coverageMap;
  }

  private static synchronized void invalidateJCallDB(final IProject proj)
  {
    loadedJCallDB.remove(proj);
  }

  private static void listenForChanges(final IProject proj)
  {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IResourceChangeListener changeListen = new IResourceChangeListener() {
      @Override
      public void resourceChanged(IResourceChangeEvent xiEvent) {
        IResourceDeltaVisitor changeVisitor = new IResourceDeltaVisitor() {
          boolean keepLooking = true;
          @Override
          public boolean visit(IResourceDelta xiDelta) {
            if ((xiDelta.getResource() != null) &&
                (xiDelta.getResource().getName().equals("test.jcalldb")))
            {
              invalidateJCallDB(proj);
              keepLooking = false;
            }
            return keepLooking;
          }
        };
        try {
          xiEvent.getDelta().accept(changeVisitor);
        } catch (CoreException e) {
          // Throw away exception
        }
      }
    };
    workspace.addResourceChangeListener(changeListen,
                                        IResourceChangeEvent.POST_CHANGE);
  }
}
