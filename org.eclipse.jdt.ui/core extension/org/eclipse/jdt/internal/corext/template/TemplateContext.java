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
package org.eclipse.jdt.internal.corext.template;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

/**
 * A template context. A template context is associated with a context type.
 */
public abstract class TemplateContext {

	/** The context type of this context */
	private final ContextType fContextType;
	/** Additional variables. */
	private final Map fVariables= new HashMap();
	/** A flag to indicate that the context should not be modified. */
	private boolean fReadOnly;

	/**
	 * Creates a template context of a particular context type.
	 */
	protected TemplateContext(ContextType contextType) {
		fContextType= contextType;
		fReadOnly= true;
	}

	/**
	 * Returns the context type of this context.
	 */
	public ContextType getContextType() {
	 	return fContextType;   
	}
	
	/**
	 * Sets or clears the read-only flag.
	 */
	public void setReadOnly(boolean readOnly) {
		fReadOnly= readOnly;	
	}
	
	/**
	 * Returns <code>true</code> if context is read-only, <code>false</code> otherwise.
	 */
	public boolean isReadOnly() {
		return fReadOnly;	
	}
	
	/**
	 * Defines the value of a variable.
	 * @param name the name of the variable
	 * @param value the value of the variable, <code>null</code> to undefine a variable
	 */
	public void setVariable(String name, String value) {
		fVariables.put(name, value);
	}
	
	/**
	 * Returns the value of a defined variable.
	 * @param name the name of the variable
	 * @return returns the value of the variable, <code>null</code> if the variable was not defined
	 */
	public String getVariable(String name) {
		return (String) fVariables.get(name);
	}

	/**
	 * Evaluates the template and returns a template buffer.
	 * @param template the template to evaluate
	 * @return returns the buffer with the evaluated template or <code>null</code> if the buffer could not be created
	 */
	public abstract TemplateBuffer evaluate(Template template) throws CoreException;
	
	/**
	 * Tests if the specified template can be evaluated in this context.
	 */
	public abstract boolean canEvaluate(Template template);
	
}
