package org.revisionfilter.utils.rcs;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.revisionfilter.utils.rcs.svn.SVNRevisionSystem;

/**
 * Wrap another revision control system and cache all results. The cache cannot
 * be reset, the caller must create a new instance.
 */
public class CachedRevisionSystem implements IRevisionSystem
{
  private final IRevisionSystem system;
  private final Map<String, Set<Integer>> cachedDirtyLines = new ConcurrentHashMap<String, Set<Integer>>();

  public static enum RevisionSystem
  {
    SVN
  }

  /**
   * Default cTor - Assume we are using SVN
   */
  public CachedRevisionSystem()
  {
    this(RevisionSystem.SVN);
  }

  /**
   * cTor for a specific type of backend
   * 
   * @param type
   */
  public CachedRevisionSystem(RevisionSystem type)
  {
    system = new SVNRevisionSystem();
  }

  @Override
  public boolean isDirty(IResource resource, int flags)
  {
    return system.isDirty(resource, flags);
  }

  @Override
  public Set<Integer> getDirtyLines(IFile file)
  {
    String absoluteFileName = file.getLocation().toOSString();
    Set<Integer> dirtyLines = cachedDirtyLines.get(absoluteFileName);
    if (dirtyLines == null)
    {
      dirtyLines = system.getDirtyLines(file);
      cachedDirtyLines.put(absoluteFileName, dirtyLines);
    }
    return dirtyLines;
  }

}
