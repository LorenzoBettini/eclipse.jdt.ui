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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.util.Assert;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.jarpackager.IJarDescriptionWriter;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.IJavaStatusConstants;

import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;

/**
 * Writes a JarPackage to an underlying OutputStream
 */
public class JarPackageWriter extends Object implements IJarDescriptionWriter {
	
	protected OutputStream fOutputStream;
	
	/**
	 * Create a JarPackageWriter on the given output stream.
	 * It is the clients responsibility to close the output stream.
	 */
	public JarPackageWriter(OutputStream outputStream) {
		Assert.isNotNull(outputStream);
		fOutputStream= new BufferedOutputStream(outputStream);
	}
	
	public void write(JarPackageData jarPackage) throws CoreException {
		try  {
			writeXML(jarPackage);
		} catch (IOException ex) {
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, ex.getLocalizedMessage(), ex));
		}
	}

	/**
	 * Writes a XML representation of the JAR specification
	 * to to the underlying stream.
	 * 
	 * @exception IOException	if writing to the underlying stream fails
	 */
	public void writeXML(JarPackageData jarPackage) throws IOException {
		Assert.isNotNull(jarPackage);
		DocumentBuilder docBuilder= null;
		DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		try {   	
	    	docBuilder= factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IOException(JarPackagerMessages.getString("JarWriter.error.couldNotGetXmlBuilder")); //$NON-NLS-1$
		}
		Document document= docBuilder.newDocument();
		
		// Create the document
		Element xmlJarDesc= document.createElement(JarPackagerUtil.DESCRIPTION_EXTENSION);
		document.appendChild(xmlJarDesc);
		xmlWriteJarLocation(jarPackage, document, xmlJarDesc);
		xmlWriteOptions(jarPackage, document, xmlJarDesc);
		if (jarPackage.areClassFilesExported())
			xmlWriteManifest(jarPackage, document, xmlJarDesc);
		xmlWriteSelectedElements(jarPackage, document, xmlJarDesc);

		// Write the document to the stream
		OutputFormat format= new OutputFormat();
		format.setIndenting(true);
		SerializerFactory serializerFactory= SerializerFactory.getSerializerFactory(Method.XML);
		Serializer serializer= serializerFactory.makeSerializer(fOutputStream,	format);
		serializer.asDOMSerializer().serialize(document);
	}

	private void xmlWriteJarLocation(JarPackageData jarPackage, Document document, Element xmlJarDesc) throws DOMException {
		Element jar= document.createElement(JarPackagerUtil.JAR_EXTENSION);
		xmlJarDesc.appendChild(jar);
		jar.setAttribute("path", jarPackage.getJarLocation().toString()); //$NON-NLS-1$
	}

	private void xmlWriteOptions(JarPackageData jarPackage, Document document, Element xmlJarDesc) throws DOMException {
		Element options= document.createElement("options"); //$NON-NLS-1$
		xmlJarDesc.appendChild(options);
		options.setAttribute("overwrite", "" + jarPackage.allowOverwrite()); //$NON-NLS-2$ //$NON-NLS-1$
		options.setAttribute("compress", "" + jarPackage.isCompressed()); //$NON-NLS-2$ //$NON-NLS-1$
		options.setAttribute("exportErrors", "" + jarPackage.areErrorsExported()); //$NON-NLS-2$ //$NON-NLS-1$
		options.setAttribute("exportWarnings", "" + jarPackage.exportWarnings()); //$NON-NLS-2$ //$NON-NLS-1$
		options.setAttribute("saveDescription", "" + jarPackage.isDescriptionSaved()); //$NON-NLS-2$ //$NON-NLS-1$
		options.setAttribute("descriptionLocation", jarPackage.getDescriptionLocation().toString()); //$NON-NLS-1$
		options.setAttribute("useSourceFolders", "" + jarPackage.useSourceFolderHierarchy()); //$NON-NLS-2$ //$NON-NLS-1$
		options.setAttribute("buildIfNeeded", "" + jarPackage.isBuildingIfNeeded()); //$NON-NLS-2$ //$NON-NLS-1$
	}

	private void xmlWriteManifest(JarPackageData jarPackage, Document document, Element xmlJarDesc) throws DOMException {
		Element manifest= document.createElement("manifest"); //$NON-NLS-1$
		xmlJarDesc.appendChild(manifest);
		manifest.setAttribute("manifestVersion", jarPackage.getManifestVersion()); //$NON-NLS-1$
		manifest.setAttribute("usesManifest", "" + jarPackage.usesManifest()); //$NON-NLS-2$ //$NON-NLS-1$
		manifest.setAttribute("reuseManifest", "" + jarPackage.isManifestReused()); //$NON-NLS-2$ //$NON-NLS-1$
		manifest.setAttribute("saveManifest", "" + jarPackage.isManifestSaved()); //$NON-NLS-2$ //$NON-NLS-1$
		manifest.setAttribute("generateManifest", "" + jarPackage.isManifestGenerated()); //$NON-NLS-2$ //$NON-NLS-1$
		manifest.setAttribute("manifestLocation", jarPackage.getManifestLocation().toString()); //$NON-NLS-1$
		if (jarPackage.getManifestMainClass() != null)
			manifest.setAttribute("mainClassHandleIdentifier", jarPackage.getManifestMainClass().getHandleIdentifier()); //$NON-NLS-1$
		xmlWriteSealingInfo(jarPackage, document, manifest);
	}

	private void xmlWriteSealingInfo(JarPackageData jarPackage, Document document, Element manifest) throws DOMException {
		Element sealing= document.createElement("sealing"); //$NON-NLS-1$
		manifest.appendChild(sealing);
		sealing.setAttribute("sealJar", "" + jarPackage.isJarSealed()); //$NON-NLS-2$ //$NON-NLS-1$
		Element packagesToSeal= document.createElement("packagesToSeal"); //$NON-NLS-1$
		sealing.appendChild(packagesToSeal);
		add(jarPackage.getPackagesToSeal(), packagesToSeal, document);
		Element packagesToUnSeal= document.createElement("packagesToUnSeal"); //$NON-NLS-1$
		sealing.appendChild(packagesToUnSeal);
		add(jarPackage.getPackagesToUnseal(), packagesToUnSeal, document);
	}

	private void xmlWriteSelectedElements(JarPackageData jarPackage, Document document, Element xmlJarDesc) throws DOMException {
		Element selectedElements= document.createElement("selectedElements"); //$NON-NLS-1$
		xmlJarDesc.appendChild(selectedElements);
		selectedElements.setAttribute("exportClassFiles", "" + jarPackage.areClassFilesExported()); //$NON-NLS-2$ //$NON-NLS-1$
		selectedElements.setAttribute("exportJavaFiles", "" + jarPackage.areJavaFilesExported()); //$NON-NLS-2$ //$NON-NLS-1$
		Object[] elements= jarPackage.getElements();
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			if (element instanceof IResource)
				add((IResource)element, selectedElements, document);
			else if (element instanceof IJavaElement)
				add((IJavaElement)element, selectedElements, document);
			// Note: Other file types are not handled by this writer
		}
	}

	/**
     * Writes a String representation of the JAR specification
     * to to the underlying stream.
     * @exception IOException	Writing to the underlying stream.
     */
    public void writeString(JarPackageData jarPackage) throws IOException {
    	Assert.isNotNull(jarPackage);
		OutputStreamWriter streamWriter= new OutputStreamWriter(fOutputStream);
		BufferedWriter writer= new BufferedWriter(streamWriter);
		writer.write(JarPackagerMessages.getString("JarWriter.output.title")); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.exportBin", jarPackage.areClassFilesExported())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.exportJava", jarPackage.areJavaFilesExported())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.jarFileName", jarPackage.getJarLocation().toOSString())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.compressed", jarPackage.isCompressed())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.overwrite", jarPackage.allowOverwrite())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.saveDescription", jarPackage.isDescriptionSaved())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.descriptionFile", jarPackage.getDescriptionLocation())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getString("JarWriter.output.lineSeparator")); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.generateManifest", jarPackage.isManifestGenerated())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.saveManifest", jarPackage.isManifestSaved())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.reuseManifest", jarPackage.isManifestReused())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.manifestName", jarPackage.getManifestLocation())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.jarSealed", jarPackage.isJarSealed())); //$NON-NLS-1$
		writer.newLine();
		writer.write(JarPackagerMessages.getFormattedString("JarWriter.output.mainClass", JarPackagerUtil.getMainClassName(jarPackage))); //$NON-NLS-1$
		writer.flush();
    }

	/**
     * Closes this stream.
     * It is the client's responsibility to close the stream.
     * 
     * @throws CoreException
     */
    public void close() throws CoreException {
    	if (fOutputStream != null) {
			try {
				fOutputStream.close();
			} catch (IOException ex) {
				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, ex.getLocalizedMessage(), ex));
			}
    	}
	}

	private void add(IResource resource, Element parent, Document document) {
		Element element= null;
		if (resource.getType() == IResource.PROJECT) {
			element= document.createElement("project"); //$NON-NLS-1$
			parent.appendChild(element);
			element.setAttribute("name", resource.getName()); //$NON-NLS-1$
			return;
		}
		if (resource.getType() == IResource.FILE)
			element= document.createElement("file"); //$NON-NLS-1$
		else if (resource.getType() == IResource.FOLDER)
			element= document.createElement("folder"); //$NON-NLS-1$
		parent.appendChild(element);
		element.setAttribute("path", resource.getFullPath().toString()); //$NON-NLS-1$
	}
	
	private void add(IJavaElement javaElement, Element parent, Document document) {
		Element element= document.createElement("javaElement"); //$NON-NLS-1$
		parent.appendChild(element);
		element.setAttribute("handleIdentifier", javaElement.getHandleIdentifier()); //$NON-NLS-1$
	}

	private void add(IPackageFragment[] packages, Element parent, Document document) {
		for (int i= 0; i < packages.length; i++) {
			Element pkg= document.createElement("package"); //$NON-NLS-1$
			parent.appendChild(pkg);
			pkg.setAttribute("handleIdentifier", packages[i].getHandleIdentifier()); //$NON-NLS-1$
		}
	}

	/*
	 * This writer always returns OK
	 */
	public IStatus getStatus() {
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null); //$NON-NLS-1$
	}
}
