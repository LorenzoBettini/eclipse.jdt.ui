/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.generics;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.SingletonTypeSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeUniverseSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CastVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.IndependentTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeEquivalenceSet;


public class InferTypeArgumentsConstraintsSolver {

	private final static String CHOSEN_TYPE= "chosenType"; //$NON-NLS-1$
	
	private final InferTypeArgumentsTCModel fTCModel;
	
	/**
	 * The work-list used by the type constraint solver to hold the set of
	 * nodes in the constraint graph that remain to be (re-)processed. Entries
	 * are <code>ConstraintVariable2</code>s.
	 */
	private LinkedList/*<ConstraintVariable2>*/ fWorkList;
	
	private HashMap/*<ICompilationUnit, List<ConstraintVariable2>>*/ fDeclarationsToUpdate;
	private HashMap/*<ICompilationUnit, List<CastVariable2>>*/ fCastsToRemove;

	private ElementStructureEnvironment fElemStructureEnv;
	
	public InferTypeArgumentsConstraintsSolver(InferTypeArgumentsTCModel typeConstraintFactory) {
		fTCModel= typeConstraintFactory;
		fWorkList= new LinkedList();
	}
	
	public void solveConstraints() {
		ConstraintVariable2[] allConstraintVariables= fTCModel.getAllConstraintVariables();
		ParametricStructureComputer parametricStructureComputer= new ParametricStructureComputer(allConstraintVariables, fTCModel);
		Collection/*<CollectionElementVariable2>*/ newVars= parametricStructureComputer.createElemConstraintVariables();
		fElemStructureEnv= parametricStructureComputer.getElemStructureEnv();
		
		ArrayList newAllConstraintVariables= new ArrayList();
		newAllConstraintVariables.addAll(Arrays.asList(allConstraintVariables));
		newAllConstraintVariables.addAll(newVars);
		allConstraintVariables= (ConstraintVariable2[]) newAllConstraintVariables.toArray(new ConstraintVariable2[newAllConstraintVariables.size()]);
		
		
		//loop over all TypeEquivalenceSets and unify the elements from the fElemStructureEnv with the existing TypeEquivalenceSets
		HashSet allTypeEquivalenceSets= new HashSet();
		for (int i= 0; i < allConstraintVariables.length; i++) {
			TypeEquivalenceSet typeEquivalenceSet= allConstraintVariables[i].getTypeEquivalenceSet();
			if (typeEquivalenceSet != null)
				allTypeEquivalenceSets.add(typeEquivalenceSet);
		}
		for (Iterator iter= allTypeEquivalenceSets.iterator(); iter.hasNext();) {
			TypeEquivalenceSet typeEquivalenceSet= (TypeEquivalenceSet) iter.next();
			ConstraintVariable2[] contributingVariables= typeEquivalenceSet.getContributingVariables();
			for (int i= 0; i < contributingVariables.length; i++) {
				for (int j= i + 1; j < contributingVariables.length; j++) {
					ConstraintVariable2 first= contributingVariables[i];
					ConstraintVariable2 second= contributingVariables[j];
					fTCModel.createElementEqualsConstraints(first, second); // recursively
				}
			}
		}
		ITypeConstraint2[] allTypeConstraints= fTCModel.getAllTypeConstraints();
		for (int i= 0; i < allTypeConstraints.length; i++) {
			ITypeConstraint2 typeConstraint= allTypeConstraints[i];
			fTCModel.createElementEqualsConstraints(typeConstraint.getLeft(), typeConstraint.getRight());
		}
		
		initializeTypeEstimates(allConstraintVariables);
		fWorkList.addAll(Arrays.asList(allConstraintVariables));
		runSolver();
		chooseTypes(allConstraintVariables);
		findCastsToRemove(fTCModel.getCastVariables());
		// TODO: clear caches?
	}

