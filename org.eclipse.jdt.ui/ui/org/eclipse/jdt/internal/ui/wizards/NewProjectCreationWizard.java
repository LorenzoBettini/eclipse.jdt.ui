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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class NewProjectCreationWizard extends NewElementWizard implements IExecutableExtension {

	public static final String NEW_PROJECT_WIZARD_ID= "org.eclipse.jdt.ui.wizards.NewProjectCreationWizard"; //$NON-NLS-1$
		
	private NewProjectCreationWizardPage fJavaPage;
	private WizardNewProjectCreationPage fMainPage;
	private IConfigurationElement fConfigElement;
	
	public NewProjectCreationWizard() {
		super();
		
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.getString("NewProjectCreationWizard.title")); //$NON-NLS-1$
	}

	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fMainPage= new WizardNewProjectCreationPage("NewProjectCreationWizard"); //$NON-NLS-1$
		fMainPage.setTitle(NewWizardMessages.getString("NewProjectCreationWizard.MainPage.title")); //$NON-NLS-1$
		fMainPage.setDescription(NewWizardMessages.getString("NewProjectCreationWizard.MainPage.description")); //$NON-NLS-1$
		addPage(fMainPage);
		fJavaPage= new NewProjectCreationWizardPage(fMainPage);
		addPage(fJavaPage);
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		fJavaPage.performFinish(monitor); // use the full progress monitor
		BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
		selectAndReveal(fJavaPage.getJavaProject().getProject());
	}
	
	protected void handleFinishException(Shell shell, InvocationTargetException e) {
		String title= NewWizardMessages.getString("NewProjectCreationWizard.op_error.title"); //$NON-NLS-1$
		String message= NewWizardMessages.getString("NewProjectCreationWizard.op_error_create.message");			 //$NON-NLS-1$
		ExceptionHandler.handle(e, getShell(), title, message);
	}	
			
	/*
	 * Stores the configuration element for the wizard.  The config element will be used
	 * in <code>performFinish</code> to set the result perspective.
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		fConfigElement= cfig;
	}
	
	/* (non-Javadoc)
	 * @see IWizard#performCancel()
	 */
	public boolean performCancel() {
		fJavaPage.performCancel();
		return super.performCancel();
	}


		

}
