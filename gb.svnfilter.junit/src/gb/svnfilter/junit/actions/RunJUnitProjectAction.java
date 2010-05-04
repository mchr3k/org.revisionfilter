package gb.svnfilter.junit.actions;

import gb.svnfilter.junit.Activator;
import gb.svnfilter.junit.JCallDBLoader;
import gb.svnutils.SvnDiffManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 *  Run as JUnit Test (SVN)
 */
public class RunJUnitProjectAction extends AbstractRunJUnitAction
{
  @Override
  public void run()
  {
    ISelection selection = currentSelection;
    if (selection instanceof IStructuredSelection)
    {
      IStructuredSelection structSel = (IStructuredSelection)selection;
      for (Object selItem : structSel.toList())
      {
        if (selItem instanceof IProject)
        {
          final IProject proj = (IProject)selItem;
          boolean javaNature = false;
          try
          {
            javaNature = proj.isNatureEnabled(JavaCore.NATURE_ID);
          } catch (CoreException e)
          {
            IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                "Error checking whether this is a java project", e);
            StatusManager.getManager().handle(status);
          }
          if (javaNature)
          {
            Job job = new Job("Finding SVN tests which need to be run")
            {
              @Override
              protected IStatus run(IProgressMonitor monitor)
              {
                monitor.beginTask("Progress", 100);
                try
                {
                  findJunitTests(monitor, proj);
                }
                catch (CoreException e)
                {
                  IStatus status = new Status(IStatus.ERROR,
                      Activator.PLUGIN_ID,
                      "Error encountered when seraching for tests to run", e);
                  StatusManager.getManager().handle(status);
                }
                monitor.done();
                return Status.OK_STATUS;
              }
            };
           job.setPriority(Job.LONG);
           job.schedule();
          }
        }
      }
    }
  }

  private void findJunitTests(IProgressMonitor monitor, IProject proj) throws CoreException
  {
    /*
     * 1. Find all new/changed source files
     * 2. Add all junit files
     * 3. For each changed type - add all calling test classes
     *
     * Note: we don't have to consider new types as they can only be
     * covered by changed junit test classes which will already be included
     * in step 2.
     */
    SvnDiffManager diffManager = new SvnDiffManager();
    IJavaProject jproj = JavaCore.create(proj);

    // Find all new/changed source files
    monitor.subTask("Finding dirty types");
    Set<IType> dirtyTypes = getDirtyTypes(diffManager, jproj);

    // Find all "interesting" junit test classes.
    //
    // Interesting Types:
    // - All dirty subclasses of TestCase
    // - All subclasses (which contain test methods) of a dirty class which
    //   itself subclasses TestCase
    monitor.worked(30);
    monitor.subTask("Selecting affected test classes");
    Set<IType> dirtyJunitTypes = getInterestingJunitTypes(jproj, dirtyTypes);
    Set<IType> testClassesToBeRun = new HashSet<IType>();
    testClassesToBeRun.addAll(dirtyJunitTypes);

    // Remove all the test types from the Set of dirtyTypes
    Set<IType> dirtyNonJunitTypes = new HashSet<IType>(dirtyTypes);
    dirtyNonJunitTypes.removeAll(dirtyJunitTypes);

    if (dirtyNonJunitTypes.size() > 0)
    {
      // Load test callers data
      monitor.worked(30);
      monitor.subTask("Loading test call data");
      Map<String, Set<String>> coverageMap = JCallDBLoader.loadJCallDB(proj);
      Map<IType, Set<IType>> testCallers = getTestCallers(jproj, coverageMap);

      // Add all junit tests calling into changed types
      monitor.worked(30);
      monitor.subTask("Selecting test classes which call changed classes");
      Set<IType> junitTypesCallingChangedType =
                                    getReachableJunitTypes(testCallers,
                                                           dirtyNonJunitTypes);
      testClassesToBeRun.addAll(junitTypesCallingChangedType);
    }
    else
    {
      monitor.worked(30);
    }

    // Launch the results
    monitor.worked(10);
    monitor.subTask("Launching selected test classes");
    runTests(jproj, testClassesToBeRun);
  }

  private Map<IType, Set<IType>> getTestCallers(
                                     IJavaProject jproj,
                                     Map<String, Set<String>> coverageMap)
                                     throws CoreException
  {
    Map<IType, Set<IType>> testCallers = new HashMap<IType, Set<IType>>();
    for (Entry<String, Set<String>> entry : coverageMap.entrySet())
    {
      // Lookup source type
      String sourcePath = entry.getKey();
      IJavaElement element = jproj.findElement(new Path(sourcePath));
      if (element != null)
      {
        // Extract a Set of types contained within this source file
        Set<IType> calledTypes = new HashSet<IType>();
        getContainedTypes(element, calledTypes);
        Set<IType> callingTypes = new HashSet<IType>();
        for (IType calledType : calledTypes)
        {
          testCallers.put(calledType, callingTypes);
        }

        // Extract the calling classes
        Set<String> callingClasses = entry.getValue();
        for (String callingClass : callingClasses)
        {
          IType callingType = jproj.findType(callingClass);
          if (callingType != null)
          {
            callingTypes.add(callingType);
          }
        }
      }
    }
    return testCallers;
  }

  private void getContainedTypes(IJavaElement element,
                                 Set<IType> types)
                                 throws CoreException
  {
    if (element.getElementType() == IJavaElement.TYPE)
    {
      types.add((IType)element);
    }
    if (element instanceof IParent)
    {
      IParent parent = (IParent)element;
      for (IJavaElement newchildele : parent.getChildren())
      {
        getContainedTypes(newchildele, types);
      }
    }
  }

  private Set<IType> getReachableJunitTypes(Map<IType, Set<IType>> testCallers,
                                            Set<IType> changedNonJunitTypes)
  {
    Set<IType> reachableJunitTypes = new HashSet<IType>();
    for (IType type : changedNonJunitTypes)
    {
      Set<IType> callingTypes = testCallers.get(type);
      if (callingTypes != null)
      {
        reachableJunitTypes.addAll(callingTypes);
      }
    }
    return reachableJunitTypes;
  }

  private Set<IType> getInterestingJunitTypes(IJavaProject jproj,
                                              Set<IType> candidateTypes)
                                              throws CoreException
  {
    Set<IType> interestingJunitTypes = new HashSet<IType>();
    for (IType candidateType : candidateTypes)
    {
      ITypeHierarchy typeHierarchy =
                         candidateType.newTypeHierarchy(new NullProgressMonitor());
      if (isTestCaseSubclass(jproj, typeHierarchy.getAllSupertypes(candidateType)))
      {
        // Add non abstract classes which have test methods
        if (!Flags.isAbstract(candidateType.getFlags()) &&
            hasTestMethods(candidateType))
        {
          interestingJunitTypes.add(candidateType);
        }

        // Stuff subclasses into a Set
        IType[] subtypes = typeHierarchy.getAllSubtypes(candidateType);
        Set<IType> subtypeSet = new HashSet<IType>();
        for (IType subtype : subtypes)
        {
          subtypeSet.add(subtype);
        }

        // Examine subclasses
        interestingJunitTypes.addAll(getInterestingJunitTypes(jproj,
                                                              subtypeSet));
      }
    }
    return interestingJunitTypes;
  }

  private Set<IType> getDirtyTypes(SvnDiffManager diffManager,
                                   IJavaProject jproj) throws CoreException
  {
    Set<IType> dirtyTypes = new HashSet<IType>();
    for (IPackageFragmentRoot root : jproj.getAllPackageFragmentRoots())
    {
      if (!root.isArchive())
      {
        for (IJavaElement childele : root.getChildren())
        {
          findDirtyTypes(childele, dirtyTypes, diffManager);
        }
      }
    }
    return dirtyTypes;
  }

  private boolean hasTestMethods(IType type) throws CoreException
  {
    // Construct a Set of types to consider
    ITypeHierarchy typeHierarchy =
                              type.newTypeHierarchy(new NullProgressMonitor());
    IType[] superTypes = typeHierarchy.getAllSupertypes(type);
    Set<IType> superTypesSet = new HashSet<IType>();
    superTypesSet.add(type);
    for (IType superType : superTypes)
    {
      superTypesSet.add(superType);
    }

    // Examine this type and all supertypes
    for (IType currentType : superTypesSet)
    {
      for (IMethod method : currentType.getMethods())
      {
        if (method.getElementName().startsWith("test") &&
            (method.getNumberOfParameters() == 0) &&
            Flags.isPublic(method.getFlags()))
        {
          return true;
        }
      }
    }

    return false;
  }

  private boolean isTestCaseSubclass(IJavaProject jproj, IType[] allSupertypes) throws CoreException
  {
    IType testCaseType = jproj.findType("junit.framework.TestCase");
    for (IType superType : allSupertypes)
    {
      if (superType.equals(testCaseType))
      {
        return true;
      }
    }
    return false;
  }

  private void findDirtyTypes(IJavaElement childele,
                              Set<IType> dirtySourceList,
                              SvnDiffManager diffManager) throws CoreException
  {
    if (childele.getElementType() == IJavaElement.TYPE)
    {
      if (diffManager.isSVNDirty(childele))
      {
        dirtySourceList.add((IType)childele);
      }
    }
    if (childele instanceof IParent)
    {
      IParent parent = (IParent)childele;
      for (IJavaElement newchildele : parent.getChildren())
      {
        findDirtyTypes(newchildele, dirtySourceList, diffManager);
      }
    }
  }
}
