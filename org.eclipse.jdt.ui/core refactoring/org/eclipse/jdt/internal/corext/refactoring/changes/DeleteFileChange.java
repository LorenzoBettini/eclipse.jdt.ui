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
package org.eclipse.jdt.internal.corext.refactoring.changes;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IReorgExceptionHandler;

public class DeleteFileChange extends AbstractDeleteChange {

	private IPath fPath;
	
	public DeleteFileChange(IFile file){
		Assert.isNotNull(file, "file");  //$NON-NLS-1$
		fPath= Utils.getResourcePath(file);
	}
	
	private IFile getFile(){
		return Utils.getFile(fPath);
	}
	
	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		String pattern= "Delete file ''{0}''";
		return MessageFormat.format(pattern, new String[]{fPath.lastSegment()});
	}

	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return getFile();
	}

	/* non java-doc
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(ChangeContext context, IProgressMonitor pm) throws CoreException{
		IFile file= getFile();
		Assert.isNotNull(file);
		Assert.isTrue(file.exists());
		try {
			file.delete(false, true, pm);
		} catch (CoreException e) {
			if (! (context.getExceptionHandler() instanceof IReorgExceptionHandler))
				throw e;
			IReorgExceptionHandler handler= (IReorgExceptionHandler)context.getExceptionHandler();
			IStatus[] children= e.getStatus().getChildren();
			if (children.length == 1 && children[0].getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL){
				if (handler.forceDeletingResourceOutOfSynch(file.getName(), e)){
					file.delete(true, true, pm);
					return;
				}	else
						return; //do not rethrow in this case
			} else
				throw e;
		}
	}
}

