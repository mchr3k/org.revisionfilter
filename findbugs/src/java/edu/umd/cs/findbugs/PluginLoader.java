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

package edu.umd.cs.findbugs;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.CloudFactory;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.username.NameLookup;
import edu.umd.cs.findbugs.plan.ByInterfaceDetectorFactorySelector;
import edu.umd.cs.findbugs.plan.DetectorFactorySelector;
import edu.umd.cs.findbugs.plan.DetectorOrderingConstraint;
import edu.umd.cs.findbugs.plan.ReportingDetectorFactorySelector;
import edu.umd.cs.findbugs.plan.SingleDetectorFactorySelector;

/**
 * Loader for a FindBugs plugin.
 * A plugin is a jar file containing two metadata files,
 * "findbugs.xml" and "messages.xml".  Those files specify
 * <ul>
 * <li> the bug pattern Detector classes,
 * <li> the bug patterns detected (including all text for displaying
 * detected instances of those patterns), and
 * <li> the "bug codes" which group together related bug instances
 * </ul>
 *
 * <p> The PluginLoader creates a Plugin object to store
 * the Detector factories and metadata.</p>
 *
 * @author David Hovemeyer
 * @see Plugin
 * @see PluginException
 */
public class PluginLoader {

	private static final boolean DEBUG = SystemProperties.getBoolean("findbugs.debug.PluginLoader");

	// ClassLoader used to load classes and resources
	private ClassLoader classLoader;
	
	// Keep a count of how many plugins we've seen without a
	// "pluginid" attribute, so we can assign them all unique ids.
	private static int nextUnknownId;

	// The loaded Plugin
	private Plugin plugin;
	
	private final boolean corePlugin;
	
	private final URL loadedFrom;

	/**
	 * Constructor.
	 *
	 * @param url the URL of the plugin Jar file
	 * @throws PluginException if the plugin cannot be fully loaded
	 */
	public PluginLoader(URL url) throws PluginException {
		this.classLoader = new URLClassLoader(new URL[]{url});
		loadedFrom = url;
		corePlugin = false;
		init();
	}

	/**
	 * Constructor.
	 *
	 * @param url    the URL of the plugin Jar file
	 * @param parent the parent classloader
	 */
	public PluginLoader(URL url, ClassLoader parent) throws PluginException {
		this.classLoader = new URLClassLoader(new URL[]{url}, parent);
		loadedFrom = url;
		corePlugin = false;
		init();
	}
	
	/**
	 * Constructor.
	 * Loads a plugin using the caller's class loader.
	 * This constructor should only be used to load the
	 * "core" findbugs detectors, which are built into
	 * findbugs.jar.
	 */
	public PluginLoader() {
		this.classLoader = this.getClass().getClassLoader();
		corePlugin = true;
		loadedFrom = null;
	}
	
	/**
     * @return Returns the classLoader.
     */
    public ClassLoader getClassLoader() {
	    return classLoader;
    }

	/**
	 * Get the Plugin.
	 * @throws PluginException if the plugin cannot be fully loaded
	 */
	public Plugin getPlugin() throws PluginException {
		if (plugin == null)
			init();
		return plugin;
	}
	
	/**
	 * Get a resource using the URLClassLoader classLoader.  We try
	 * findResource first because (based on experiment) we can trust it
	 * to prefer resources in the jarfile to resources on the
	 * filesystem.  Simply calling classLoader.getResource() allows the
	 * filesystem to override the jarfile, which can mess things up if,
	 * for example, there is a findbugs.xml or messages.xml in the
	 * current directory. 
	 * @param name resource to get
	 * @return URL for the resource, or null if it could not be found
	 */
	private URL getResource(String name) {
		URL url = null;

		if (classLoader instanceof URLClassLoader) {
			URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
			url = urlClassLoader.findResource(name);
			if (url == null)
				url = urlClassLoader.findResource("/" + name);
		}

		if (url == null) {
			url = classLoader.getResource(name);
			if (url == null)
				url = classLoader.getResource("/" + name);
		}

		if (url != null)
			return url;		
		return null;
	}

	public static @CheckForNull
	URL getCoreResource(String name) {
		URL u = loadFromFindBugsPluginDir(name);
		if (u != null) 
			return u;
		u = PluginLoader.class.getResource("/"+name);
		if (u != null)
			return u;
		u = PluginLoader.class.getResource(name);
		if (u != null)
			return u;
		return loadFromFindBugsEtcDir(name);
	}
	
