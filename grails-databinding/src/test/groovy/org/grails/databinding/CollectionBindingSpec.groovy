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

import spock.lang.Ignore
import spock.lang.Specification

class CollectionBindingSpec extends Specification {

    void 'Test indexed binding to a an uninitialized List'() {
        given:
        def binder = new SimpleDataBinder()
        def company = new Company()

        when:
        binder.bind company, [name: 'Some Company', 'departments[0]': [name: 'Department Zero'], 'departments[1]': [name: 'Department One'], 'departments[9]': [name: 'Department Nine']]

        then:
        company.name == 'Some Company'
        company.departments instanceof List
        company.departments.size() == 10
        company.departments[0] instanceof Department
        company.departments[0].name == 'Department Zero'
        company.departments[1] instanceof Department
        company.departments[1].name == 'Department One'
        company.departments[2] == null
        company.departments[3] == null
        company.departments[4] == null
        company.departments[5] == null
        company.departments[6] == null
        company.departments[7] == null
        company.departments[8] == null
        company.departments[9] instanceof Department
        company.departments[9].name == 'Department Nine'
    }

    void 'Test indexed binding to a partially initialized List'() {
        given:
        def binder = new SimpleDataBinder()
        def company = new Company()
        company.departments = []
        company.departments[1] = new Department(name: 'Original Department One')
        company.departments[2] = new Department(name: 'Original Department Two')
        company.departments[3] = new Department(name: 'Original Department Three')

        when:
        binder.bind company, [name: 'Some Company',
            'departments[0]': [name: 'Department Zero'],
            'departments[1]': [name: 'Department One'],
            'departments[2]': [numberOfEmployees: '99'],
            'departments[3]': null,
            'departments[9]': [name: 'Department Nine', numberOfEmployees: '42']]

        then:
        company.name == 'Some Company'
        company.departments instanceof List
        company.departments.size() == 10
        company.departments[0] instanceof Department
        company.departments[0].name == 'Department Zero'
        company.departments[1] instanceof Department
        company.departments[1].name == 'Department One'
        company.departments[2] instanceof Department
        company.departments[2].name == 'Original Department Two'
        company.departments[2].numberOfEmployees == 99
        company.departments[3] == null
        company.departments[4] == null
        company.departments[5] == null
        company.departments[6] == null
        company.departments[7] == null
        company.departments[8] == null
        company.departments[9] instanceof Department
        company.departments[9].name == 'Department Nine'
        company.departments[9].numberOfEmployees == 42
    }
    void 'Test white space around index'() {
        given:
        def binder = new SimpleDataBinder()
        def company = new Company()
        company.departments = []

        when:
        binder.bind company, [name: 'Some Company',
                              'departments[ 2  ]': [numberOfEmployees: '99', name: 'Department Two']]

        then:
        company.name == 'Some Company'
        company.departments instanceof List
        company.departments.size() == 3
        company.departments[0] == null
        company.departments[1] == null
        company.departments[2] instanceof Department
        company.departments[2].name == 'Department Two'
        company.departments[2].numberOfEmployees == 99
    }
}

class Company {
    String name
    List<Department> departments
}

class Department {
    String name
    Integer numberOfEmployees
}
