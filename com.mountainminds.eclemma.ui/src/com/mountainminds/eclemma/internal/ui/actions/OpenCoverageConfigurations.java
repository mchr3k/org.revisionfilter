/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: OpenCoverageConfigurations.java 12 2006-08-28 20:07:13Z mho $
 ******************************************************************************/
package com.mountainminds.eclemma.internal.ui.actions;

import org.eclipse.debug.ui.actions.OpenLaunchDialogAction;

import com.mountainminds.eclemma.internal.ui.EclEmmaUIPlugin;

/**
 * 
 * @author  Marc R. Hoffmann
 * @version $Revision: 12 $
 */
public class OpenCoverageConfigurations extends OpenLaunchDialogAction  {

    public OpenCoverageConfigurations() {
        super(EclEmmaUIPlugin.ID_COVERAGE_LAUNCH_GROUP);
    }
  
}