	public static @CheckForNull
	URL loadFromFindBugsEtcDir(String name) {

		String findBugsHome = DetectorFactoryCollection.getFindBugsHome();
		if (findBugsHome != null) {
			File f = new File(new File(new File(findBugsHome), "etc"), name);
			if (f.canRead())
				try {
					return f.toURL();
				} catch (MalformedURLException e) {
					// ignore it
				}
		}

		return null;
    }

	public static @CheckForNull
	URL loadFromFindBugsPluginDir(String name) {

		String findBugsHome = DetectorFactoryCollection.getFindBugsHome();
		if (findBugsHome != null) {
			File f = new File(new File(new File(findBugsHome), "plugin"), name);
			if (f.canRead())
				try {
					return f.toURL();
				} catch (MalformedURLException e) {
					// ignore it
				}
		}

		return null;
    }
	
	private static <T> Class<? extends T> getClass(ClassLoader loader, String className, Class<T> type) throws PluginException {
		try {
	        return  loader.loadClass(className).asSubclass(type);
        } catch (ClassNotFoundException e) {
	        throw new PluginException("Unable to load " + className, e);
        } catch (ClassCastException e) {
        	 throw new PluginException("Cannot cast " + className + " to " + type.getName(), e);
        }
		
	}

	
	@SuppressWarnings("unchecked")
    private void init() throws PluginException {
		// Plugin descriptor (a.k.a, "findbugs.xml").  Defines
		// the bug detectors and bug patterns that the plugin provides.
		Document pluginDescriptor;

		// Unique plugin id
		String pluginId;

		// List of message translation files in decreasing order of precedence
		ArrayList<Document> messageCollectionList = new ArrayList<Document>();

		// Read the plugin descriptor
		String name = "findbugs.xml";
		URL findbugsXML_URL = getResource(name);
		if (findbugsXML_URL == null)
			throw new PluginException("Couldn't find \"" + name + "\" in plugin");
		if(DEBUG) 
			System.out.println("PluginLoader found " + name + " at: " + findbugsXML_URL);
		
		
		if (loadedFrom != null 
				&& !findbugsXML_URL.toString().contains(loadedFrom.toString())) {
			throw new PluginDoesntContainMetadataException("plugin doesn't contain findbugs.xml: " + loadedFrom);
		}
		SAXReader reader = new SAXReader();
		
		try {
			Reader r = new InputStreamReader(findbugsXML_URL.openStream());
			pluginDescriptor = reader.read(r);
		} catch (DocumentException e) {
			throw new PluginException("Couldn't parse \"" + findbugsXML_URL + "\" using " + reader.getClass().getName(), e);
		} catch (IOException e) {
			throw new PluginException("Couldn't open \"" + findbugsXML_URL + "\"", e);
			
        }

		// Get the unique plugin id (or generate one, if none is present)
		pluginId = pluginDescriptor.valueOf("/FindbugsPlugin/@pluginid");
		if (pluginId.equals("")) {
			synchronized (PluginLoader.class) {
				pluginId = "plugin" + nextUnknownId++;
			}
		}

		// See if the plugin is enabled or disabled by default.
		// Note that if there is no "defaultenabled" attribute,
		// then we assume that the plugin IS enabled by default.
		String defaultEnabled = pluginDescriptor.valueOf("/FindbugsPlugin/@defaultenabled");
		boolean pluginEnabled = defaultEnabled.equals("") || Boolean.valueOf(defaultEnabled).booleanValue();

		// Load the message collections
			//Locale locale = Locale.getDefault();
			Locale locale = I18N.defaultLocale;
			String language = locale.getLanguage();
			String country = locale.getCountry();

			try {
			if (country != null)
				addCollection(messageCollectionList, "messages_" + language + "_" + country + ".xml");
			addCollection(messageCollectionList, "messages_" + language + ".xml");
			} catch (PluginException e) {
				AnalysisContext.logError("Error loading localized message file", e);
			}
			addCollection(messageCollectionList, "messages.xml");


		// Create the Plugin object (but don't assign to the plugin field yet,
		// since we're still not sure if everything will load correctly)
		Plugin plugin = new Plugin(pluginId, this);
		plugin.setEnabled(pluginEnabled);

		// Set provider and website, if specified
		String provider = pluginDescriptor.valueOf("/FindbugsPlugin/@provider");
		if (!provider.equals(""))
			plugin.setProvider(provider.trim());
		String website = pluginDescriptor.valueOf("/FindbugsPlugin/@website");
		if (!website.equals(""))
			plugin.setWebsite(website.trim());

		// Set short description, if specified
		Node pluginShortDesc = null;
		try {
			pluginShortDesc = findMessageNode(
					messageCollectionList,
					"/MessageCollection/Plugin/ShortDescription",
					"no plugin description");
		} catch (PluginException e) {
			// Missing description is not fatal, so ignore
		}
		if (pluginShortDesc != null) {
			plugin.setShortDescription(pluginShortDesc.getText().trim());
		}

		List<Node> cloudNodeList = pluginDescriptor.selectNodes("/FindbugsPlugin/Cloud");
		int cloudCount = 0;
		for(Node cloudNode : cloudNodeList) {
			
			String cloudClassname = cloudNode.valueOf("@cloudClass");
			String cloudid = cloudNode.valueOf("@id");
			String usernameClassname =  cloudNode.valueOf("@usernameClass");
			String propertiesLocation = cloudNode.valueOf("@properties");
			Class<? extends Cloud> cloudClass = getClass(classLoader, cloudClassname, Cloud.class);
			
			Class<? extends NameLookup> usernameClass = getClass(classLoader, usernameClassname, NameLookup.class);
			Node cloudMessageNode = findMessageNode(messageCollectionList,
					"/MessageCollection/Cloud[@id='" + cloudid + "']",
					"Missing Cloud description for cloud " + cloudid);
			String description = getChildText(cloudMessageNode, "Description").trim();
			String details = getChildText(cloudMessageNode, "Details").trim();
			PropertyBundle properties = new PropertyBundle();
			if (propertiesLocation != null && propertiesLocation.length() > 0) {
				URL properiesURL = classLoader.getResource(propertiesLocation);
				if (properiesURL == null)
					continue;
				properties.loadPropertiesFromURL(properiesURL);
			}
			List<Node> propertyNodes = cloudNode.selectNodes("Property");
			for(Node node :  propertyNodes ) {
				String key = node.valueOf("@key");
				String value = node.getText();
				properties.setProperty(key, value);
			}
			
			CloudPlugin cloudPlugin = new CloudPlugin(cloudid, classLoader, cloudClass, usernameClass, properties, description, details);
			CloudFactory.registerCloud(cloudPlugin, pluginEnabled);
		}
		
		// Create a DetectorFactory for all Detector nodes
		if (!FindBugs.noAnalysis) try {
			List<Node> detectorNodeList = pluginDescriptor.selectNodes("/FindbugsPlugin/Detector");
			int detectorCount = 0;
			for (Node detectorNode : detectorNodeList) {
				String className = detectorNode.valueOf("@class");
				String speed = detectorNode.valueOf("@speed");
				String disabled = detectorNode.valueOf("@disabled");
				String reports = detectorNode.valueOf("@reports");
				String requireJRE = detectorNode.valueOf("@requirejre");
				String hidden = detectorNode.valueOf("@hidden");
				if (speed == null || speed.length() == 0)
					speed = "fast";

				//System.out.println("Found detector: class="+className+", disabled="+disabled);

				// Create DetectorFactory for the detector
				Class<?> detectorClass = classLoader.loadClass(className);
				if (!Detector.class.isAssignableFrom(detectorClass)
						&& !Detector2.class.isAssignableFrom(detectorClass))
					throw new PluginException("Class " + className + " does not implement Detector or Detector2");
				DetectorFactory factory = new DetectorFactory(
						plugin,
						detectorClass, !disabled.equals("true"),
						speed, reports, requireJRE);
				if (Boolean.valueOf(hidden).booleanValue())
					factory.setHidden(true);
				factory.setPositionSpecifiedInPluginDescriptor(detectorCount++);
				plugin.addDetectorFactory(factory);

				// Find Detector node in one of the messages files,
				// to get the detail HTML.
				Node node = findMessageNode(messageCollectionList,
						"/MessageCollection/Detector[@class='" + className + "']/Details",
						"Missing Detector description for detector " + className);

				Element details = (Element) node;
				String detailHTML = details.getText();
				StringBuilder buf = new StringBuilder();
				buf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
				buf.append("<HTML><HEAD><TITLE>Detector Description</TITLE></HEAD><BODY>\n");
				buf.append(detailHTML);
				buf.append("</BODY></HTML>\n");
				factory.setDetailHTML(buf.toString());
			}
		} catch (ClassNotFoundException e) {
			throw new PluginException("Could not instantiate detector class: " + e, e);
		}

		// Create ordering constraints
		Node orderingConstraintsNode =
			pluginDescriptor.selectSingleNode("/FindbugsPlugin/OrderingConstraints");
		if (orderingConstraintsNode != null) {
			// Get inter-pass and intra-pass constraints
			for (Element constraintElement : (List<Element>) orderingConstraintsNode.selectNodes("./SplitPass|./WithinPass"))
			{
				// Create the selectors which determine which detectors are
				// involved in the constraint
				DetectorFactorySelector earlierSelector = getConstraintSelector(constraintElement, plugin, "Earlier");
				DetectorFactorySelector laterSelector = getConstraintSelector(constraintElement, plugin, "Later");

				// Create the constraint
				DetectorOrderingConstraint constraint = new DetectorOrderingConstraint(
						earlierSelector, laterSelector);

				// Keep track of which constraints are single-source
				constraint.setSingleSource(earlierSelector instanceof SingleDetectorFactorySelector);

				// Add the constraint to the plugin
				if (constraintElement.getName().equals("SplitPass"))
					plugin.addInterPassOrderingConstraint(constraint);
				else
					plugin.addIntraPassOrderingConstraint(constraint);
			}
		}

		// register global Category descriptions
		I18N i18n = I18N.instance();
		for (Document messageCollection : messageCollectionList) {
			List<Node> categoryNodeList = messageCollection.selectNodes("/MessageCollection/BugCategory");
			if (DEBUG) System.out.println("found "+categoryNodeList.size()+" categories in "+messageCollection.getName());
			for (Node categoryNode : categoryNodeList) {
				String key = categoryNode.valueOf("@category");
				if (key.equals(""))
					throw new PluginException("BugCategory element with missing category attribute");
				String shortDesc = getChildText(categoryNode, "Description");
				BugCategory bc = new BugCategory(key, shortDesc);
				boolean b = i18n.registerBugCategory(key, bc);
				if (DEBUG) System.out.println(b
					? "category "+key+" -> "+shortDesc
					: "rejected \""+shortDesc+"\" for category "+key+": "+i18n.getBugCategoryDescription(key));
				/* Now set the abbreviation and details. Be prepared for messages_fr.xml
				 * to specify only the shortDesc (though it should set the abbreviation
				 * too) and fall back to messages.xml for the abbreviation and details. */
				if (!b) bc = i18n.getBugCategory(key); // get existing BugCategory object
				try {
					String abbrev = getChildText(categoryNode, "Abbreviation");
					if (bc.getAbbrev() == null) {
						bc.setAbbrev(abbrev);
						if (DEBUG) System.out.println("category "+key+" abbrev -> "+abbrev);
					}
					else if (DEBUG) System.out.println("rejected abbrev '"+abbrev+"' for category "+key+": "+bc.getAbbrev());
				} catch (PluginException pe) {
					if (DEBUG) System.out.println("missing Abbreviation for category "+key+"/"+shortDesc);
					// do nothing else -- Abbreviation is required, but handle its omission gracefully
				}
				try {
					String details = getChildText(categoryNode, "Details");
					if (bc.getDetailText() == null) {
						bc.setDetailText(details);
						if (DEBUG) System.out.println("category "+key+" details -> "+details);
					}
					else if (DEBUG) System.out.println("rejected details ["+details+"] for category "+key+": ["+bc.getDetailText()+']');
				} catch (PluginException pe) {
					// do nothing -- LongDescription is optional
				}
			}
		}

		// Create BugPatterns
		List<Node> bugPatternNodeList = pluginDescriptor.selectNodes("/FindbugsPlugin/BugPattern");
		for (Node bugPatternNode : bugPatternNodeList) {
			String type = bugPatternNode.valueOf("@type");
			String abbrev = bugPatternNode.valueOf("@abbrev");
			String category = bugPatternNode.valueOf("@category");
			String experimental = bugPatternNode.valueOf("@experimental");

			// Find the matching element in messages.xml (or translations)
			String query = "/MessageCollection/BugPattern[@type='" + type + "']";
			Node messageNode = findMessageNode(messageCollectionList, query,
					"messages.xml missing BugPattern element for type " + type);

			String shortDesc = getChildText(messageNode, "ShortDescription");
			String longDesc = getChildText(messageNode, "LongDescription");
			String detailText = getChildText(messageNode, "Details");
			int cweid = 0;
			try {
				String cweString = bugPatternNode.valueOf("@cweid");
				cweid = Integer.parseInt(cweString);
			} catch (RuntimeException e) {
				assert true; // ignore
			}

			BugPattern bugPattern = new BugPattern(type, abbrev, category,
					Boolean.valueOf(experimental).booleanValue(),
					shortDesc, longDesc, detailText, cweid);
			
			try {
				String deprecatedStr = bugPatternNode.valueOf("@deprecated");
				boolean deprecated = Boolean.valueOf(deprecatedStr).booleanValue();
				if(deprecated){
					bugPattern.setDeprecated(deprecated);
				}
			} catch (RuntimeException e) {
				assert true; // ignore
			}
			
			plugin.addBugPattern(bugPattern);
			boolean unknownCategory = (null == i18n.getBugCategory(category));
			if (unknownCategory) {
				i18n.registerBugCategory(category, new BugCategory(category, category));
				// no desc, but at least now it will appear in I18N.getBugCategories().
				if (DEBUG) System.out.println("Category "+category+" (of BugPattern "
					+type+") has no description in messages*.xml");
				//TODO report this even if !DEBUG
			}
		}

		// Create BugCodes
		Set<String> definedBugCodes = new HashSet<String>();
		for (Document messageCollection : messageCollectionList) {
			List<Node> bugCodeNodeList = messageCollection.selectNodes("/MessageCollection/BugCode");
			for (Node bugCodeNode : bugCodeNodeList) {
				String abbrev = bugCodeNode.valueOf("@abbrev");
				if (abbrev.equals(""))
					throw new PluginException("BugCode element with missing abbrev attribute");
				if (definedBugCodes.contains(abbrev))
					continue;
				String description = bugCodeNode.getText();
				
				String query = "/FindbugsPlugin/BugCode[@abbrev='" + abbrev + "']";
				Node fbNode = pluginDescriptor.selectSingleNode(query);
				int cweid = 0;
				if (fbNode != null) try {
					cweid = Integer.parseInt(fbNode.valueOf("@cweid"));
				} catch (RuntimeException e) {
					assert true; // ignore
				}
				BugCode bugCode = new BugCode(abbrev, description, cweid);
				plugin.addBugCode(bugCode);
				definedBugCodes.add(abbrev);
			}

		}
		
		// If an engine registrar is specified, make a note of its classname
		Node node = pluginDescriptor.selectSingleNode("/FindbugsPlugin/EngineRegistrar");
		if (node != null) {
			String engineClassName = node.valueOf("@class");
			if (engineClassName == null) {
				throw new PluginException("EngineRegistrar element with missing class attribute");
			}

			try {
	            Class<?> engineRegistrarClass = classLoader.loadClass(engineClassName);
	            if (!IAnalysisEngineRegistrar.class.isAssignableFrom(engineRegistrarClass)) {
	            	throw new PluginException(engineRegistrarClass + " does not implement IAnalysisEngineRegistrar");
	            }
	            
	            plugin.setEngineRegistrarClass(engineRegistrarClass.<IAnalysisEngineRegistrar>asSubclass(IAnalysisEngineRegistrar.class));
            } catch (ClassNotFoundException e) {
    			throw new PluginException("Could not instantiate analysis engine registrar class: " + e, e);
            }
			
		}
		try {
			URL bugRankURL = getResource(BugRanker.FILENAME);

			if (bugRankURL == null){ 
				// see https://sourceforge.net/tracker/?func=detail&aid=2816102&group_id=96405&atid=614693
				// plugin can not have bugrank.txt. In this case, an empty bugranker will be created
				if (DEBUG) System.out.println(BugRanker.FILENAME + " not found for plugin " + pluginId);
				//TODO report this even if !DEBUG				
			}
			BugRanker ranker = new BugRanker(bugRankURL);
			plugin.setBugRanker(ranker);
		} catch (IOException e) {
        	throw new PluginException("Couldn't parse \"" + BugRanker.FILENAME + "\"", e);
        }

		// Success!
		// Assign to the plugin field, so getPlugin() can return the
		// new Plugin object.
		this.plugin = plugin;

	}