	private void initializeTypeEstimates(ConstraintVariable2[] allConstraintVariables) {
		TypeSet.initialize(fTCModel.getJavaLangObject());
		for (int i= 0; i < allConstraintVariables.length; i++) {
			ConstraintVariable2 cv= allConstraintVariables[i];
			//TODO: not necessary for types that are not used in a TypeConstraint but only as type in CollectionElementVariable
			//TODO: handle nested element variables; see ParametricStructureComputer.createAndInitVars()
			TypeEquivalenceSet set= cv.getTypeEquivalenceSet();
			if (set == null) {
				set= new TypeEquivalenceSet(cv);
				set.setTypeEstimate(createInitialEstimate(cv));
				cv.setTypeEquivalenceSet(set);
			} else {
				TypeSet typeEstimate= (TypeSet) cv.getTypeEstimate();
				if (typeEstimate == null) {
					ConstraintVariable2[] cvs= set.getContributingVariables();
					typeEstimate= TypeUniverseSet.create();
					for (int j= 0; j < cvs.length; j++) //TODO: optimize: just try to find an immutable CV; if not found, use Universe
						typeEstimate= typeEstimate.intersectedWith(createInitialEstimate(cvs[j]));
					set.setTypeEstimate(typeEstimate);
				}
			}
		}
	}

	private TypeSet createInitialEstimate(ConstraintVariable2 cv) {
		// TODO: check assumption: only immutable CVs have a type
//		ParametricStructure parametricStructure= fElemStructureEnv.elemStructure(cv);
//		if (parametricStructure != null && parametricStructure != ParametricStructureComputer.ParametricStructure.NONE) {
//			return SubTypesOfSingleton.create(parametricStructure.getBase());
//		}
		
		TType type= cv.getType();
		if (type == null) {
			return TypeUniverseSet.create();
			
		} else if (cv instanceof IndependentTypeVariable2) {
			return TypeUniverseSet.create();
			//TODO: solve problem with recursive bounds
//			TypeVariable tv= (TypeVariable) type;
//			TType[] bounds= tv.getBounds();
//			TypeSet result= SubTypesOfSingleton.create(bounds[0].getErasure());
//			for (int i= 1; i < bounds.length; i++) {
//				result= result.intersectedWith(SubTypesOfSingleton.create(bounds[i].getErasure()));
//			}
//			return result;
			
		} else {
			return new SingletonTypeSet(type);
		}
	}

	private void runSolver() {
		while (! fWorkList.isEmpty()) {
			// Get a variable whose type estimate has changed
			ConstraintVariable2 cv= (ConstraintVariable2) fWorkList.removeFirst();
			List/*<ITypeConstraint2>*/ usedIn= fTCModel.getUsedIn(cv);
			processConstraints(usedIn, cv);
		}
	}
	
	/**
	 * Given a list of <code>ITypeConstraint2</code>s that all refer to a
	 * given <code>ConstraintVariable2</code> (whose type bound has presumably
	 * just changed), process each <code>ITypeConstraint</code>, propagating
	 * the type bound across the constraint as needed.
	 * 
	 * @param usedIn the <code>List</code> of <code>ITypeConstraint2</code>s
	 * to process
	 * @param changedCv the constraint variable whose type bound has changed
	 */
	private void processConstraints(List/*<ITypeConstraint2>*/ usedIn, ConstraintVariable2 changedCv) {
		int i= 0;
		for (Iterator iter= usedIn.iterator(); iter.hasNext(); i++) {
			ITypeConstraint2 tc= (ITypeConstraint2) iter.next();

				maintainSimpleConstraint(changedCv, tc);
				//TODO: prune tcs which cannot cause further changes
				// Maybe these should be pruned after a special first loop over all ConstraintVariables,
				// Since this can only happen once for every CV in the work list.
//				if (isConstantConstraint(stc))
//					fTypeConstraintFactory.removeUsedIn(stc, changedCv);
		}
	}
	
