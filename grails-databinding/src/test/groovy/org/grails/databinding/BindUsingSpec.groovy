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

import spock.lang.Specification

class BindUsingSpec extends Specification {

    void 'Test BindUsing for specific property'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new ClassWithBindUsingOnProperty()

        when:
        binder.bind(obj, [name: 'Jeff Was Here'])

        then:
        'JEFF WAS HERE' == obj.name
    }

    void 'Test BindUsing on the class'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new ClassWithBindUsing()

        when:
        binder.bind(obj, [doubleIt: 9, tripleIt: 20, leaveIt: 30])

        then:
        obj.doubleIt == 18
        obj.tripleIt == 60
        obj.leaveIt == 30
    }
}

class ClassWithBindUsingOnProperty {
    @BindUsing({
        obj, source -> source['name']?.toUpperCase()
    })
    String name
}

@BindUsing(MultiplyingConverter)
class ClassWithBindUsing {
    Integer leaveIt
    Integer doubleIt
    Integer tripleIt
}

class MultiplyingConverter implements DataConverter {
    @Override
    public Object convertValue(Object obj, String propertyName, Map<String, Object> source) {
        def value = source[propertyName]
        def convertedValue = value
        switch(propertyName) {
            case 'doubleIt':
                convertedValue = value * 2
                break
            case 'tripleIt':
                convertedValue = value * 3
                break
        }
        convertedValue
    }
}
