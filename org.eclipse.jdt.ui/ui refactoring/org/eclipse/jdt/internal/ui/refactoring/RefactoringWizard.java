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

package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class RefactoringWizard extends Wizard {

	private String fPageTitle;
	private Refactoring fRefactoring;
	private IChange fChange;
	private RefactoringStatus fActivationStatus= new RefactoringStatus();
	private RefactoringStatus fStatus;
	private boolean fHasUserInputPages;
	private boolean fExpandFirstNode;
	private boolean fIsChangeCreationCancelable;
	private boolean fPreviewReview;
	private boolean fPreviewShown;
	
	private String fErrorPageContextHelpId;
	
	public RefactoringWizard(Refactoring ref, String pageTitle, String errorPageContextHelpId) {
		setNeedsProgressMonitor(true);
		Assert.isNotNull(pageTitle);
		Assert.isNotNull(ref);
		fRefactoring= ref;
		fPageTitle= pageTitle;
		fErrorPageContextHelpId= errorPageContextHelpId;
		fIsChangeCreationCancelable= true;
		
		setWindowTitle(RefactoringMessages.getString("RefactoringWizard.title")); //$NON-NLS-1$
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR);
	}

	public void setChangeCreationCancelable(boolean isChangeCreationCancelable){
		fIsChangeCreationCancelable= isChangeCreationCancelable;
	}
	
	//---- Hooks to overide ---------------------------------------------------------------

	/**
	 * Some refactorings do activation checking when the wizard is going to be opened. 
	 * They do this since activation checking is expensive and can't be performed on 
	 * opening a corresponding menu. Wizards that need activation checking on opening
	 * should reimplement this method and should return <code>true</code>. This default
	 * implementation returns <code>false</code>.
	 *
	 * @return <code>true<code> if activation checking should be performed on opening;
	 *  otherwise <code>false</code> is returned
	 */
	protected boolean checkActivationOnOpen() {
		return false;
	}
	 
	/**
	 * Hook to add user input pages to the wizard. This default implementation 
	 * adds nothing.
	 */
	protected void addUserInputPages(){
	}
	
	/**
	 * Hook to add the error page to the wizard. This default implementation 
	 * adds an <code>ErrorWizardPage</code> to the wizard.
	 */
	protected void addErrorPage(){
		addPage(new ErrorWizardPage(fErrorPageContextHelpId));
	}
	
	/**
	 * Hook to add the page the gives a prefix of the changes to be performed. This default 
	 * implementation  adds a <code>PreviewWizardPage</code> to the wizard.
	 */
	protected void addPreviewPage(){
		addPage(new PreviewWizardPage());
	}
	
	/**
	 * Hook to determine if the wizard has more than one user input page without
	 * actually creating the pages.
	 * 
	 * @return boolean <code>true<code> if multi page user input exists.
	 * Otherwise <code>false</code> is returned
	 */
	public boolean hasMultiPageUserInput() {
		return false;
	}
	
	protected int getMessageLineWidthInChars() {
		return 80;
	}
	
	//---- Setter and Getters ------------------------------------------------------------
	
	/**
	 * Returns the refactoring this wizard is using.
	 */	
	public Refactoring getRefactoring(){
		return fRefactoring;
	}
	
	public boolean hasUserInputPages(){
		return fHasUserInputPages;		
	}

	/**
	 * Sets the change object.
	 */
	public void setChange(IChange change){
		IPreviewWizardPage page= (IPreviewWizardPage)getPage(PreviewWizardPage.PAGE_NAME);
		if (page != null)
			page.setChange(change);
		fChange= change;
	}

	/**
	 * Returns the current change object.
	 */
	public IChange getChange() {
		return fChange;
	}
	
	/**
	 * Sets the refactoring status.
	 * 
	 * @param status the refactoring status to set.
	 */
	public void setStatus(RefactoringStatus status) {
		ErrorWizardPage page= (ErrorWizardPage)getPage(ErrorWizardPage.PAGE_NAME);
		if (page != null)
			page.setStatus(status);
		fStatus= status;
	}
	
	/**
	 * Returns the current refactoring status.
	 */
	public RefactoringStatus getStatus() {
		return fStatus;
	} 
	
	/**
	 * Sets the refactoring status returned from input checking. Any previously 
	 * computed activation status is merged into the given status before it is set 
	 * to the error page.
	 * 
	 * @param status the input status to set.
	 * @see #getActivationStatus()
	 */
	public void setInputStatus(RefactoringStatus status) {
		RefactoringStatus newStatus= new RefactoringStatus();
		if (fActivationStatus != null)
			newStatus.merge(fActivationStatus);
		newStatus.merge(status);	
		setStatus(newStatus);			
	}
	
	/**
	 * Sets the refactoring status returned from activation checking.
	 * 
	 * @param status the activation status to be set.
	 */
	public void setActivationStatus(RefactoringStatus status) {
		fActivationStatus= status;
		setStatus(status);
	}
		
	/**
	 * Returns the activation status computed during the start up off this
	 * wizard. This methdod returns <code>null</code> if no activation
	 * checking has been performed during startup.
	 * 
	 * @return the activation status computed during startup.
	 */
	public RefactoringStatus getActivationStatus() {
		return fActivationStatus;
	}
	
	/**
	 * Returns the default page title used for this refactoring.
	 */
	public String getPageTitle() {
		return fPageTitle;
	}
	
	/**
	 * Set the default page title used for this refactoring.
	 */
	public void setPageTitle(String title) {
		fPageTitle= title;
		setupPageTitles();
	}
	 
	/**
	 * Defines whether the frist node in the preview page is supposed to be expanded.
	 * 
	 * @param expand <code>true</code> if the first node is to be expanded. Otherwise
	 *  <code>false</code>
	 */
	public void setExpandFirstNode(boolean expand) {
		fExpandFirstNode= true;
	}
	
	/**
	 * Returns <code>true</code> if the first node in the preview page is supposed to be
	 * expanded. Otherwise <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if the first node in the preview page is supposed to be
	 * 	expanded; otherwise <code>false</code>
	 */
	public boolean getExpandFirstNode() {
		return fExpandFirstNode;
	}
	
	/**
	 * Computes the wizard page that should follow the user input page. This is
	 * either the error page or the proposed changes page, depending on the
	 * result of the condition checking.
	 * 
	 * @return the wizard page that should be shown after the last user input
	 *  page
	 */
	public IWizardPage computeUserInputSuccessorPage(IWizardPage caller) {
		return computeUserInputSuccessorPage(caller, getContainer());
	}

	private IWizardPage computeUserInputSuccessorPage(IWizardPage caller, IRunnableContext context) {
		IChange change= createChange(CheckConditionsOperation.INPUT, RefactoringStatus.OK, true, context);
		// Status has been updated since we have passed true
		RefactoringStatus status= getStatus();
		
		// Creating the change has been canceled
		if (change == null && status == null) {		
			setChange(change);
			return caller;
		}
				
		// Set change if we don't have fatal errors.
		if (!status.hasFatalError())
			setChange(change);
		
		if (status.isOK()) {
			return getPage(PreviewWizardPage.PAGE_NAME);
		} else {
			return getPage(ErrorWizardPage.PAGE_NAME);
		}
	} 
	
	/**
	 * Initialize all pages with the managed page title.
	 */
	protected void setupPageTitles() {
		if (fPageTitle == null)
			return;
			
		IWizardPage[] pages= getPages();
		for (int i= 0; i < pages.length; i++) {
			pages[i].setTitle(fPageTitle);
		}
	}
	
	/**
	 * Forces the visiting of the preview page. The OK/Finish button will be
	 * disabled until the user has reached the preview page.
	 */
	public void setPreviewReview(boolean review) {
		fPreviewReview= review;
		getContainer().updateButtons();	
	}
	
	public void setPreviewShown(boolean shown) {
		fPreviewShown= shown;
		getContainer().updateButtons();
	}
	
	public boolean canFinish() {
		if (fPreviewReview && !fPreviewShown)
			return false;
		return super.canFinish();
	}


	//---- Change management -------------------------------------------------------------

	/**
	 * Creates a new change object for the refactoring. Method returns <code>
	 * null</code> if the change cannot be created.
	 * 
	 * @param style the conditions to check before creating the change.
	 * @param checkPassedSeverity the severity below which the conditions check
	 *  is treated as 'passed'
	 * @param updateStatus if <code>true</code> the wizard's status is updated
	 *  with the status returned from the <code>CreateChangeOperation</code>.
	 *  if <code>false</code> no status updating is performed.
	 */
	IChange createChange(int style, int checkPassedSeverity, boolean updateStatus) {
		return createChange(style, checkPassedSeverity, updateStatus, getContainer());
	}

	private IChange createChange(int style, int checkPassedSeverity, boolean updateStatus, IRunnableContext context){
		CreateChangeOperation op= new CreateChangeOperation(fRefactoring, style);
		op.setCheckPassedSeverity(checkPassedSeverity); 

		InvocationTargetException exception= null;
		try {
			context.run(true, fIsChangeCreationCancelable, op);
		} catch (InterruptedException e) {
			setStatus(null);
			return null;
		} catch (InvocationTargetException e) {
			exception= e;
		}
		
		if (updateStatus) {
			RefactoringStatus status= null;
			if (exception != null) {
				status= new RefactoringStatus();
				String msg= exception.getMessage();
				if (msg != null) {
					status.addFatalError(RefactoringMessages.getFormattedString("RefactoringWizard.see_log", msg)); //$NON-NLS-1$
				} else {
					status.addFatalError(RefactoringMessages.getString("RefactoringWizard.Internal_error")); //$NON-NLS-1$
				}
				JavaPlugin.log(exception);
			} else {
				status= op.getStatus();
			}
			setStatus(status, style);
		} else {
			if (exception != null)
				ExceptionHandler.handle(exception, RefactoringMessages.getString("RefactoringWizard.refactoring"), RefactoringMessages.getString("RefactoringWizard.unexpected_exception")); //$NON-NLS-2$ //$NON-NLS-1$
		}
		IChange change= op.getChange();	
		return change;
	}

	public boolean performFinish(PerformChangeOperation op) {
		return PerformRefactoringUtil.performRefactoring(op, fRefactoring, getContainer(), getContainer().getShell());
	}
	
	//---- Condition checking ------------------------------------------------------------

	public RefactoringStatus checkInput() {
		return internalCheckCondition(getContainer(), CheckConditionsOperation.INPUT);
	}
	
	/**
	 * Checks the condition for the given style.
	 * @param style the conditions to check.
	 * @return the result of the condition check.
	 * @see CheckPreconditionsOperation
	 */
	protected RefactoringStatus internalCheckCondition(IRunnableContext context, int style) {
		
		CheckConditionsOperation op= new CheckConditionsOperation(fRefactoring, style); 

		Exception exception= null;
		try {
			context.run(true, true, op);
		} catch (InterruptedException e) {
			exception= e;
		} catch (InvocationTargetException e) {
			exception= e;
		}
		RefactoringStatus status= null;
		if (exception != null) {
			JavaPlugin.log(exception);
			status= new RefactoringStatus();
			status.addFatalError(RefactoringMessages.getString("RefactoringWizard.internal_error_1")); //$NON-NLS-1$
		} else {
			status= op.getStatus();
		}
		setStatus(status, style);
		return status;	
	}
	
	/**
	 * Sets the status according to the given style flag.
	 * 
	 * @param status the refactoring status to set.
	 * @param style a flag indicating if the status is a activation, input checking, or
	 *  precondition checking status.
	 * @see CheckConditionsOperation
	 */
	protected void setStatus(RefactoringStatus status, int style) {
		if ((style & CheckConditionsOperation.PRECONDITIONS) == CheckConditionsOperation.PRECONDITIONS)
			setStatus(status);
		else if ((style & CheckConditionsOperation.ACTIVATION) == CheckConditionsOperation.ACTIVATION)
			setActivationStatus(status);
		else if ((style & CheckConditionsOperation.INPUT) == CheckConditionsOperation.INPUT)
			setInputStatus(status);
	}

	
	//---- Reimplementation of Wizard methods --------------------------------------------

	public boolean performFinish() {
		Assert.isNotNull(fRefactoring);
		
		RefactoringWizardPage page= (RefactoringWizardPage)getContainer().getCurrentPage();
		return page.performFinish();
	}
	
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (fHasUserInputPages)
			return super.getPreviousPage(page);
		if (! page.getName().equals(ErrorWizardPage.PAGE_NAME)){
			if (fStatus.isOK())
				return null;
		}		
		return super.getPreviousPage(page);		
	}

	public IWizardPage getStartingPage() {
		if (fHasUserInputPages)
			return super.getStartingPage();
		return computeUserInputSuccessorPage(null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
	}
	
	public void addPages() {
		if (checkActivationOnOpen()) {
			internalCheckCondition(new BusyIndicatorRunnableContext(), CheckConditionsOperation.ACTIVATION);
		}
		if (fActivationStatus.hasFatalError()) {
			addErrorPage();
			// Set the status since we added the error page
			setStatus(getStatus());	
		} else { 
			Assert.isTrue(getPageCount() == 0);
			addUserInputPages();
			if (getPageCount() > 0)
				fHasUserInputPages= true;
			addErrorPage();
			addPreviewPage();	
		}
		setupPageTitles();
	}
	
	public void addPage(IWizardPage page) {
		Assert.isTrue(page instanceof RefactoringWizardPage);
		super.addPage(page);
	}
}
