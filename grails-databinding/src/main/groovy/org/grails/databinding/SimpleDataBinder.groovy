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
package org.grails.databinding

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult

import java.lang.reflect.ParameterizedType
import java.util.regex.Matcher

import org.grails.databinding.converters.BooleanConversionHelper
import org.grails.databinding.converters.ByteConversionHelper
import org.grails.databinding.converters.CharConversionHelper
import org.grails.databinding.converters.DateConverter
import org.grails.databinding.converters.DoubleConversionHelper
import org.grails.databinding.converters.FloatConversionHelper
import org.grails.databinding.converters.IntegerConversionHelper
import org.grails.databinding.converters.LongConversionHelper
import org.grails.databinding.converters.ShortConversionHelper
import org.grails.databinding.converters.ValueConverter
import org.grails.databinding.errors.SimpleBindingError
import org.grails.databinding.events.DataBindingListener
import org.grails.databinding.xml.GPathResultMap

@CompileStatic
class SimpleDataBinder implements DataBinder {

    protected Map<Class, DataConverter> typeConverters = new HashMap<Class, DataConverter>()
    protected Map<Class, ValueConverter> conversionHelpers = new HashMap<Class, ValueConverter>()

    static final INDEXED_PROPERTY_REGEX = /(.*)\[(\d+)\]/

    SimpleDataBinder() {
        conversionHelpers.put(Boolean.TYPE, new BooleanConversionHelper())
        conversionHelpers.put(Byte.TYPE, new ByteConversionHelper())
        conversionHelpers.put(Character.TYPE, new CharConversionHelper())
        conversionHelpers.put(Short.TYPE, new ShortConversionHelper())
        conversionHelpers.put(Integer.TYPE, new IntegerConversionHelper())
        conversionHelpers.put(Long.TYPE, new LongConversionHelper())
        conversionHelpers.put(Float.TYPE, new FloatConversionHelper())
        conversionHelpers.put(Double.TYPE, new DoubleConversionHelper())

        registerTypeConverter(java.util.Date.class, new DateConverter(java.util.Date))
        registerTypeConverter(java.sql.Date.class, new DateConverter(java.sql.Date))
        registerTypeConverter(java.util.Calendar.class, new DateConverter(java.util.Calendar))
    }

    public registerTypeConverter(Class clazz, DataConverter converter) {
        typeConverters[clazz] = converter
    }

    void bind(obj, Map source) {
        bind obj, source, null, null, null
    }

    void bind(obj, Map source, DataBindingListener listener) {
        bind obj, source, null, null, listener
    }

    void bind(obj, Map source, List whiteList) {
        bind obj, source, whiteList, null, null
    }

    void bind(obj, Map source, List whiteList, List blackList) {
        bind obj, source, whiteList, blackList, null
    }

    void bind(obj, GPathResult gpath) {
        bind obj, new GPathResultMap(gpath)
    }

    protected isOkToBind(String propName, List whiteList, List blackList) {
        !blackList?.contains(propName) && (!whiteList || whiteList.contains(propName))
    }

    void bind(obj, Map<String, Object> source, List whiteList, List blackList, DataBindingListener listener) {
        def structuredPropertiesProcessed = []
        source.each {String propName, val ->
            if(isOkToBind(propName, whiteList, blackList)) {
                def isStructuredEditorProperty = false
                def metaProperty = obj.metaClass.getMetaProperty propName
                if(!metaProperty && propName.indexOf('_') > 0) {
                    def simplePropName = propName[0..<propName.indexOf('_')]
                    if(isOkToBind(simplePropName, whiteList, blackList)) {
                        if(!structuredPropertiesProcessed.contains(simplePropName)) {
                            structuredPropertiesProcessed << simplePropName
                            metaProperty = obj.metaClass.getMetaProperty simplePropName
                            if(metaProperty) {
                                propName = simplePropName
                                isStructuredEditorProperty = true
                            }
                        }
                    }
                }
                if(metaProperty) {
                    def propertyType = metaProperty.type
                    if(typeConverters.containsKey(propertyType)) {
                        def converter = typeConverters[propertyType]
                        if(!(converter instanceof DateConverter) || isStructuredEditorProperty) {
                            val = typeConverters[propertyType].convertValue obj, propName, source
                        }
                    }
                    setPropertyValue obj, source, propName, val, listener
                } else {
                    processProperty obj, propName, val, source, whiteList, blackList, listener
                }
            }
        }
    }

