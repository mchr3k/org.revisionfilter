/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.tobject.findbugs.decorators;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import de.tobject.findbugs.builder.ResourceUtils;
import de.tobject.findbugs.builder.WorkItem;

/**
 * A simple decorator which adds (in currently hardcoded way) bug counts to the resources.
 * There are 3 different decorators configured via plugin.xml (project/folder/file),
 * current implementation is the same for all.
 *
 * @author Andrei
 */
public class ResourceBugCountDecorator implements ILabelDecorator {

	public Image decorateImage(Image image, Object element) {
		return null;
	}

	public String decorateText(String text, Object element) {
		WorkItem item = ResourceUtils.getWorkItem(element);
		int markerCount = item.getMarkerCount(false);
		if(markerCount == 0){
			return text;
		}
		return text + " (" + markerCount + ")";
	}

	public void addListener(ILabelProviderListener listener) {
		// noop
	}

	public void dispose() {
		// noop
	}

	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	public void removeListener(ILabelProviderListener listener) {
		// noop
	}

}
