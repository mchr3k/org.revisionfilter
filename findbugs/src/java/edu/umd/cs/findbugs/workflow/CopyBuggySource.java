/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 William Pugh
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
package edu.umd.cs.findbugs.workflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugAnnotationWithSourceLines;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.SourceFinder;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 * 
 * @author William Pugh
 */

public class CopyBuggySource {

	/**
	 * 
	 */
	private static final String USAGE = "Usage: <cmd> "
			+ "  <bugs.xml> <destinationSrcDir>";

	public static void main(String[] args) throws IOException,
			DocumentException {
		FindBugs.setNoAnalysis();
		DetectorFactoryCollection.instance();
		if (args.length != 2) {
			System.out.println(USAGE);
			return;
		}

		BugCollection origCollection;
		origCollection = new SortedBugCollection();
		origCollection.readXML(args[0]);
		File src = new File(args[1]);
		byte buf[] = new byte[4096];
		if (!src.isDirectory())
			throw new IllegalArgumentException(args[1]
					+ " is not a source directory");
		Project project = origCollection.getProject();
		SourceFinder sourceFinder = new SourceFinder(project);
		HashSet<String> copied = new HashSet<String>();
		HashSet<String> couldNotCreate = new HashSet<String>();
		for (BugInstance bug : origCollection.getCollection()) {
			for (Iterator<BugAnnotation> i = bug.annotationIterator(); i
					.hasNext();) {
				BugAnnotation ann = i.next();
				SourceLineAnnotation sourceAnnotation;
				if (ann instanceof BugAnnotationWithSourceLines)
					sourceAnnotation = ((BugAnnotationWithSourceLines) ann)
							.getSourceLines();
				else if (ann instanceof SourceLineAnnotation)
					sourceAnnotation = (SourceLineAnnotation) ann;
				else
					continue;
				if (sourceAnnotation == null)
					continue;
				if (sourceAnnotation.isUnknown())
					continue;
				String fullName;

				String packageName = sourceAnnotation.getPackageName();
				String sourceFile = sourceAnnotation.getSourceFile();
				if (packageName == "")
					fullName = sourceFile;
				else
					fullName = packageName.replace('.', File.separatorChar)
							+ File.separatorChar + sourceFile;
				if (copied.add(fullName)) {
					File file = new File(src, fullName);
					if (file.exists()) {
						System.out.println(file + " already exists");
						continue;
					}
					File parent = file.getParentFile();
					InputStream in = null;
					OutputStream out = null;
					try {
						in = sourceFinder.openSource(packageName, sourceFile);
						if (!parent.mkdirs() && !parent.isDirectory()) {
							String path = parent.getPath();
							if (couldNotCreate.add(path))
							System.out.println("Can't to create directory for " 
									+ path);
							in.close();
							continue;
						}
						out = new FileOutputStream(file);
						while (true) {
							int sz = in.read(buf);
							if (sz < 0)
								break;
							out.write(buf, 0, sz);
						}
						System.out.println("Copied " + file);
					} catch (FileNotFoundException e) {
						System.out.println("Did not find " + file);
					} catch (IOException e) {	
						System.out.println("Problem copying " + file);
						e.printStackTrace(System.out);
					} finally {
						close(in);
						close(out);
					}
					
				}
			}
		}
	}

	public static void close(InputStream in) {
		try {
			if (in != null)
				in.close();
		} catch (IOException e) {
		}

	}

	public static void close(OutputStream out) {
		try {
			if (out != null)
				out.close();
		} catch (IOException e) {
		}

	}
}
