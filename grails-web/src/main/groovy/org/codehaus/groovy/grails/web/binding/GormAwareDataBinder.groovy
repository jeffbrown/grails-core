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

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.databinding.SimpleDataBinder
import org.grails.databinding.events.DataBindingListener

@CompileStatic
class GormAwareDataBinder extends SimpleDataBinder {
    
    GrailsApplication grailsApplication

    @Override    
    protected Class<?> getReferencedTypeForCollection(String name, Object target) {
        Class referencedType = null
        if (grailsApplication != null) {
            GrailsDomainClass dc = (GrailsDomainClass) grailsApplication.getArtefact(
                    DomainClassArtefactHandler.TYPE, target.getClass().getName());
            if (dc != null) {
                GrailsDomainClassProperty domainProperty = dc.getPersistentProperty(name);
                if (domainProperty != null) {
                    referencedType = domainProperty.getReferencedPropertyType();
                }
            }
        }
        referencedType ?: super.getReferencedTypeForCollection(name, target)
    }

    
	@Override
	protected processProperty(obj, String propName, val, Map source,  List whiteList, List blackList, DataBindingListener listener) {
		if(propName.endsWith('.id')) {
			def simplePropName = propName[0..-4]
			if(isOkToBind(simplePropName, whiteList, blackList)) {
				def metaProperty = obj.metaClass.getMetaProperty simplePropName
				if(metaProperty) {
					def persistedInstance = InvokerHelper.invokeStaticMethod(((MetaBeanProperty)metaProperty).field.type, 'get', val)
					setPropertyValue obj, source, simplePropName, persistedInstance, listener
				}
			}
		} else {
			super.processProperty obj, propName, val, source, whiteList, blackList, listener
		}
	}
}
