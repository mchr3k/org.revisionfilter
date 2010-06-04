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
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.PackageStats;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.filter.Filter;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 * 
 * @author William Pugh
 */

public class SetBugDatabaseInfo {

	/**
	 * 
	 */
	private static final String USAGE = "Usage: <cmd> " + " [options] [<oldData> [<newData>]]";

	static class SetInfoCommandLine extends CommandLine {
		String revisionName;
		String projectName;

		String exclusionFilterFile;
		String lastVersion;
		
		String cloudId;
		HashMap<String,String> cloudProperties = new HashMap<String,String>();

		boolean withMessages = false;
		boolean purgeStats = false;
		boolean purgeClassStats = false;
		boolean purgeMissingClasses = false;
		
		boolean resetSource = false;
		boolean resetProject = false;
		boolean purgeDesignations = false;

		long revisionTimestamp = 0L;

		public List<String> sourcePaths = new LinkedList<String>();

		public List<String> searchSourcePaths = new LinkedList<String>();

		SetInfoCommandLine() {
			addOption("-name", "name", "set name for (last) revision");
			addOption("-projectName", "name", "set name for project");
			addOption("-timestamp", "when", "set timestamp for (last) revision");
			addSwitch("-resetSource", "remove all source search paths");
			addSwitch("-resetProject", "remove all source search paths, analysis and auxilary classpath entries");
			addOption("-source", "directory", "Add this directory to the source search path");
			addSwitch("-purgeStats", "purge/delete information about sizes of analyzed class files");
			addSwitch("-uploadDesignations", "upload all designations to cloud");
			addSwitch("-purgeDesignations", "purge/delete user designations (e.g., MUST_FIX or NOT_A_BUG");
			addSwitch("-purgeClassStats", "purge/delete information about sizes of analyzed class files, but retain class stats");
			addSwitch("-purgeMissingClasses", "purge list of missing classes");
			addOption("-findSource", "directory", "Find and add all relevant source directions contained within this directory");
			addOption("-suppress", "filter file", "Suppress warnings matched by this file (replaces previous suppressions)");
			addOption("-lastVersion", "version", "Trim the history to just include just the specified version");
			addSwitch("-withMessages", "Add bug descriptions");
			addOption("-cloud", "id", "set cloud id");
			addOption("-cloudProperty", "key=value", "set cloud property");

		}

		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			if (option.equals("-withMessages"))
				withMessages = true;
			else if (option.equals("-resetSource"))
				resetSource = true;
			else if (option.equals("-resetProject"))
				resetProject = true;
			else if (option.equals("-purgeStats"))
				purgeStats = true;
			else if (option.equals("-purgeDesignations"))
				purgeDesignations = true;
			else if (option.equals("-purgeClassStats"))
				purgeClassStats = true;
			else if (option.equals("-purgeMissingClasses"))
				purgeMissingClasses = true;
			else
				throw new IllegalArgumentException("no option " + option);

		}

		@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			if (option.equals("-name"))
				revisionName = argument;
			else if (option.equals("-cloud")) 
				cloudId = argument;
			else if (option.equals("-cloudProperty")) {
				int e = argument.indexOf('=');
				if (e == -1)
					throw new IllegalArgumentException("Bad cloud property: " + argument);
				String key = argument.substring(0, e);
				String value = argument.substring(e+1);
				cloudProperties.put(key, value);
				
			}
			else if (option.equals("-projectName"))
				projectName = argument;
			else if (option.equals("-suppress"))
				exclusionFilterFile = argument;
			else if (option.equals("-timestamp"))
				revisionTimestamp = Date.parse(argument);

