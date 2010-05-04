package gb.svnutils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TokenComparator;
import org.eclipse.compare.internal.CompareContainer;
import org.eclipse.compare.internal.DocumentManager;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.compare.internal.merge.DocumentMerger.Diff;
import org.eclipse.compare.internal.merge.DocumentMerger.IDocumentMergerInput;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.ui.compare.CompareMessages;
import org.eclipse.jdt.internal.ui.compare.JavaStructureCreator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Point;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Utility function which performs subversion related diff activities
 */
public class SvnDiffManager {

  private List<String> mDiffedFiles = new ArrayList<String>();

  private Map<String,List<Point>> mFileDiffRegions = new HashMap<String, List<Point>>();

  private Map<String,DiffNode> mStructSigToDiffMap = new HashMap<String,DiffNode>();

  private Map<IResource, DiffNode> mResourceToDiffMap = new HashMap<IResource, DiffNode>();

  private Map<String, Boolean> dirtyStatus = new HashMap<String, Boolean>();

  private static final char ANCESTOR_CONTRIBUTOR = MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR;

  private static final char RIGHT_CONTRIBUTOR = MergeViewerContentProvider.RIGHT_CONTRIBUTOR;

  private static final char LEFT_CONTRIBUTOR = MergeViewerContentProvider.LEFT_CONTRIBUTOR;

  /**
   * Determine whether a particular java element is dirty. Ignore unversioned files.
   * @param element Element to check.
   * @return True if the given element is dirty.
   */
  public boolean isSVNDirty(IJavaElement element) {
    return isSVNDirty(element, false);
  }

  /**
   * Determine whether a particular java element is dirty.
   * @param element Element to check.
   * @param element Whether an unversioned file should be considered "dirty".
   * @return True if the given element is dirty.
   */
  public boolean isSVNDirty(IJavaElement element, boolean xiUnversionedIsDirty) {

    if (element == null)
    {
      throw new NullPointerException("element is null");
    }

    // Lookup resources
    IJavaElement lResourceElement = getJavaElementResource(element);
    IResource localResource = getResource(lResourceElement);
    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(localResource);

    // Check whether the SVN resource is being ignored
    boolean isIgnored = false;
    try {
      if (svnResource.isIgnored()) {
        return false;
      }
    } catch (SVNException e) {
      // Exception - don't go any further.
      return false;
    }

    // Assume that files/folders are dirty unless SVN says they are clean
    boolean isAdded = false;
    boolean isDirty = true;
    boolean isUnversioned = false;
    boolean isCodeBlock = (element.getElementType() == IJavaElement.METHOD) ||
                          (element.getElementType() == IJavaElement.INITIALIZER) ||
                          (element.getElementType() == IJavaElement.TYPE);
    if (!isIgnored) {
      try {
        // Check that the resource is actually under version control
        LocalResourceStatus status = svnResource.getStatus();
        isUnversioned = status.isUnversioned();
        isDirty = isSvnResourceDirty(svnResource);
        isAdded = status.isAdded();

        if (isCodeBlock && isDirty && !isUnversioned)
        {
          DiffNode lResult = mResourceToDiffMap.get(localResource);
          if (lResult == null)
          {
            // Perform Folder Tree Diff
            lResult = doFolderTreeDiff(svnResource);
            if (lResult != null)
            {
              loadFolderTreeDiffResult(lResult);
            }
          }

          if (lResult != null)
          {
            // Extra processing for code blocks which haven't just been added
            if (isCodeBlock && !isAdded)
            {
              // Get the two file elements of concern
              ITypedElement localElement = lResult.getLeft();
              ITypedElement svnElement = lResult.getRight();

              // Look up through the element tree for candidate structural elements
              Stack<IJavaElement> structuralElements = getStructuralElements(element);
              IJavaElement topMostStructuralElement = structuralElements.peek();

              // Assume code blocks are clean unless they appear in the struct
              // diff
              isDirty = false;

              String structSig = getStructuralSig(topMostStructuralElement);
              String structPath = getStructuralPath(topMostStructuralElement);
              String uniqueSig = structPath + "#" + structSig;
              DiffNode lStructResult = mStructSigToDiffMap.get(uniqueSig);

              if ((svnElement != null) &&
                  (lStructResult == null) &&
                  (!mDiffedFiles.contains(structPath)))
              {
                IResourceProvider provider = (IResourceProvider)lResult.getLeft();
                IResource resource = provider.getResource();
                if (resource != null)
                {
                  lStructResult = doStructuralDiff(localElement, svnElement);
                  loadStructuralDiffResult(resource, lStructResult);
                  mDiffedFiles.add(structPath);
                }
              }

              if (lStructResult != null)
              {
                isDirty = isStructurallyDifferent(structuralElements, 
                                                  lStructResult);
              }
            }
            else
            {
              // Normal diff processing
              isDirty = inFolderTreeDiff(localResource, lResult);
            }
          }
        }
      }
      catch (Exception ex)
      {
        IStatus status = new Status(IStatus.ERROR, SvnDiffManagerActivator.ID,
            "Exception when checking SVN status of : " + element, ex);
        StatusManager.getManager().handle(status);
      }
    }
    boolean isSVNDirty = (isUnversioned && xiUnversionedIsDirty) ||
                         (!isUnversioned && !isIgnored && isDirty);
    if (isSVNDirty)
    {
      SvnUtilsConsoleFactory.outputLine(element.getElementName() + " is dirty");
    }
    return isSVNDirty;
  }

