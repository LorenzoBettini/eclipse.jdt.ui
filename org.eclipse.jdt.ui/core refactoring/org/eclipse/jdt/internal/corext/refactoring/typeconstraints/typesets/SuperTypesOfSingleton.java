/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Robert M. Fuhrer (rfuhrer@watson.ibm.com), IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.ArrayType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TTypes;

public class SuperTypesOfSingleton extends TypeSet {
	/**
	 * The "base type" defining the lower bound of this set.
	 */
	private TType fLowerBound;

	private static final Map/*<IType arg>*/ sCommonExprs= new LinkedHashMap();//@perf
	public static void clear() {
		sCommonExprs.clear();
	}

	public static SuperTypesOfSingleton create(TType subType) {
		if (sCommonExprs.containsKey(subType)) {
			sCommonExprHits++;
			return (SuperTypesOfSingleton) sCommonExprs.get(subType);
		} else {
			SuperTypesOfSingleton s= new SuperTypesOfSingleton(subType);

			sCommonExprMisses++;
			sCommonExprs.put(subType, s);
			return s;
		}
	}

	private SuperTypesOfSingleton(TType t) {
		super();
		fLowerBound= t;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isUniverse()
	 */
	public boolean isUniverse() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#makeClone()
	 */
	public TypeSet makeClone() {
		return new SuperTypesOfSingleton(fLowerBound);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#intersectedWith(org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	protected TypeSet specialCasesIntersectedWith(TypeSet other) {
		if (other.isSingleton() && other.anyMember().equals(fLowerBound))
			return other;		// xsect(superTypes(A),A) = A

		if (other instanceof SuperTypesOfSingleton) {
			SuperTypesOfSingleton otherSuper= (SuperTypesOfSingleton) other;

			if (otherSuper.fLowerBound.canAssignTo(fLowerBound))
				return this;
			if (fLowerBound.canAssignTo(otherSuper.fLowerBound))
				return otherSuper;
		} else if (other.hasUniqueUpperBound()) {
			TType otherUpper= other.uniqueUpperBound();

			if (otherUpper.equals(fLowerBound))
				return new SingletonTypeSet(fLowerBound);
			if ((otherUpper != fLowerBound && otherUpper.canAssignTo(fLowerBound)) ||
				! fLowerBound.canAssignTo(otherUpper))
				return EmptyTypeSet.create();
		}
//		else if (other instanceof SuperTypesSet) {
//			SuperTypesSet otherSub= (SuperTypesSet) other;
//			TypeSet otherLowers= otherSub.lowerBound();
//
//			for(Iterator iter= otherLowers.iterator(); iter.hasNext(); ) {
//				IType t= (IType) iter.next();
//
//				if ()
//			}
//		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isEmpty()
	 */
	public boolean isEmpty() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#upperBound()
	 */
	public TypeSet upperBound() {
		return new SingletonTypeSet(sJavaLangObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#lowerBound()
	 */
	public TypeSet lowerBound() {
		return new SingletonTypeSet(fLowerBound);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#hasUniqueLowerBound()
	 */
	public boolean hasUniqueLowerBound() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#hasUniqueUpperBound()
	 */
	public boolean hasUniqueUpperBound() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#uniqueLowerBound()
	 */
	public TType uniqueLowerBound() {
		return fLowerBound;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#uniqueUpperBound()
	 */
	public TType uniqueUpperBound() {
		return sJavaLangObject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#superTypes()
	 */
	public TypeSet superTypes() {
		return this; // makeClone();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#contains(org.eclipse.jdt.core.IType)
	 */
	public boolean contains(TType t) {
		if (t.equals(fLowerBound))
			return true;
		if (t.equals(sJavaLangObject))
			return true;
		return fLowerBound.canAssignTo(t);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#containsAll(org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public boolean containsAll(TypeSet other) {
		// Optimization: if other is also a SubTypeOfSingleton, just compare bounds
		if (other instanceof SuperTypesOfSingleton) {
			SuperTypesOfSingleton otherSuper= (SuperTypesOfSingleton) other;
			return fLowerBound.canAssignTo(otherSuper.fLowerBound);
		}
		// Optimization: if other is a SubTypesSet, just compare all its bounds to mine
		if (other instanceof SuperTypesSet) {
			SuperTypesSet otherSuper= (SuperTypesSet) other;
			TypeSet otherLowerBounds= otherSuper.lowerBound();

			for(Iterator iter= otherLowerBounds.iterator(); iter.hasNext(); ) {
				TType t= (TType) iter.next();
				if (!fLowerBound.canAssignTo(t))
					return false;
			}
			return true;
		}
		// For now, no more tricks up my sleeve; get an iterator
		for(Iterator iter= other.iterator(); iter.hasNext(); ) {
			TType t= (TType) iter.next();

			if (!fLowerBound.canAssignTo(t))
				return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#iterator()
	 */
	public Iterator iterator() {
		return enumerate().iterator();
//		return new Iterator() {
//			// First type returned is fLowerBound, then each of the supertypes, in turn
//			//
//			// If the lower bound is an array type, return the set of array types
//			// { Array(superType(elementTypeOf(fUpperBound))) }
//			boolean isArray= (fLowerBound instanceof ArrayIType);
//			private Set/*<IType>*/ superTypes= sTypeHierarchy.getAllSupertypes(getElementTypeOf(fLowerBound));
//			private Iterator/*<IType>*/ superTypeIter= superTypes.iterator();
//			private int nDims= getDimsOf(fLowerBound);
//			private int idx= (isArray ? -2 : -1);
//			public void remove() { /*do nothing*/ }
//			public boolean hasNext() { return idx < superTypes.size(); }
//			public Object next() {
//				int i=idx++;
//				if (i < -1) return sJavaLangObject;
//				if (i < 0) return fLowerBound;
//				return makePossiblyArrayTypeFor((IType) superTypeIter.next(), nDims);
//			}
//		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isSingleton()
	 */
	public boolean isSingleton() {
		// The only thing that doesn't have at least 1 proper supertype is java.lang.Object
		return fLowerBound.equals(sJavaLangObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#anyMember()
	 */
	public TType anyMember() {
		return fLowerBound;
	}

	private EnumeratedTypeSet fEnumCache= null;

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#enumerate()
	 */
	public EnumeratedTypeSet enumerate() {
		if (fEnumCache == null) {
			if (fLowerBound instanceof ArrayType) {
				ArrayType at= (ArrayType) fLowerBound;
				fEnumCache= EnumeratedTypeSet.makeArrayTypesForElements(TTypes.getAllSuperTypesIterator(at.getComponentType()));
				fEnumCache.add(sJavaLangObject);
			} else
				fEnumCache= new EnumeratedTypeSet(TTypes.getAllSuperTypesIterator(fLowerBound));

			fEnumCache.add(fLowerBound);
			fEnumCache.initComplete();
		}
		return fEnumCache;
	}

	public boolean equals(Object o) {
		if (!(o instanceof SuperTypesOfSingleton))
			return false;
		SuperTypesOfSingleton other= (SuperTypesOfSingleton) o;

		return other.fLowerBound.equals(fLowerBound);
	}

	public String toString() {
		return "<" + fID + ": superTypes(" + fLowerBound.getPrettySignature() + ")>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
