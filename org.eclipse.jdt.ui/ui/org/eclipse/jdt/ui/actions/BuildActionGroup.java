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
package org.eclipse.jdt.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.BuildAction;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Contributes all build related actions to the context menu and installs handlers for the 
 * corresponding global menu actions.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class BuildActionGroup extends ActionGroup {

	private IWorkbenchSite fSite;
	
	private BuildAction fBuildAction;
	private BuildAction fFullBuildAction;
 	private RefreshAction fRefreshAction;

	/**
	 * Creates a new <code>BuildActionGroup</code>. The group requires that
	 * the selection provided by the view part's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public BuildActionGroup(IViewPart part) {
		fSite= part.getSite();
		Shell shell= fSite.getShell();
		ISelectionProvider provider= fSite.getSelectionProvider();
		
		fBuildAction= new BuildAction(shell, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		fBuildAction.setText(ActionMessages.getString("BuildAction.label")); //$NON-NLS-1$
		
		fFullBuildAction= new BuildAction(shell, IncrementalProjectBuilder.FULL_BUILD);
		fFullBuildAction.setText(ActionMessages.getString("RebuildAction.label")); //$NON-NLS-1$
		
		fRefreshAction= new RefreshAction(fSite);
		
		provider.addSelectionChangedListener(fBuildAction);
		provider.addSelectionChangedListener(fFullBuildAction);
		provider.addSelectionChangedListener(fRefreshAction);
	}
	
	/**
	 * Returns the refresh action managed by this group.
	 * 
	 * @return the refresh action. If this group doesn't manage a refresh action
	 * 	<code>null</code> is returned
	 */
	public IAction getRefreshAction() {
		return fRefreshAction;
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		setGlobalActionHandlers(actionBar);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		ISelection selection= getContext().getSelection();
		if (!ResourcesPlugin.getWorkspace().isAutoBuilding() && isBuildTarget(selection)) {
			appendToGroup(menu, fBuildAction);
		}
		appendToGroup(menu, fRefreshAction);
		super.fillContextMenu(menu);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		provider.removeSelectionChangedListener(fBuildAction);
		provider.removeSelectionChangedListener(fFullBuildAction);
		provider.removeSelectionChangedListener(fRefreshAction);
		super.dispose();
	}	
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		actionBar.setGlobalActionHandler(IWorkbenchActionConstants.BUILD_PROJECT, fBuildAction);
		actionBar.setGlobalActionHandler(IWorkbenchActionConstants.REBUILD_PROJECT, fFullBuildAction);
		actionBar.setGlobalActionHandler(IWorkbenchActionConstants.REFRESH, fRefreshAction);
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, action);
	}
	
	private boolean isBuildTarget(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return false;
		IStructuredSelection selection= (IStructuredSelection)s;
		if (selection.size() != 1)
			return false;
		return selection.getFirstElement() instanceof IJavaProject;
	}
}