  /**
   * Filter the lines array such that all clean lines are set to -1.
   * @param lines An array of lines within element to check.
   * @param element Element to check.
   * @return A modified copy of lines with clean lines set to -1.
   */
  public int[] filterSVNCleanLines(int[] lines, IJavaElement element) {
    int[] ret = lines;
    boolean isDirtyElement = isSVNDirty(element);
    if (isDirtyElement)
    {
      try
      {
        // Fetch the existing diff info
        IJavaElement lElementForSvnLookup = element;
        if ((element.getElementType() == IJavaElement.METHOD) ||
            (element.getElementType() == IJavaElement.INITIALIZER)) {
          lElementForSvnLookup = element.getParent();
        }

        // Get the file diff result
        lElementForSvnLookup = getJavaElementResource(lElementForSvnLookup);
        IResource localResource = getResource(lElementForSvnLookup);
        DiffNode lResult = mResourceToDiffMap.get(localResource);

        // Lookup upwards through the tree for the correct element which
        // would be in the structural diff
        Stack<IJavaElement> structuralElements = getStructuralElements(element);
        if (structuralElements.size() > 0)
        {
          IJavaElement structuralElement = structuralElements.pop();

          // Get the structural diff sig
          String elementSig = getStructuralSig(structuralElement);
          String path = getStructuralPath(structuralElement);
          String uniqueSig = path + "#" + elementSig;

          // Lookup the structural result
          DiffNode lStructuralDiffResult = mStructSigToDiffMap.get(uniqueSig);
          ITypedElement localElement = lResult.getLeft();
          ITypedElement svnElement = lResult.getRight();

          // Now get line based diff data
          List<Point> diffRegions = mFileDiffRegions.get(path);
          if (diffRegions == null)
          {
            diffRegions = doLineDiff(lStructuralDiffResult, localElement, svnElement);
            mFileDiffRegions.put(path, diffRegions);
          }

          // Check each line in the block
          lines = applyLineDiffFilter(lines, diffRegions);
        }
      }
      catch (Exception ex)
      {
        IStatus status = new Status(IStatus.ERROR, SvnDiffManagerActivator.ID,
            "Exception when checking SVN status of lines in: " + element, ex);
        StatusManager.getManager().handle(status);
      }
    }
    return ret;
  }

  private String getStructuralPath(IJavaElement xiElement) {
    String typePath = xiElement.getResource().getFullPath().toOSString();
    return typePath;
  }

  @SuppressWarnings("restriction")
  private String getStructuralSig(IJavaElement xiElement) {
    String elementSig = null;
    if (xiElement.getElementType() == IJavaElement.METHOD) {
      // Special Processing for Methods
      elementSig = getMethodSignature((SourceMethod)xiElement);
    }
    else if (xiElement.getElementType() == IJavaElement.INITIALIZER)
    {
      elementSig = CompareMessages.JavaNode_initializer;
    }
    else if (xiElement.getElementType() == IJavaElement.TYPE ||
             xiElement.getElementType() == IJavaElement.FIELD)
    {
      elementSig = xiElement.getElementName();
    }
    return elementSig;
  }