			else if (option.equals("-source"))
				sourcePaths.add(argument);
			else if (option.equals("-lastVersion")) {
						
			}
			else if (option.equals("-findSource"))
				searchSourcePaths.add(argument);
			else
				throw new IllegalArgumentException("Can't handle option " + option);

		}

	}

	public static void main(String[] args) throws IOException,
			DocumentException {
		FindBugs.setNoAnalysis();
		DetectorFactoryCollection.instance();
		SetInfoCommandLine commandLine = new SetInfoCommandLine();
		int argCount = commandLine.parse(args, 0, 2, USAGE);

		SortedBugCollection origCollection = new SortedBugCollection();

		if (argCount < args.length) 
			origCollection.readXML(args[argCount++]);
		else
			origCollection.readXML(System.in);
		Project project = origCollection.getProject();

		if (commandLine.revisionName != null)
			origCollection.setReleaseName(commandLine.revisionName);
		if (commandLine.projectName != null)
			origCollection.getProject().setProjectName(commandLine.projectName);
		if (commandLine.revisionTimestamp != 0)
			origCollection.setTimestamp(commandLine.revisionTimestamp);
		origCollection.setWithMessages(commandLine.withMessages);

		if (commandLine.purgeDesignations) 
			for(BugInstance b : origCollection) {
				b.setUserDesignation(null);
			}
		if (commandLine.exclusionFilterFile != null) {
			project.setSuppressionFilter(Filter.parseFilter(commandLine.exclusionFilterFile));
		}
		if (commandLine.resetProject) {
			project.getSourceDirList().clear();
			project.getFileList().clear();
			project.getAuxClasspathEntryList().clear();
		}
		if (commandLine.cloudId != null)
			project.setCloudId(commandLine.cloudId);
		for(Map.Entry<String,String> e : commandLine.cloudProperties.entrySet()) {
			project.getCloudProperties().setProperty(e.getKey(), e.getValue());
		}
		if (commandLine.resetSource)
			project.getSourceDirList().clear();
		for(String source : commandLine.sourcePaths)
			project.addSourceDir(source);
		if (commandLine.purgeStats)
			origCollection.getProjectStats().getPackageStats().clear();
		if (commandLine.purgeClassStats)
			for(PackageStats ps : origCollection.getProjectStats().getPackageStats()) {
				ps.getClassStats().clear();
			}
		if (commandLine.purgeMissingClasses)
			origCollection.clearMissingClasses();
		if (commandLine.lastVersion != null) {
			Map<String, AppVersion> versions = new HashMap<String, AppVersion>();
			SortedMap<Long, AppVersion> timeStamps = new TreeMap<Long, AppVersion>();

			for(Iterator<AppVersion> i = origCollection.appVersionIterator(); i.hasNext(); ) {
				AppVersion v = i.next();
				versions.put(v.getReleaseName(), v);
				timeStamps.put(v.getTimestamp(), v);
			}
			// add current version to the maps
			AppVersion v = origCollection.getCurrentAppVersion();
			versions.put(v.getReleaseName(), v);
			timeStamps.put(v.getTimestamp(), v);

			long last = edu.umd.cs.findbugs.workflow.Filter.FilterCommandLine.getVersionNum(
					versions, timeStamps, commandLine.lastVersion, true, v.getSequenceNumber());
			if (last < origCollection.getSequenceNumber()) {
				String name = origCollection.getAppVersionFromSequenceNumber(last).getReleaseName();
				long timestamp = origCollection.getAppVersionFromSequenceNumber(last).getTimestamp();
				origCollection.setReleaseName(name);
				origCollection.setTimestamp(timestamp);
				origCollection.trimAppVersions(last);
			}

		}

		Map<String,Set<String>> missingFiles = new HashMap<String,Set<String>>();
		if (!commandLine.searchSourcePaths.isEmpty()) {
			sourceSearcher = new SourceSearcher(project);
			for(BugInstance bug : origCollection.getCollection()) {
				SourceLineAnnotation src = bug.getPrimarySourceLineAnnotation();
				if (!sourceSearcher.sourceNotFound.contains(src.getClassName())
						&& !sourceSearcher.findSource(src)) {
					Set<String> paths = missingFiles.get(src.getSourceFile());
					if (paths == null) {
						paths = new HashSet<String>();
						missingFiles.put(src.getSourceFile(), paths);
					}
					String fullPath = fullPath(src);
					// System.out.println("Missing " + fullPath);
					paths.add(fullPath);
				}
			}
			Set<String> foundPaths = new HashSet<String>();
			for(String f : commandLine.searchSourcePaths) 
				for(File javaFile :  RecursiveSearchForJavaFiles.search(new File(f))) {
					Set<String> matchingMissingClasses = missingFiles.get(javaFile.getName());
					if (matchingMissingClasses == null) {
						// System.out.println("Nothing for " + javaFile);
					} else for(String sourcePath : matchingMissingClasses) {
						String path = javaFile.getAbsolutePath();
						if (path.endsWith(sourcePath)) {
							String dir = path.substring(0,path.length() - sourcePath.length());
							foundPaths.add(dir);
							
						}
					}

				}

			Set<String> toRemove = new HashSet<String>();
			for(String p1 : foundPaths)
				for(String p2 : foundPaths)
					if (!p1.equals(p2) && p1.startsWith(p2)) {
						toRemove.add(p1);
						break;
					}
			foundPaths.removeAll(toRemove);
			
			for(String dir : foundPaths)  {
				project.addSourceDir(dir);
				if (argCount < args.length)
					System.out.println("Found " + dir);
				}

		}
			// OK, now we know all the missing source files
			// we also know all the .java files in the directories we were pointed to


		if (argCount < args.length) 
			origCollection.writeXML(args[argCount++]);
		else
			origCollection.writeXML(System.out);

	}

	static String fullPath(SourceLineAnnotation src) {
		return src.getPackageName().replace('.', File.separatorChar) + File.separatorChar + src.getSourceFile();
	}

	static SourceSearcher sourceSearcher;

}