	private static DetectorFactorySelector getConstraintSelector(
			Element constraintElement,
			Plugin plugin,
			String singleDetectorElementName/*,
			String detectorCategoryElementName*/) throws PluginException {
		Node node = constraintElement.selectSingleNode("./" + singleDetectorElementName);
		if (node != null) {
			String detectorClass = node.valueOf("@class");
			return new SingleDetectorFactorySelector(plugin, detectorClass);
		}

		node = constraintElement.selectSingleNode("./" + singleDetectorElementName + "Category");
		if (node != null) {
			boolean spanPlugins = Boolean.valueOf(node.valueOf("@spanplugins")).booleanValue();
			
			String categoryName = node.valueOf("@name");
			if (!categoryName.equals("")) {
				if (categoryName.equals("reporting")) {
					return new ReportingDetectorFactorySelector(spanPlugins ? null : plugin);
				} else if (categoryName.equals("training")) {
					return new ByInterfaceDetectorFactorySelector(spanPlugins ? null : plugin, TrainingDetector.class);
				} else if (categoryName.equals("interprocedural")) {
					return new ByInterfaceDetectorFactorySelector(spanPlugins ? null : plugin, InterproceduralFirstPassDetector.class);
				} else {
					throw new PluginException("Invalid category name " + categoryName + " in constraint selector node");
				}
			}
		}
		
		node = constraintElement.selectSingleNode("./" + singleDetectorElementName + "Subtypes");
		if (node != null) {
			boolean spanPlugins = Boolean.valueOf(node.valueOf("@spanplugins")).booleanValue();

			String superName = node.valueOf("@super");
			if (!superName.equals("")) {
				try {
					Class<?> superClass = Class.forName(superName);
					return new ByInterfaceDetectorFactorySelector(spanPlugins ? null : plugin, superClass);
				} catch (ClassNotFoundException e) {
					throw new PluginException("Unknown class " + superName + " in constraint selector node"); 
				}
			}
		}
		throw new PluginException("Invalid constraint selector node");
	}

