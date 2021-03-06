/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: IJavaCoverageListener.java 17 2006-08-28 20:49:08Z mho $
 ******************************************************************************/
package com.mountainminds.eclemma.core.analysis;

/**
 * Callback interface implemented by clients that want to be informed, when the
 * current Java model coverage has changes.
 * 
 * @author Marc R. Hoffmann
 * @version $Revision: 17 $
 */
public interface IJavaCoverageListener {

  /**
   * Called when the current coverage data has changed. 
   */
  public void coverageChanged();
  
}
