/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: ILazyBinding.java 199 2006-12-18 14:49:40Z mtnminds $
 ******************************************************************************/
package com.mountainminds.eclemma.internal.core.analysis;

import org.eclipse.jdt.core.IJavaElement;

/**
 * This internal interface may be implemented by coverage objects. It allows
 * binding its children lazily to the corresponding Java model elements.
 *  
 * children lazily to t
 * @author  Marc R. Hoffmann
 * @version $Revision: 199 $
 */
public interface ILazyBinding {
  
  /**
   * Called to bind the children of this coverage object lazily to the
   * corresponding Java model elements.
   * 
   * @param element  the Java element corresponding to this coverage object
   * @param modelcoverage  the coverage object for the Java model itself
   */
  public void resolve(IJavaElement element, JavaModelCoverage modelcoverage);

}
