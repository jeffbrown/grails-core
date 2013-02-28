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
package org.grails.databinding.xml

import groovy.util.slurpersupport.GPathResult


class GPathResultMap implements Map {
    private GPathResult gpath

    GPathResultMap(GPathResult gpath) {
        this.gpath = gpath
    }

    @Override
    public int size() {
        def uniqueNames = [] as Set
        gpath.children().each { child ->
            uniqueNames << child.name()
        }
        uniqueNames.size()
    }

    @Override
    public boolean containsKey(Object key) {
        gpath[key].size()
    }

    @Override
    public Set entrySet() {
        def entries = [] as Set
        gpath.childNodes().each { childNode ->
            def name = childNode.name()
            def value = get name
            entries << new AbstractMap.SimpleImmutableEntry(name, value)
        }
        return entries
    }

    @Override
    public Object get(Object key) {
        def value = gpath[key]
        if(value.size() > 1) {
            def list = []
            value.iterator().each {
                list << it.text()
            }
            return list
        } else if(value.children().size() == 0) {
            return value.text()
        }
        return new GPathResultMap(value)
    }

    @Override
    public Set keySet() {
        gpath.children().collect { it.name() } as Set
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException()
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException()
    }

    @Override
    public boolean isEmpty() {
        !size()
    }

    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException()
    }

    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException()
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException()
    }

    @Override
    public Collection values() {
        throw new UnsupportedOperationException()
    }
}
