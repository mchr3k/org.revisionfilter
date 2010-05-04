/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: JavaApplicationLauncher.java 378 2007-08-13 15:33:00Z mtnminds $
 ******************************************************************************/
package com.mountainminds.eclemma.core.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.mountainminds.eclemma.internal.core.launching.InstrumentedClasspathProvider;

/**
 * Launcher for local Java applications.
 * 
 * @author  Marc R. Hoffmann
 * @version $Revision: 378 $
 */
public class JavaApplicationLauncher extends CoverageLauncher {

  protected void modifyConfiguration(ILaunchConfigurationWorkingCopy workingcopy,
      ICoverageLaunchInfo info) throws CoreException {
    workingcopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
        InstrumentedClasspathProvider.ID);
  }

}