	private void maintainSimpleConstraint(ConstraintVariable2 changedCv, ITypeConstraint2 stc) {
		ConstraintVariable2 left= stc.getLeft();
		ConstraintVariable2 right= stc.getRight();

		TypeEquivalenceSet leftSet= left.getTypeEquivalenceSet();
		TypeEquivalenceSet rightSet= right.getTypeEquivalenceSet();
		TypeSet leftEstimate= (TypeSet) leftSet.getTypeEstimate();
		TypeSet rightEstimate= (TypeSet) rightSet.getTypeEstimate();
			
		if (leftEstimate.isUniverse() && rightEstimate.isUniverse())
			return; // nothing to do

		if (leftEstimate.equals(rightEstimate))
			return; // nothing to do

		TypeSet lhsSuperTypes= leftEstimate.superTypes();
		TypeSet rhsSubTypes= rightEstimate.subTypes();

		if (! rhsSubTypes.containsAll(leftEstimate)) {
			TypeSet xsection= leftEstimate.intersectedWith(rhsSubTypes);

			if (xsection.isEmpty())
				throw new IllegalStateException("Type estimate set is now empty for LHS in " + left + " <= " + right + "; estimates were " + leftEstimate + " <= " + rightEstimate); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			leftSet.setTypeEstimate(xsection);
			fWorkList.addAll(Arrays.asList(leftSet.getContributingVariables()));
		}
		if (! lhsSuperTypes.containsAll(rightEstimate)) {
			TypeSet xsection= rightEstimate.intersectedWith(lhsSuperTypes);

			if (xsection.isEmpty())
				throw new IllegalStateException("Type estimate set is now empty for RHS in " + left + " <= " + right + "; estimates were " + leftEstimate + " <= " + rightEstimate); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			rightSet.setTypeEstimate(xsection);
			fWorkList.addAll(Arrays.asList(rightSet.getContributingVariables()));
		}
	}

	private void chooseTypes(ConstraintVariable2[] allConstraintVariables) {
		fDeclarationsToUpdate= new HashMap();
		for (int i= 0; i < allConstraintVariables.length; i++) {
			ConstraintVariable2 cv= allConstraintVariables[i];
				
			TypeEquivalenceSet set= cv.getTypeEquivalenceSet();
			if (set == null)
				continue; //TODO: should not happen iff all unused constraint variables got pruned
			//TODO: should calculate only once per EquivalenceRepresentative; can throw away estimate TypeSet afterwards
			TType type= cv.getTypeEstimate().chooseSingleType(); //TODO: is null for Universe TypeSet
			setChosenType(cv, type);
			
			if (cv instanceof CollectionElementVariable2) {
				CollectionElementVariable2 elementCv= (CollectionElementVariable2) cv;
				ICompilationUnit cu= elementCv.getCompilationUnit();
				if (cu != null) //TODO: shouldn't be the case
					addToMultiMap(fDeclarationsToUpdate, cu, cv);
				else {
					int TODO= 1;
				}
			}
		}
	}

	private void findCastsToRemove(CastVariable2[] castVariables) {
		fCastsToRemove= new HashMap();
		for (int i= 0; i < castVariables.length; i++) {
			CastVariable2 castCv= castVariables[i];
			ConstraintVariable2 expressionVariable= castCv.getExpressionVariable();
			TType chosenType= InferTypeArgumentsConstraintsSolver.getChosenType(expressionVariable);
			if (chosenType != null && chosenType.canAssignTo(castCv.getType())) {
				ICompilationUnit cu= castCv.getCompilationUnit();
				addToMultiMap(fCastsToRemove, cu, castCv);
			}
		}
	}

	private void addToMultiMap(HashMap map, ICompilationUnit cu, ConstraintVariable2 cv) {
		ArrayList cvs= (ArrayList) map.get(cu);
		if (cvs != null) {
			cvs.add(cv);
		} else {
			cvs= new ArrayList(1);
			cvs.add(cv);
			map.put(cu, cvs);
		}
	}

	public HashMap/*<ICompilationUnit, List<ConstraintVariable2>>*/ getDeclarationsToUpdate() {
		return fDeclarationsToUpdate;
	}
	
	public HashMap/*<ICompilationUnit, List<CastVariable2>>*/ getCastsToRemove() {
		return fCastsToRemove;
	}
	
	public static TType getChosenType(ConstraintVariable2 cv) {
		TType type= (TType) cv.getData(CHOSEN_TYPE);
		if (type != null)
			return type;
		TypeEquivalenceSet set= cv.getTypeEquivalenceSet();
		if (set == null) { //TODO: should not have to set this here. Clean up when caching chosen type
			// no representative == no restriction
			set= new TypeEquivalenceSet(cv);
			set.setTypeEstimate(TypeUniverseSet.create());
			cv.setTypeEquivalenceSet(set);
		}
		return cv.getTypeEstimate().chooseSingleType();
	}

	private static void setChosenType(ConstraintVariable2 cv, TType type) {
		cv.setData(CHOSEN_TYPE, type);
	}
}
