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
package org.eclipse.jdt.internal.junit.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.*;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * The wizard base class for JUnit creation wizards.
 */
public abstract class JUnitWizard extends Wizard {

	private IWorkbench fWorkbench;
	protected static String DIALOG_SETTINGS_KEY= "JUnitWizards"; //$NON-NLS-1$
	private IStructuredSelection fSelection;

	public JUnitWizard() {
		setNeedsProgressMonitor(true);
	}
	
	/*
	 * @see IWizard#performFinish()
	 */
	public abstract boolean performFinish();

	/**
	 * Run a runnable
	 */	
	protected boolean finishPage(IRunnableWithProgress runnable) {
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(runnable);
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			Shell shell= getShell();
			String title= WizardMessages.getString("NewJUnitWizard.op_error.title"); //$NON-NLS-1$
			String message= WizardMessages.getString("NewJUnitWizard.op_error.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, shell, title, message);
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}

	protected void openResource(final IResource resource) {
		if (resource.getType() == IResource.FILE) {
			final IWorkbenchPage activePage= JUnitPlugin.getDefault().getActivePage();
			if (activePage != null) {
				final Display display= getShell().getDisplay();
				if (display != null) {
					display.asyncExec(new Runnable() {
						public void run() {
							try {
								activePage.openEditor((IFile)resource);
							} catch (PartInitException e) {
								JUnitPlugin.log(e);
							}
						}
					});
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		fWorkbench= workbench;
		fSelection= currentSelection;
	}
	
	public IStructuredSelection getSelection() {
		return fSelection;
	}

	protected void selectAndReveal(IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, fWorkbench.getActiveWorkbenchWindow());
	} 
	
	protected void initDialogSettings() {
		IDialogSettings pluginSettings= JUnitPlugin.getDefault().getDialogSettings();
		IDialogSettings wizardSettings= pluginSettings.getSection(DIALOG_SETTINGS_KEY);
		if (wizardSettings == null) {
			wizardSettings= new DialogSettings(DIALOG_SETTINGS_KEY);
			pluginSettings.addSection(wizardSettings);
		}
		setDialogSettings(wizardSettings);
	}
}
