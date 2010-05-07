package org.revisionfilter.utils;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.revisionfilter.utils.rcs.svn.SVNRevisionSystem;

/**
 * Used to check a resource against the base revision within a revision control system.
 */
public class RevisionChecker implements IRevisionSystem
{
  private final IRevisionSystem system;

  public static enum RevisionSystem
  {
    SVN
  }
  
  /**
   * Default cTor - Assume we are using SVN
   */
  public RevisionChecker()
  {
    this(RevisionSystem.SVN);    
  }
  
  /**
   * cTor for a specific type of backend
   * @param type
   */
  public RevisionChecker(RevisionSystem type)
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
    return system.getDirtyLines(file);
  }
  
  
}
