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
package org.eclipse.jdt.internal.ui.jarpackager;

import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IJavaElement;

/**
 * Only selects packages (i.e. IPackageFragments) which are in
 * the initial packages list and parent types (i.e. package fragment
 * root, Java project and Java model)
 */
class SealPackagesFilter  extends ViewerFilter {

	private List fAllowedPackages;	

	public SealPackagesFilter(List packages) {
		fAllowedPackages= packages;
	}
	/**
	 * Returns the result of this filter, when applied to the
	 * given inputs.
	 *
	 * @param inputs the set of elements to 
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IJavaElement) {
			int type= ((IJavaElement)element).getElementType();
			if (type == IJavaElement.JAVA_MODEL || type == IJavaElement.JAVA_PROJECT || type ==IJavaElement.PACKAGE_FRAGMENT_ROOT)
				return true;
			return (type == IJavaElement.PACKAGE_FRAGMENT && fAllowedPackages.contains(element));
			
		}
		else
			return false;
	}
}
