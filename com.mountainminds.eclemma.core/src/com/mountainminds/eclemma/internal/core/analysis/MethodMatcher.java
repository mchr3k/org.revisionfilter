package com.mountainminds.eclemma.internal.core.analysis;

import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.mountainminds.eclemma.internal.core.EclEmmaCorePlugin;
import com.vladium.emma.data.MethodDescriptor;

public class MethodMatcher {

  private final IType type;
  private final IJavaElement[] children;
  private final boolean[][] covered;
  private final MethodDescriptor[] methods;  
  private final int staticInitInd;
  
  private int childInd = 0;
  private int methodInd = 0;
  
  private boolean matched = false;
  private boolean matchedStatic = false;
    
  public MethodMatcher(IType type, boolean[][] covered, MethodDescriptor[] methods) throws JavaModelException {
    this.type = type;
    this.children = type.getChildren();
    this.covered = covered;
    this.methods = methods;
    
//    ArrayList mdlist = new ArrayList();
//    for (int ii = 0; ii < methods.length; ii++)
//    {
//      MethodDescriptor methodEle = methods[ii];
//      if (methodEle.getName().startsWith("access$"))
//      {
//        // Ignore
//      }
//      else
//      {
//        mdlist.add(methodEle.getName() + ":" + methodEle.getDescriptor());
//      }
//    }
//    ArrayList jelist = new ArrayList();
//    for (int ii = 0; ii < children.length; ii++)
//    {
//      IJavaElement childEle = children[ii];
//      if ((childEle.getElementType() == IJavaElement.METHOD) || 
//          (childEle.getElementType() == IJavaElement.INITIALIZER)) 
//      {
//        jelist.add(childEle.toString());
//      }
//    }
//    
//    System.out.println(jelist.get(0));
//    System.out.println();
//    
//    for (int ii = 0; ii < Math.max(jelist.size(), mdlist.size()); ii++)
//    {
//      if (ii < mdlist.size())
//      {
//        System.out.println(mdlist.get(ii));
//      }
//      if ((ii + 1) < jelist.size())
//      {
//        System.out.println(jelist.get(ii + 1));
//      }
//      System.out.println();
//    }
    
    // Extract the static init descriptor if there is one
    int staticInit = -1;
    for (int i = 0; i < methods.length; i++)      
    {
      if ("<clinit>".equals(methods[i].getName())) //$NON-NLS-1$
      {
        staticInit = i;
        break;
      }
    }
    staticInitInd = staticInit;
  }

  public boolean methodMatched() {

    if (matchedStatic)
    {
      childInd++;
      matchedStatic = false;
    }
    
    if (matched)
    {
      childInd++;
      methodInd++;
      matched = false;
    }
    
    if (methodInd == staticInitInd)
    {
      methodInd++;
    }
    
    // Iterate over methods to skip generated access methods
    while ((methodInd < methods.length) &&
           methods[methodInd].getName().startsWith("access$")) //$NON-NLS-1$
    {
      methodInd++;
    }
    
    // Guard against reaching the end of the array
    if ((methodInd >= methods.length))
    {
      matched = false;
      return false;
    }
    
    for (; childInd < children.length; childInd++) {
      
      IJavaElement childEle = children[childInd];
      MethodDescriptor methodDesc = methods[methodInd];
      
      // children[] includes more than just methods
      // methods[] contains descriptors for all the methods and
      // initializer code within the Class
      // We can match up these elements by skipping over elements
      // of children[] which are not methods or initializers.
      // This logic relies on the fact that these two arrays
      // hold the methods in the same order.
      if ((childEle.getElementType() == IJavaElement.METHOD) || 
          (childEle.getElementType() == IJavaElement.INITIALIZER)) {
                
        if (methodInd >= methods.length)
        {
          logNotEnoughMethodDescriptors();
          return false;
        }
        
        // Special case for static init blocks. These all map to a single
        // static method descriptor
        if (childEle.toString().startsWith("<static initializer ")) //$NON-NLS-1$
        {
          matchedStatic = true;
          return true;
        }
        
        if ("<init>".equals(methodDesc.getName()))//$NON-NLS-1$            
        {
          // If the child ele is an initializer we just keep searching as we should
          // find a constructor soon
          if (childEle.getElementType() == IJavaElement.INITIALIZER)
          {
            continue;
          }
          // Special case where we have a method descriptor for the
          // implicit no-args constructor but no corresponding java
          // element.
          else if(!type.getElementName().equals(childEle.getElementName()))
          {
            // Skip over this descriptor
            methodInd++;
          }
        }
        
        if (methodInd >= methods.length)
        {
          logNotEnoughMethodDescriptors();
          return false;
        }

        matched = true;
        return true;
      }
    }
    return false;
  }

  private void logNotEnoughMethodDescriptors() {
    IStatus status = new Status(IStatus.WARNING, 
                                EclEmmaCorePlugin.ID,
                                IStatus.ERROR,
      "Fewer coverage descriptors than expected when examining type: " + type + "\n" +   //$NON-NLS-1$//$NON-NLS-2$
      "Method Descriptors: " + toStringDescriptors(methods) + "\n" +  //$NON-NLS-1$ //$NON-NLS-2$
      "Java Elements: " + Arrays.asList(children) + "\n",  //$NON-NLS-1$ //$NON-NLS-2$
      new Exception());
    EclEmmaCorePlugin.getInstance().getLog().log(status);
  }

  private String toStringDescriptors(MethodDescriptor[] methods) {
    String ret = "["; //$NON-NLS-1$
    for (int ii = 0; ii < methods.length; ii++)
    {
      MethodDescriptor method = methods[ii];
      ret += method.getName() + method.getDescriptor();
      if (ii < methods.length - 1)
      {
        ret += ","; //$NON-NLS-1$
      }
    }
    ret += "]"; //$NON-NLS-1$
    return ret;
  }

  public IJavaElement getMatchedElement() {
    return children[childInd];
  }

  public MethodDescriptor getMatchedDescriptor() {
    return matchedStatic ? methods[staticInitInd] : methods[methodInd];
  }

  public boolean[] getMatchedCoverageData() {
    return ((covered == null) ? null : 
            (matchedStatic ? covered[staticInitInd] : covered[methodInd]));
  }

}
