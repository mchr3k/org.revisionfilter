/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: CoverageContextualLaunchAction.java 384 2007-08-15 20:22:58Z mtnminds $
 ******************************************************************************/
package com.mountainminds.eclemma.internal.ui.actions;

import org.eclipse.debug.ui.actions.ContextualLaunchAction;

import com.mountainminds.eclemma.core.CoverageTools;

/**
 * An action delegate for the "Coverage As" context menu entry.
 * 
 * @author Brock Janiczak
 * @version $Revision: 384 $
 */
public class CoverageContextualLaunchAction extends ContextualLaunchAction {
  
  public CoverageContextualLaunchAction() {
    super(CoverageTools.LAUNCH_MODE);
  }
  
}
