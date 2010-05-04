/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.io.File;

public final class FindBugsAnalysisFileFilter extends FindBugsFileFilter {

	public static final  FindBugsAnalysisFileFilter INSTANCE = new FindBugsAnalysisFileFilter();
	@Override
	public boolean accept(File arg0) {
		boolean accept = arg0.getName().endsWith(".xml") || 
		(OriginalGUI2ProjectFile.fileContainingXMLData(arg0).exists()) ||
		arg0.isDirectory();
		return accept;
	}

	@Override
	public String getDescription() {
		return "FindBugs analysis results (.xml)";
	}

    @Override
    SaveType getSaveType() {
    	return SaveType.XML_ANALYSIS;
    }
}