  /**
   * Structural diff does not include anonymous inner classes
   * @param element
   * @return
   * @throws JavaModelException
   */
  protected Stack<IJavaElement> getStructuralElements(IJavaElement element) throws JavaModelException
  {
    IJavaElement currentSearchElement = element;

    Stack<IJavaElement> elementList = new Stack<IJavaElement>();

    // Create a stack of parent types
    while ((currentSearchElement.getElementType() != IJavaElement.COMPILATION_UNIT))
    {
      if (currentSearchElement.getElementType() == IJavaElement.TYPE)
      {
        // Type are allowed to contain children within a structural diff
        elementList.push(currentSearchElement);
      }
      else if (currentSearchElement.getElementType() == IJavaElement.FIELD ||
               currentSearchElement.getElementType() == IJavaElement.INITIALIZER ||
               currentSearchElement.getElementType() == IJavaElement.METHOD)
      {
        // Fields, methods and initializers are not explored by a structural
        // diff. The whole element is either marked as dirty or clean.
        elementList.clear();
        elementList.push(currentSearchElement);
      }
      currentSearchElement = currentSearchElement.getParent();
    }

    // Return the enclosing block
    return elementList;
  }

  /**
   * The SVN resource does a recursive dirty check. This lookup caches the
   * results from this check to avoid having to do this repeatedly.
   * @param xiSvnResource
   * @return True if the SVN resource is dirty
   * @throws TeamException
   */
  private boolean isSvnResourceDirty(ISVNLocalResource xiSvnResource)
      throws TeamException {
    Boolean lStatus = null;

    // Lookup cached state
    if (xiSvnResource.getResource() != null) {
      lStatus = dirtyStatus.get(xiSvnResource.getResource().toString());
    }
    if (lStatus != null) {
      return lStatus;
    }

    // Save new state
    if (xiSvnResource.getStatus().isDirty() ||
        xiSvnResource.getStatus().isAdded()) {
      if (xiSvnResource.getResource() != null)
      {
        dirtyStatus.put(xiSvnResource.getResource().toString(), true);
      }
      return true;
    }

    // Recurse into folders
    if (xiSvnResource instanceof ISVNFolder) {
      ISVNFolder lSvnFolder = (ISVNFolder) xiSvnResource;
      ISVNResource[] children = lSvnFolder.members(new NullProgressMonitor(),
          ISVNFolder.ALL_UNIGNORED_MEMBERS);
      for (int i = 0; i < children.length; i++) {
        if (children[i] instanceof ISVNLocalResource) {
          ISVNLocalResource localChild = (ISVNLocalResource) children[i];
          boolean isChildDirty = isSvnResourceDirty(localChild);
          if (isChildDirty
              || (localChild.exists() && !localChild.isManaged())) {
            // if a child resource is dirty consider the parent dirty as
            // well, there is no need to continue checking other siblings.
            return true;
          }

          // Store details about clean children
          if (!isChildDirty)
          {
            if (xiSvnResource.getResource() != null)
            {
              dirtyStatus.put(xiSvnResource.getResource().toString(), true);
            }
          }
        }
      }
    }
    return false;
  }

  private DiffNode doFolderTreeDiff(ISVNLocalResource svnResource) throws
                                                     InterruptedException,
                                                     InvocationTargetException,
                                                     SVNException
  {
    // Perform Diff
    SVNLocalCompareInput lInput = new SVNLocalCompareInput(svnResource,
        SVNRevision.BASE);
    lInput.run(new NullProgressMonitor());

    // Extract Result
    DiffNode lResult = (DiffNode) lInput.getCompareResult();
    return lResult;
  }

  private void loadFolderTreeDiffResult(DiffNode diff)
  {
    IResourceProvider lLeftEle = (IResourceProvider)diff.getLeft();
    if (lLeftEle != null)
    {
      IResource lResource = lLeftEle.getResource();
      mResourceToDiffMap.put(lResource, diff);
    }

    if (diff.getChildren() != null)
    {
      for (IDiffElement element : diff.getChildren())
      {
        loadFolderTreeDiffResult((DiffNode)element);
      }
    }
  }