    protected processProperty(obj, String propName, val, Map source, List whiteList, List blackList, DataBindingListener listener) {
        if(propName ==~ INDEXED_PROPERTY_REGEX) {
            Matcher matcher = propName =~ INDEXED_PROPERTY_REGEX
            matcher[0]
            def simplePropertyName = matcher.group(1)
            def metaProperty = obj.metaClass.getMetaProperty simplePropertyName
            if(metaProperty) {
                def index = Integer.parseInt(matcher.group(2))
                def propertyType = metaProperty.type
                if(Collection.isAssignableFrom(propertyType)) {
                    Collection collectionInstance = (Collection)obj[simplePropertyName]
                    if(collectionInstance == null) {
                        collectionInstance = initializeCollection obj, simplePropertyName, propertyType
                    }
                    def indexedInstance = collectionInstance[index]
                    if(indexedInstance == null) {
                        Class genericType = ((ParameterizedType)obj.getClass().getDeclaredField(simplePropertyName).genericType).getActualTypeArguments()[0]
                        indexedInstance = genericType.newInstance()
                        addElementToCollectionAt collectionInstance, index, indexedInstance
                    }
                    if(val instanceof Map) {
                        bind indexedInstance, (Map)val, whiteList, blackList, listener
                    } else if (val == null && indexedInstance != null) {
                        addElementToCollectionAt collectionInstance, index, null
                    }
                }
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected addElementToCollectionAt(Collection collection, index, val) {
        collection[index] = val
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Collection initializeCollection(obj, String propertyName, Class type) {
        if(List.isAssignableFrom(type)) {
            obj[propertyName] = new ArrayList()
        }
        obj[propertyName]
    }

    protected ValueConverter getValueConverterForField(obj, String propName) {
        def converter
        try {
            def field = obj.getClass().getDeclaredField propName
            if(field) {
                def annotation = field.getAnnotation BindUsing
                if(annotation) {
                    def valueClass = annotation.value()
                    if(Closure.isAssignableFrom(valueClass)) {
                        Closure closure = (Closure)valueClass.newInstance(null, null)
                        converter = new ClosureValueConverter(converterClosure: closure.curry(obj))
                    }
                }
            }
        } catch (Exception e) {
        }
        converter
    }

    protected ValueConverter getValueConverterForClass(obj, String propName) {
        def converter
        def annotation = obj.getClass().getAnnotation BindUsing
        if(annotation) {
            def valueClass = annotation.value()
            if(DataConverter.isAssignableFrom(valueClass)) {
                DataConverter dataConverter = (DataConverter)valueClass.newInstance()
                converter = new ClosureValueConverter(converterClosure: { Map it -> dataConverter.convertValue(obj, propName, it) })
            }
        }
        converter
    }

    protected ValueConverter getValueConverter(obj, String propName) {
        def converter = getValueConverterForField obj, propName
        if(!converter) {
            converter = getValueConverterForClass obj, propName
        }
        converter
    }

    protected setPropertyValue(obj, Map source, String propName, propertyValue, DataBindingListener listener) {
        def converter = getValueConverter obj, propName

        if(converter) {
            propertyValue = converter.convert source
        }
        if(listener == null || listener.beforeBinding(obj, propName, propertyValue) != false) {
            def metaProperty = obj.metaClass.getMetaProperty(propName)
            def propertyType = metaProperty.type

            if(propertyValue == null || propertyType == Object || propertyType.isAssignableFrom(propertyValue.getClass())) {
                obj[propName] = propertyValue
            } else {
                try {
                    if(propertyValue instanceof Map) {
                        initializeProperty(obj, propName, propertyType)
                        bind obj[propName], propertyValue
                    } else {
                        obj[propName] = convert(propertyType, propertyValue)
                    }
                } catch (Exception e) {
                    if(listener) {
                        def error = new SimpleBindingError(obj, propName, propertyValue, e.cause ?: e)
                        listener.bindingError error
                    }
                }
            }
        } else if(listener != null && propertyValue instanceof Map && obj[propName] != null) {
            bind obj[propName], propertyValue
        }
        listener?.afterBinding obj, propName
    }

    protected initializeProperty(obj, String propName, Class propertyType) {
        obj[propName] = propertyType.newInstance()
    }

    protected convert(Class typeToConvertTo, value) {
        if(conversionHelpers.containsKey(typeToConvertTo)) {
            return conversionHelpers.get(typeToConvertTo).convert(value)
        }
        typeToConvertTo.newInstance value
    }
}
