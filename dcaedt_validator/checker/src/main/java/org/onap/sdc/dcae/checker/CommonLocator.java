package org.onap.sdc.dcae.checker;

import java.io.InputStream;
import java.io.IOException;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;

import java.nio.file.Paths;

import java.util.Set;
import java.util.LinkedHashSet;

import com.google.common.collect.Iterables;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;


public class CommonLocator implements TargetLocator {

	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	private Set<URI> searchPaths = new LinkedHashSet(); 

	/* will create a locator with 2 default search paths: the file directory 
	 * from where the app was and the jar from which this checker (actually this
	 * class) was loaded */
	public CommonLocator() {
		addSearchPath(
			Paths.get(".").toAbsolutePath().normalize().toUri());
	}
	
	public CommonLocator(String... theSearchPaths) {
		for (String path: theSearchPaths) {
			addSearchPath(path);
		}
	}

	public boolean addSearchPath(URI theURI) {

		if (!theURI.isAbsolute()) {
			errLogger.log(LogLevel.WARN, this.getClass().getName(), "Search paths must be absolute uris: {}", theURI);
			return false;
		}

		return searchPaths.add(theURI);
	}

	public boolean addSearchPath(String thePath) {
		URI suri = null; 
		try {
			suri = new URI(thePath);
		}
		catch(URISyntaxException urisx) {
			errLogger.log(LogLevel.WARN, this.getClass().getName(), "Invalid search path: {} {}", thePath, urisx);
			return false;
		}

		return addSearchPath(suri);
	}

	public Iterable<URI> searchPaths() {
		return Iterables.unmodifiableIterable(this.searchPaths);
	}

	/**
	 * Takes the given path and first URI resolves it and then attempts to open
	 * it (a way of verifying its existence) against each search path and stops
	 * at the first succesful test.
	 */
	public Target resolve(String theName) {
		URI puri = null;
		InputStream	pis = null;
		
		//try classpath
		URL purl = getClass().getClassLoader().getResource(theName);
		if (purl != null) {
			try {
				return new Target(theName, purl.toURI());
			}
			catch (URISyntaxException urisx) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "The file {} wasn't found {}", theName, urisx);
			}
		}

		//try absolute
		try {
			puri = new URI(theName);
			if (puri.isAbsolute()) {
				try {
					pis = puri.toURL().openStream();
				}
				catch (IOException iox) {
					errLogger.log(LogLevel.WARN, this.getClass().getName(), "The path {} is an absolute uri but it cannot be opened {}", theName, iox);
					return null;
				}
			}
		}
		catch(URISyntaxException urisx) {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "TargetResolver failed attempting {} {}", puri, urisx);
			//keep it silent but what are the chances ..
		}

		//try relative to the search paths
		for (URI suri: searchPaths) {
			try {
				puri = suri.resolve(theName);
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "TargetResolver trying {}", puri);
				pis = puri.toURL().openStream();
				return new Target(theName, puri.normalize());
			}
			catch (Exception x) {
				debugLogger.log(LogLevel.ERROR, this.getClass().getName(), "TargetResolver failed attempting {} {}", puri, x);
				continue;
			}
			finally {
				if (pis!= null) {
					try {
						pis.close();
					}
					catch (IOException iox) {
					}
				}
			}
		}

		return null;
	}

	public String toString() {
		return "CommonLocator(" + this.searchPaths + ")";
	}

	
	public static void main(String[] theArgs) {
		TargetLocator tl = new CommonLocator();
		tl.addSearchPath(java.nio.file.Paths.get("").toUri());
		tl.addSearchPath("file:///");
		debugLogger.log(LogLevel.DEBUG, CommonLocator.class.getName(), tl.resolve(theArgs[0]).toString());
	}
}
