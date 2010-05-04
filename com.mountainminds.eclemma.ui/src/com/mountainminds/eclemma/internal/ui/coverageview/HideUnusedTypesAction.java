/*******************************************************************************
 * Copyright (c) 2006, 2007 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: HideUnusedTypesAction.java 393 2007-08-28 13:05:34Z mtnminds $
 ******************************************************************************/
package com.mountainminds.eclemma.internal.ui.coverageview;

import org.eclipse.jface.action.Action;

import com.mountainminds.eclemma.internal.ui.UIMessages;

/**
 * Action to hide types not loaded at all. Internally used by the coverage view.
 *  
 * @author  Marc R. Hoffmann
 * @version $Revision: 393 $
 */
class HideUnusedTypesAction extends Action {

  private final ViewSettings settings;
  private final CoverageView view;
  
  HideUnusedTypesAction(ViewSettings settings, CoverageView view) {
    super(UIMessages.CoverageViewHideUnusedTypesAction_label, AS_CHECK_BOX);
    this.settings = settings;
    this.view = view;
    setChecked(settings.getHideUnusedTypes());
  }

  public void run() {
    settings.setHideUnusedTypes(isChecked());
    view.refreshViewer();
  }

}
