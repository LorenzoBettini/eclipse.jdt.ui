/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class OpenPackageWizardAction extends AbstractOpenWizardAction {
	
	public OpenPackageWizardAction() {
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_PACKAGE_WIZARD_ACTION);
	}
	
	public OpenPackageWizardAction(String label, Class[] acceptedTypes) {
		super(label, acceptedTypes, false);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_PACKAGE_WIZARD_ACTION);
	}
	
	protected Wizard createWizard() { 
		return new NewPackageCreationWizard();
	}
	
	protected boolean shouldAcceptElement(Object obj) { 
		return isOnBuildPath(obj) && !isInArchive(obj);
	}
}

