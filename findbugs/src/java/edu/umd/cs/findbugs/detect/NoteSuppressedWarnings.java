/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.ClassWarningSuppressor;
import edu.umd.cs.findbugs.DelegatingBugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.FieldWarningSuppressor;
import edu.umd.cs.findbugs.FilterBugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.MethodWarningSuppressor;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.PackageWarningSuppressor;
import edu.umd.cs.findbugs.ParameterWarningSuppressor;
import edu.umd.cs.findbugs.SuppressionMatcher;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

public class NoteSuppressedWarnings
		extends AnnotationVisitor
		implements Detector, NonReportingDetector {

	private  Set<String> packages = new HashSet<String>();

	private SuppressionMatcher suppressionMatcher;

	private NoteSuppressedWarnings recursiveDetector;

	public NoteSuppressedWarnings(BugReporter bugReporter) {
		this(bugReporter, false);
	}

	public NoteSuppressedWarnings(BugReporter bugReporter, boolean recursive) {
		if (!recursive) {
			DelegatingBugReporter b = (DelegatingBugReporter) bugReporter;
			BugReporter origBugReporter = b.getDelegate();
			suppressionMatcher = new SuppressionMatcher();
			BugReporter filterBugReporter = new FilterBugReporter(origBugReporter,
					suppressionMatcher, false);
			b.setDelegate(filterBugReporter);
			recursiveDetector = new NoteSuppressedWarnings(bugReporter, true);
			recursiveDetector.suppressionMatcher = suppressionMatcher;
		}
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		if  (!BCELUtil.preTiger(javaClass)) javaClass.accept(this);
	}

	@Override
	public void visit(JavaClass obj) {
		if (recursiveDetector == null)
			return;
		try {
			if (getClassName().endsWith("package-info"))
				return;
			String packageName = getPackageName().replace('/', '.');
			if (!packages.add(packageName))
				return;
			String packageInfo = "package-info";
			if (packageName.length() > 0)
				packageInfo = packageName + "." + packageInfo;

			JavaClass packageInfoClass = Repository.lookupClass(packageInfo);
			recursiveDetector.visitJavaClass(packageInfoClass);
		} catch (ClassNotFoundException e) {
			// ignore
		}
	}

	@Override
	public void visitAnnotation(String annotationClass, Map<String, Object> map,
			boolean runtimeVisible) {
		if (!annotationClass.endsWith("SuppressWarnings"))
			return;
		Object value = map.get("value");
		if (value == null || !(value instanceof Object[])) {
			suppressWarning(null);
			return;
		}
		Object[] suppressedWarnings = (Object[]) value;
		if (suppressedWarnings.length == 0)
			suppressWarning(null);
		else
			for (Object suppressedWarning : suppressedWarnings)
				if (suppressedWarning instanceof String)
				suppressWarning((String) suppressedWarning);
	}

	@Override
	public void visitParameterAnnotation(int p, String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {
		if (!annotationClass.endsWith("SuppressWarnings"))
			return;
		if (!getMethod().isStatic()) p++;
		Object value = map.get("value");
		if (value == null || !(value instanceof Object[])) {
			suppressWarning(p, null);
			return;
		}
		Object[] suppressedWarnings = (Object[]) value;
		if (suppressedWarnings.length == 0)
			suppressWarning(p, null);
		else
			for (Object suppressedWarning : suppressedWarnings)
				if (suppressedWarning instanceof String)
				suppressWarning(p, (String) suppressedWarning);
	}
	
	private void suppressWarning(int parameter, String pattern) {
		String className = getDottedClassName();
		ClassAnnotation clazz = new ClassAnnotation(className);
		suppressionMatcher.addSuppressor(new ParameterWarningSuppressor(pattern, clazz,
					MethodAnnotation.fromVisitedMethod(this), parameter));
		
	}

	private void suppressWarning(String pattern) {
		String className = getDottedClassName();
		ClassAnnotation clazz = new ClassAnnotation(className);
		if (className.endsWith("package-info") && recursiveDetector == null)
			suppressionMatcher.addPackageSuppressor(new PackageWarningSuppressor(pattern,
					getPackageName().replace('/', '.')));
		else if (visitingMethod())
			suppressionMatcher.addSuppressor(new MethodWarningSuppressor(pattern, clazz,
					MethodAnnotation.fromVisitedMethod(this)));
		else if (visitingField())
			suppressionMatcher.addSuppressor(new FieldWarningSuppressor(pattern, clazz,
					FieldAnnotation.fromVisitedField(this)));
		else
			suppressionMatcher.addSuppressor(new ClassWarningSuppressor(pattern, clazz));
	}

	public void report() {

	}

}
