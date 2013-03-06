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
package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import org.codehaus.groovy.grails.web.binding.GormAwareDataBinder

class GormAwareDataBinderSpec extends GormSpec {

    void 'Test dotted id binding'() {
        given:
        def binder = new GormAwareDataBinder()
        def author = new Author(name: 'David Foster Wallace').save(flush: true)
        def publication = new Publication()

        when:
        binder.bind publication, [title: 'Infinite Jest', 'author.id': author.id]

        then:
        publication.title == 'Infinite Jest'
        publication.author.name == 'David Foster Wallace'

        when:
        publication.author = null
        publication.title = null
        binder.bind publication, [title: 'Infinite Jest', 'author.id': author.id], [], ['author']

        then:
        publication.title == 'Infinite Jest'
        publication.author == null
    }

    void 'Test binding to a hasMany List'() {
        given:
        def binder = new GormAwareDataBinder(grailsApplication: grailsApplication)
        def publisher = new Publisher()

        when:
        binder.bind publisher, [name: 'Apress',
            'publications[0]': [title: 'DGG', author: [name: 'Graeme']],
            'publications[2]': [title: 'DGG2', author: [name: 'Jeff']]]

        then:
        publisher.name == 'Apress'
        publisher.publications instanceof List
        publisher.publications.size() == 3
        publisher.publications[0].title == 'DGG'
        publisher.publications[0].author.name == 'Graeme'
        publisher.publications[1] == null
        publisher.publications[2].title == 'DGG2'
        publisher.publications[2].author.name == 'Jeff'
    }

    @Override
    List getDomainClasses() {
        [Publication, Author, Publisher]
    }
}

@Entity
class Publisher {
    String name
    static hasMany = [publications: Publication]
    List publications
}

@Entity
class Publication {
    String title
    Author author
}

@Entity
class Author {
    String name
}