  /**
   * Recursive search for a Resource within a Diff.
   * @param projectResource
   * @param lResult
   * @return
   */
  private boolean inFolderTreeDiff(IResource projectResource, DiffNode lResult) {
    IResource lDiffResource = ((IResourceProvider) lResult.getLeft())
        .getResource();

    if (lDiffResource.equals(projectResource)) {
      return true;
    }

    IDiffElement[] lChildren = lResult.getChildren();
    if (lChildren != null) {
      for (int i = 0; i < lChildren.length; i++) {
        IDiffElement lDiffChild = lChildren[i];
        if (lDiffChild != null) {
          DiffNode lChildDiffNode = (DiffNode) lDiffChild;
          lDiffResource = ((IResourceProvider) lChildDiffNode.getLeft())
              .getResource();
          if (lDiffResource.equals(projectResource)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Perform a structural diff
   * @param leftElement
   * @param rightElement
   * @return
   * @throws CoreException
   */
  protected static DiffNode doStructuralDiff(ITypedElement leftElement,
      ITypedElement rightElement) throws CoreException
  {
    Differencer fDifferencer = new Differencer()
    {
      @Override
      protected boolean contentsEqual(Object o1, Object o2) {
        JavaStructureCreator fStructureCreator = new JavaStructureCreator();
        if (fStructureCreator != null) {
          boolean ignoreWhiteSpace = true;
          String s1 = fStructureCreator.getContents(o1, ignoreWhiteSpace);
          String s2 = fStructureCreator.getContents(o2, ignoreWhiteSpace);
          if (s1 == null || s2 == null)
            return false;
          return s1.equals(s2);
        }
        return false;
      }
    };

    JavaStructureCreator fStructureCreator = new JavaStructureCreator();
    IStructureComparator lSvnStruct = fStructureCreator.createStructure(
        leftElement, new NullProgressMonitor());
    IStructureComparator lBaseStruct = fStructureCreator.createStructure(
        rightElement, new NullProgressMonitor());
    DiffNode fRoot = (DiffNode) fDifferencer.findDifferences(false,
        new NullProgressMonitor(), null, null, lSvnStruct, lBaseStruct);
    return fRoot;
  }

  /**
   * Index the results of a structural diff
   * @param fileResource
   * @param fileResult
   */
  protected void loadStructuralDiffResult(IResource fileResource, DiffNode fileResult)
  {
    ITypedElement lLeftEle = fileResult.getLeft();
    if ((lLeftEle != null) &&
        !lLeftEle.getName().contains("Import Declarations") &&
        !lLeftEle.getName().contains("Compilation Unit") &&
        !lLeftEle.getName().contains("Package Declarations"))
    {
      String path = fileResource.getFullPath().toOSString();
      mStructSigToDiffMap.put(path + "#" + lLeftEle.getName(), fileResult);
    }

    if (fileResult.getChildren() != null)
    {
      for (IDiffElement element : fileResult.getChildren())
      {
        loadStructuralDiffResult(fileResource, (DiffNode)element);
      }
    }
  }

  /**
   * Recursive search for a particular method signature within a Diff
   * @param targetElements
   * @param newDiff
   * @return
   */
  protected boolean isStructurallyDifferent(Stack<IJavaElement> targetElements, 
                                            DiffNode newDiff) {

    // Extract the sigs
    IJavaElement topMostElement = targetElements.peek();
    String targetSig = getStructuralSig(topMostElement);
    ITypedElement lLeft = newDiff.getLeft();
    String diffSignature = (lLeft != null ? lLeft.getName() : "");

    // Compare the current node
    if (targetSig.equals(diffSignature)) {

      // Match! Throw away the topmost element
      targetElements.pop();

      // This can happen in the following scenarios
      // 1. The target element is totally new - there are no child diff nodes
      // 2. The target element is not new but a child has changed
      // - check the child diff nodes unless we have already matched the 
      // most specific target we had
      if ((newDiff.getChildren().length == 0) ||
          targetElements.isEmpty())
      {
        return true;
      }
    }

    // Recurse into children
    IDiffElement[] lChildren = newDiff.getChildren();
    if (lChildren != null) {
      for (int i = 0; i < lChildren.length; i++) {
        IDiffElement lDiffChild = lChildren[i];
        if (lDiffChild != null) {
          boolean lRet = isStructurallyDifferent(targetElements, 
                                                 (DiffNode) lDiffChild);
          if (lRet) {
            return lRet;
          }
        }
      }
    }

    return false;
  }

  @SuppressWarnings("restriction")
    private List<Point> doLineDiff(final DiffNode fInput,
                            final ITypedElement fLeftElement,
                            final ITypedElement fRightElement)
    {
      List<Point> diffRegions = new ArrayList<Point>();
      final IDocument fLeftDoc = getDocument(fLeftElement);
      final IDocument fRightDoc = getDocument(fRightElement);
      final CompareConfiguration fConf = new CompareConfiguration();
      fConf.setProperty(CompareConfiguration.IGNORE_WHITESPACE, true);
      final CompareContainer fContainer = new CompareContainer()
      {
        @Override
        @SuppressWarnings("unused")
        public void run(boolean xiFork, boolean xiCancelable,
            IRunnableWithProgress xiRunnable) throws InvocationTargetException,
            InterruptedException {
          xiRunnable.run(new NullProgressMonitor());
        }
      };
      fConf.setContainer(fContainer);
      DocumentMerger fMerger = new DocumentMerger(new IDocumentMergerInput() {
        public ITokenComparator createTokenComparator(String line) {
          return new TokenComparator(line);
        }
        public CompareConfiguration getCompareConfiguration() {
          return fConf;
        }
        public IDocument getDocument(char contributor) {
          switch (contributor) {
          case LEFT_CONTRIBUTOR:
            return fLeftDoc;
          case RIGHT_CONTRIBUTOR:
            return fRightDoc;
          case ANCESTOR_CONTRIBUTOR:
            return null;
          }
          return null;
        }
        public int getHunkStart() {
          Object input = fInput;
          if (input != null && input instanceof DiffNode){
            ITypedElement right = ((DiffNode) input).getRight();
            if (right != null) {
              Object element = Utilities.getAdapter(right, IHunk.class);
              if (element instanceof IHunk)
                return ((IHunk)element).getStartPosition();
            }
            ITypedElement left = ((DiffNode) input).getLeft();
            if (left != null) {
              Object element = Utilities.getAdapter(left, IHunk.class);
              if (element instanceof IHunk)
                return ((IHunk)element).getStartPosition();
            }
          }
          return 0;
        }
        public Position getRegion(char contributor) {
          return null;
        }
        public boolean isHunkOnLeft() {
          ITypedElement left = ((ICompareInput)fInput).getRight();
          return left != null && Utilities.getAdapter(left, IHunk.class) != null;
        }
        public boolean isIgnoreAncestor() {
          return true;
        }
        public boolean isPatchHunk() {
          return Utilities.isHunk(fInput);
        }

        public boolean isShowPseudoConflicts() {
          return false;
        }
        public boolean isThreeWay() {
          return false;
        }
        public boolean isPatchHunkOk() {
          if (isPatchHunk())
            return Utilities.isHunkOk(fInput);
          return false;
        }

      });

      // Perform diff
      try {
        fMerger.doDiff();
      } catch (CoreException ex) {
        IStatus status = new Status(IStatus.ERROR, SvnDiffManagerActivator.ID,
            "Exception when attempting line based diff: " +
            fLeftElement.getName(), ex);
        StatusManager.getManager().handle(status);
      }

      // Translate the differences into blocks of changed lines
      Diff lOldDiff = null;
      Diff lDiff = fMerger.findNext(LEFT_CONTRIBUTOR, -1, -1, true);
      while ((lDiff != null) &&
             (lDiff != lOldDiff))
      {
        lOldDiff = lDiff;
        Position localPos = lDiff.getPosition(LEFT_CONTRIBUTOR);
        Point diffRegion = getLineRange(fLeftDoc, localPos);

        diffRegions.add(diffRegion);

        lDiff = fMerger.findNext(LEFT_CONTRIBUTOR, -1,
                                 localPos.getOffset() + localPos.getLength(),
                                 true);
      }
      return diffRegions;
    }

  /*
     * Returns the start line and the number of lines which correspond to the given position.
     * Starting line number is 0 based.
     */
    private Point getLineRange(IDocument fLeftDoc, Position p) {

      Point region = new Point(0, 0);
      IDocument doc= fLeftDoc;

      if (p == null || doc == null) {
        region.x= 0;
        region.y= 0;
        return region;
      }

      int start= p.getOffset();
      int length= p.getLength();

      int startLine= 0;
      try {
        startLine= doc.getLineOfOffset(start);
      } catch (BadLocationException e) {
        // silently ignored
      }

      int lineCount= 0;

      if (length == 0) {
        // do nothing
      } else {
        int endLine= 0;
        try {
          endLine= doc.getLineOfOffset(start + length - 1); // why -1?
        } catch (BadLocationException e) {
          // silently ignored
        }
        lineCount= endLine-startLine+1;
      }

      region.x= startLine + 1;
      region.y= lineCount;
      return region;
    }

  private int[] applyLineDiffFilter(int[] lines, List<Point> diffRegions) {
    for (int ii = 0; ii < lines.length; ii++)
    {
      // Avoid processing lines which are already filtered
      if (lines[ii] != -1)
      {
        boolean diffLine = false;
        for (Point diffRegion : diffRegions)
        {
          if ((diffRegion.x <= lines[ii]) &&
              (lines[ii] < (diffRegion.x + diffRegion.y)))
          {
            // Line is within a changed region - quit out of loop
            diffLine = true;
            break;
          }
        }
        if (!diffLine)
        {
          lines[ii] = -1;
        }
      }
    }
    return lines;
  }

  private static IJavaElement getJavaElementResource(IJavaElement xiElement)
  {
    IJavaElement currentElement = xiElement;
    IResource localResource = getResource(currentElement);
    while (localResource == null)
    {
      // May be an anonymous inner type
      currentElement = currentElement.getParent();
      if (currentElement != null)
      {
        localResource = getResource(currentElement);
      }
      else
      {
        throw new NullPointerException("Could not find element with matching resource");
      }
    }
    return currentElement;
  }

  /**
   * Returns the resource for the given input object, or null if there is no
   * resource associated with it.
   *
   * @param object
   *          the object to find the resource for
   * @return the resource for the given object, or null
   */
  private static IResource getResource(Object object) {
    if (object instanceof IResource) {
      return (IResource) object;
    }
    if (object instanceof IAdaptable) {
      return (IResource) ((IAdaptable) object).getAdapter(IResource.class);
    }
    return null;
  }

  @SuppressWarnings("restriction")
  private static String getMethodSignature(SourceMethod element)
  {
    StringBuffer elementSignature = new StringBuffer();
    element.toStringInfo(0, elementSignature);
    String elementSig = elementSignature.toString();
    elementSig = elementSig.replace("static ", "");
    if (elementSig.indexOf(" ") > -1
        && elementSig.indexOf(" ") < elementSig.indexOf("(")) {
      // Strip return type
      elementSig = elementSig.substring(elementSig.indexOf(" ") + 1);
    }
    return fixMethodSignature(elementSig);
  }

  private static String fixMethodSignature(String xiMethodSig)
  {
    String[] methodParts = xiMethodSig.split("\\(");
    String[] methodParts2 = methodParts[1].split("\\)");

    String methodName = methodParts[0];
    String methodArgs = "";
    if (methodParts2.length == 1)
    {
      methodArgs = methodParts2[0];
    }
    if ("".equals(methodArgs))
    {
      // Do nothing - no args
    }
    else if (!methodArgs.contains(","))
    {
      // Fix single arg
      if (methodArgs.contains("."))
      {
        methodArgs = methodArgs.substring(methodArgs.lastIndexOf(".") + 1);
      }
    }
    else
    {
      // Fix multiple args
      String[] args = methodArgs.split(",");
      methodArgs = "";
      for (int i = 0; i < args.length; i++)
      {
        if (args[i].contains("."))
        {
          args[i] = " " + args[i].substring(args[i].lastIndexOf(".") + 1);
        }
        methodArgs += args[i] + ",";
      }
      methodArgs = methodArgs.substring(0, methodArgs.length() - 1);
    }

    return methodName + "(" + methodArgs + ")";
  }

  @SuppressWarnings("restriction")
  private static IDocument getDocument(ITypedElement fElement) {
    IDocument newDocument = null;
    if (fElement instanceof IStreamContentAccessor) {
      newDocument= DocumentManager.get(fElement);
      if (newDocument == null) {
        IStreamContentAccessor sca = (IStreamContentAccessor)fElement;
        String s= null;

        try {
          String encoding = internalGetEncoding(sca);
          s = Utilities.readString(sca, encoding);
        } catch (CoreException ex) {
          // Silently drop
        }

        newDocument= new Document(s != null ? s : ""); //$NON-NLS-1$
        DocumentManager.put(fElement, newDocument);
      }
    }
    return newDocument;
  }

  private static String internalGetEncoding(IStreamContentAccessor fElement) {
    String fEncoding = null;
    if (fElement instanceof IEncodedStreamContentAccessor) {
      try {
        fEncoding = ((IEncodedStreamContentAccessor) fElement)
            .getCharset();
      } catch (CoreException e) {
        // silently ignored
      }
    }
    if (fEncoding != null) {
      return fEncoding;
    }
    return ResourcesPlugin.getEncoding();
  }
}
