/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

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
import org.onap.sdc.common.onaplog.enums.LogLevel;


public class CommonLocator implements TargetLocator {

    private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

    private Set<URI> searchPaths = new LinkedHashSet<>();

    /* will create a locator with 2 default search paths: the file directory
     * from where the app was and the jar from which this checker (actually this
     * class) was loaded */
    CommonLocator() {
        addSearchPath(
            Paths.get(".").toAbsolutePath().normalize().toUri());
    }

    public boolean addSearchPath(URI theURI) {

        if (!theURI.isAbsolute()) {
            errLogger.log(LogLevel.WARN, this.getClass().getName(), "Search paths must be absolute uris: {}", theURI);
            return false;
        }

        return searchPaths.add(theURI);
    }

    public boolean addSearchPath(String thePath) {
        URI suri;
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
                pis = getPathInputStream(puri,theName);
                if (pis == null){
                    return null;
                }
            }
        }
        catch(URISyntaxException urisx) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "TargetResolver failed attempting {} {}", theName, urisx);
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
            }
            finally {
                if (pis!= null) {
                    try {
                        pis.close();
                    }
                    catch (IOException iox) {
                        debugLogger.log(LogLevel.ERROR, this.getClass().getName(),"Error closing input stream {}", iox);
                    }
                }
            }
        }

        return null;
    }

    private InputStream getPathInputStream(URI puri, String theName){
        InputStream res = null;
        try (InputStream pis = puri.toURL().openStream()){
            res = pis;
        }
        catch (IOException iox) {
            errLogger.log(LogLevel.WARN, this.getClass().getName(), "The path {} is an absolute uri but it cannot be opened {}", theName, iox);
        }
        return res;
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
