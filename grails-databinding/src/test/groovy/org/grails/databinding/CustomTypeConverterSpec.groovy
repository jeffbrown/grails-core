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

class CustomTypeConverterSpec extends Specification {

    void 'Test custom type converter'() {
        given:
        def binder = new SimpleDataBinder()
        binder.registerTypeConverter Address, new AddressConverter()
        def resident = new Resident()
        def bindingSource = [:]
        bindingSource.name = 'Scott'
        bindingSource.homeAddress_someCity = "Scott's Home City"
        bindingSource.homeAddress_someState = "Scott's Home State"
        bindingSource.workAddress_someState = "Scott's Work State"

        when:
        binder.bind resident, bindingSource

        then:
        resident.name == 'Scott'
        resident.homeAddress
        resident.homeAddress.city == "Scott's Home City"
        resident.homeAddress.state == "Scott's Home State"
        resident.workAddress
        resident.workAddress.state == "Scott's Work State"
        resident.workAddress.city == null
    }
}

class Resident {
    String name
    Address homeAddress
    Address workAddress
}

class Address {
    String state
    String city
}

class AddressConverter implements DataConverter {

    @Override
    public Object convertValue(Object obj, String propertyName, Map<String, Object> source) {
        def address = new Address()

        address.state = source[propertyName + '_someState']
        address.city = source[propertyName + '_someCity']

        address
    }
}