	private String lookupDetectorClass(Plugin plugin, String name) throws PluginException {
		// If the detector name contains '.' characters, assume it is
		// fully qualified already.  Otherwise, assume it is a short
		// name that resolves to another detector in the same plugin.

		if (name.indexOf('.') < 0) {
			DetectorFactory factory = plugin.getFactoryByShortName(name);
			if (factory == null)
				throw new PluginException("No detector found for short name '" + name + "'");
			name = factory.getFullName();
		}
		return name;
	}

	private void addCollection(List<Document> messageCollectionList, String filename)
			throws  PluginException {
		URL messageURL = getResource(filename);
		if (messageURL != null) {
			SAXReader reader = new SAXReader();
            try {
            	InputStreamReader stream = new InputStreamReader(messageURL.openStream());
				Document messageCollection = reader.read(stream);
            	messageCollectionList.add(messageCollection);
            } catch (Exception e) {
            	throw new PluginException("Couldn't parse \"" + messageURL +"\"", e);
            }
			
		}
	}

	private static Node findMessageNode(List<Document> messageCollectionList, String xpath,
										String missingMsg) throws PluginException {

		for (Document document : messageCollectionList) {
			Node node = document.selectSingleNode(xpath);
			if (node != null)
				return node;
		}
		throw new PluginException(missingMsg);
	}

	private static @CheckForNull Node findOptionalMessageNode(List<Document> messageCollectionList, String xpath) throws PluginException {
		for (Document document : messageCollectionList) {
			Node node = document.selectSingleNode(xpath);
			if (node != null)
				return node;
		}
		return null;
	}
	private static String getChildText(Node node, String childName) throws PluginException {
		Node child = node.selectSingleNode(childName);
		if (child == null)
			throw new PluginException("Could not find child \"" + childName + "\" for node");
		return child.getText();
	}
	private static @CheckForNull String getOptionalChildText(Node node, String childName)  {
		Node child = node.selectSingleNode(childName);
		if (child == null)
			return null;
		return child.getText();
	}
	public boolean isCorePlugin() {
		return corePlugin;
	}
}

// vim:ts=4
