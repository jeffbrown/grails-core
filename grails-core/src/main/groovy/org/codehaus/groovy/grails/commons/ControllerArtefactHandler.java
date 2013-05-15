/*
 * Copyright 2004-2005 the original author or authors.
 *
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
 */
package org.codehaus.groovy.grails.commons;

import grails.util.Environment;

import java.util.Map;

import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.springframework.context.ApplicationContext;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * Lookup controllers for uris.
 *
 * <p>This class is responsible for looking up controller classes for uris.</p>
 *
 * <p>Lookups are cached in non-development mode, and the cache size can be controlled using the grails.urlmapping.cache.maxsize config property.</p>
 *
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class ControllerArtefactHandler extends ArtefactHandlerAdapter implements GrailsApplicationAware {

    private static final String URL_MAPPING_CACHE_MAX_SIZE = "grails.urlmapping.cache.maxsize";
    private static final GrailsClass NO_CONTROLLER = new AbstractGrailsClass(Object.class, "Controller") {};

    public static final String TYPE = "Controller";
    public static final String PLUGIN_NAME = "controllers";
    private ConcurrentLinkedHashMap<String, GrailsClass> uriToControllerClassCache;
    private ArtefactInfo artefactInfo;

    private GrailsApplication grailsApplication;

    public ControllerArtefactHandler() {
        super(TYPE, GrailsControllerClass.class, DefaultGrailsControllerClass.class,
                DefaultGrailsControllerClass.CONTROLLER, false);
    }

    @Override
    public void initialize(ArtefactInfo artefacts) {
        Object cacheSize = grailsApplication.getFlatConfig().get(URL_MAPPING_CACHE_MAX_SIZE);
        if (cacheSize == null) {
            cacheSize = 10000;
        }

        uriToControllerClassCache = new ConcurrentLinkedHashMap.Builder<String, GrailsClass>()
                .initialCapacity(500)
                .maximumWeightedCapacity(new Integer(cacheSize.toString()))
                .build();

        artefactInfo = artefacts;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public GrailsClass getArtefactForFeature(Object featureId) {
        if (artefactInfo == null) {
            return null;
        }

        String uri;
        String pluginName = null;
        String controllerNamespace = null;

        if (featureId instanceof Map) {
            Map featureIdMap = (Map)featureId;
            uri = (String)featureIdMap.get("uri");
            pluginName = (String)featureIdMap.get("pluginName");
            controllerNamespace = (String)featureIdMap.get("controllerNamespace");
        } else {
            uri = featureId.toString();
        }

        String cacheKey = (controllerNamespace != null ? controllerNamespace : "") + ":" + (pluginName != null ? pluginName : "") + ":" + uri;

        GrailsClass controllerClass = uriToControllerClassCache.get(cacheKey);
        if (controllerClass == null) {
            final ApplicationContext mainContext = grailsApplication.getMainContext();
            GrailsPluginManager grailsPluginManager = null;
            if (mainContext.containsBean(GrailsPluginManager.BEAN_NAME)) {
                final Object pluginManagerBean = mainContext.getBean(GrailsPluginManager.BEAN_NAME);
                if (pluginManagerBean instanceof GrailsPluginManager) {
                    grailsPluginManager = (GrailsPluginManager) pluginManagerBean;
                }
            }
            final GrailsClass[] controllerClasses = artefactInfo.getGrailsClasses();
            // iterate in reverse in order to pick up application classes first
            for (int i = (controllerClasses.length-1); i >= 0; i--) {
                GrailsClass c = controllerClasses[i];
                if (((GrailsControllerClass) c).mapsToURI(uri)) {
                    boolean foundController = false;
                    if(pluginName == null && controllerNamespace == null) {
                        foundController = true;
                    } else {
                        boolean pluginMatches = false;
                        boolean controllerNamespaceMatches = false;
                        
                        controllerNamespaceMatches = namespaceMatches((GrailsControllerClass)c, controllerNamespace);
                        
                        if(controllerNamespaceMatches) {
                            pluginMatches = pluginMatches(c, pluginName, grailsPluginManager);
                        }
                        
                        foundController = pluginMatches && controllerNamespaceMatches;
                    }
                    if (foundController) {
                        controllerClass = c;
                        break;
                    }
                }
            }
            if (controllerClass == null) {
                controllerClass = NO_CONTROLLER;
            }

            // don't cache for dev environment
            if (Environment.getCurrent() != Environment.DEVELOPMENT) {
                uriToControllerClassCache.put(cacheKey, controllerClass);
            }
        }

        if (controllerClass == NO_CONTROLLER) {
            controllerClass = null;
        }
        return controllerClass;
    }

    /**
     * 
     * @param c the class to inspect
     * @param controllerNamespace a controller namespace
     * @return true if c is in the controllerNamespace namespace
     */
    protected boolean namespaceMatches(GrailsControllerClass c, String controllerNamespace) {
        boolean controllerNamespaceMatches;
        if(controllerNamespace != null) {
            controllerNamespaceMatches = controllerNamespace.equals(c.getNamespace());
        } else {
            controllerNamespaceMatches = (c.getNamespace() == null);
        }
        return controllerNamespaceMatches;
    }
    
    /**
     * 
     * @param c the class to inspect
     * @param pluginName the name of a plugin
     * @param grailsPluginManager the plugin manager
     * @return true if c is provided by a plugin with the name pluginName or if pluginName is null, otherwise false
     */
    protected boolean pluginMatches(GrailsClass c, String pluginName, GrailsPluginManager grailsPluginManager) {
        boolean pluginMatches = false;
        if (pluginName != null && grailsPluginManager != null) {
            final GrailsPlugin pluginForClass = grailsPluginManager.getPluginForClass(c.getClazz());
            if (pluginForClass != null && pluginName.equals(pluginForClass.getName())) {
                pluginMatches = true;
            }
        } else {
            pluginMatches = true;
        }
        return pluginMatches;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
}
