

/* Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.web.binding

import grails.util.Environment
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.databinding.SimpleDataBinder
import org.grails.databinding.events.DataBindingListener
import org.grails.databinding.xml.GPathResultMap

@CompileStatic
class GormAwareDataBinder extends SimpleDataBinder {
    private static final Map<Class, List> CLASS_TO_BINDING_INCLUDE_LIST = new ConcurrentHashMap<Class, List>()
    GrailsApplication grailsApplication

    void bind(obj, Map source) {
        bind obj, source, getBindingIncludeList(obj), null, null
    }

    void bind(obj, Map source, DataBindingListener listener) {
        bind obj, source, getBindingIncludeList(obj), null, listener
    }

    void bind(obj, GPathResult gpath) {
        bind obj, new GPathResultMap(gpath), getBindingIncludeList(obj)
    }

    @Override
    protected Class<?> getReferencedTypeForCollection(String name, Object target) {
        Class referencedType = null
        if (grailsApplication != null) {
            GrailsDomainClass dc = (GrailsDomainClass) grailsApplication.getArtefact(
            DomainClassArtefactHandler.TYPE, target.getClass().getName())
            if (dc != null) {
                GrailsDomainClassProperty domainProperty = dc.getPersistentProperty(name)
                if (domainProperty != null) {
                    referencedType = domainProperty.getReferencedPropertyType()
                }
            }
        }
        referencedType ?: super.getReferencedTypeForCollection(name, target)
    }

    @Override
    protected processProperty(obj, String propName, val, Map source,  List whiteList, List blackList, DataBindingListener listener) {
        if(propName.endsWith('.id')) {
            def simplePropName = propName[0..-4]
            def descriptor = getIndexedPropertyReferenceDescriptor simplePropName
            if(descriptor) {
                def metaProperty = obj.metaClass.getMetaProperty descriptor.propertyName
                if(metaProperty) {
                    def collection = obj[descriptor.propertyName]
                    if(collection instanceof Collection) {
                        def referencedType = getReferencedTypeForCollection descriptor.propertyName, obj
                        if(referencedType) {
                            addElementToCollectionAt (obj, descriptor.propertyName, collection, descriptor.index, 'null' == val ? null : InvokerHelper.invokeStaticMethod(referencedType, 'get', val.toString()))
                        }
                    }
                }
            } else {
                if(isOkToBind(simplePropName, whiteList, blackList)) {
                    def metaProperty = obj.metaClass.getMetaProperty simplePropName
                    if(metaProperty) {
                        def persistedInstance = 'null' == val ? null : InvokerHelper.invokeStaticMethod(((MetaBeanProperty)metaProperty).field.type, 'get', val)
                        setPropertyValue obj, source, simplePropName, persistedInstance, listener
                    }
                }
            }
        } else {
            super.processProperty obj, propName, val, source, whiteList, blackList, listener
        }
    }

    private static List getBindingIncludeList(final Object object) {
        List includeList = Collections.EMPTY_LIST
        try {
            final Class<? extends Object> objectClass = object.getClass()
            if (CLASS_TO_BINDING_INCLUDE_LIST.containsKey(objectClass)) {
                includeList = CLASS_TO_BINDING_INCLUDE_LIST.get(objectClass)
            } else {
                final Field whiteListField = objectClass.getDeclaredField(DefaultASTDatabindingHelper.DEFAULT_DATABINDING_WHITELIST)
                if (whiteListField != null) {
                    if ((whiteListField.getModifiers() & Modifier.STATIC) != 0) {
                        final Object whiteListValue = whiteListField.get(objectClass)
                        if (whiteListValue instanceof List) {
                            includeList = (List)whiteListValue
                        }
                    }
                }
                if (!Environment.getCurrent().isReloadEnabled()) {
                    CLASS_TO_BINDING_INCLUDE_LIST.put objectClass, includeList
                }
            }
        } catch (Exception e) {
        }
        includeList
    }

    @Override
    protected addElementToCollectionAt(obj, String propertyName, Collection collection, index, val) {
        def methodName = "addTo" + GrailsNameUtils.getClassName(propertyName)
        if(GrailsMetaClassUtils.invokeMethodIfExists(obj, methodName, [val] as Object[]) == null) {
            super.addElementToCollectionAt obj, propertyName, collection, index, val
        }
    }
}
