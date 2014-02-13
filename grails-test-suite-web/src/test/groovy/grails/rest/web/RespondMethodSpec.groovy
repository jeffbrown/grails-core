/*
 * Copyright 2012 the original author or authors.
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

package grails.rest.web

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesFactoryBean
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.web.servlet.ModelAndView

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

@TestFor(BookController)
@Mock(Book)
class RespondMethodSpec extends Specification{

    void setup() {
        def ga = grailsApplication
        ga.config.grails.mime.types =
            [ html: ['text/html','application/xhtml+xml'],
            xml: ['text/xml', 'application/xml'],
            text: 'text/plain',
            js: 'text/javascript',
            rss: 'application/rss+xml',
            atom: 'application/atom+xml',
            css: 'text/css',
            csv: 'text/csv',
            all: '*/*',
            json: ['application/json','text/json'],
            form: 'application/x-www-form-urlencoded',
            multipartForm: 'multipart/form-data'
        ]

        defineBeans {
            mimeTypes(MimeTypesFactoryBean) {
                grailsApplication = ga
            }
        }
    }

    @Ignore
    void "Test that the respond method produces the correct model for a domain instance and no specific content type"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            webRequest.actionName = 'show'
            controller.show(book)
            def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
            modelAndView != null
            modelAndView instanceof ModelAndView
            modelAndView.model.book == book
            modelAndView.viewName == 'show'
    }

    @Ignore
    void "Test that the respond method produces XML for a domain instance and a content type of XML"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'xml'

            def result = controller.show(book)

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'text/xml'
            response.xml.title.text() == 'The Stand'
    }

    @Ignore
    void "Test that the respond method produces XML for a list of domains and a content type of XML"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'xml'
            def result = controller.index()

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'text/xml'
    }

    @Ignore
    void "Test that the respond method produces errors XML for a domain instance that has errors and a content type of XML"() {
        given:"A book instance"
            def book = new Book(title: "")
            book.validate()

        when:"The respond method is used to render a response"
            response.format = 'xml'

            def result = controller.show(book)

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'text/xml'
            response.xml.error.message.text() == 'Property [title] of class [class grails.rest.web.Book] cannot be null'
    }

    @Ignore
    void "Test that the respond method produces JSON for a domain instance and a content type of JSON"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'json'

        def result = controller.show(book)

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'application/json'
            response.json.title == 'The Stand'
    }

    @Ignore
    void "Test that the respond method produces a 404 for a format not supported"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'xml'

            def result = controller.showWithFormats(book.id)

        then:"A modelAndView and view is produced"
            response.status == 404
    }

    @Ignore
    void "Test that the respond method produces JSON for an action that specifies explicit formats"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'json'

            def result = controller.showWithFormats(book.id)

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'application/json'
            response.json.title == 'The Stand'
    }

    void "Test that the respond method produces the correct model for a domain instance and content type is HTML"() {
        given:"A book instance"
        def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
        webRequest.actionName = 'showWithModel'
        controller.showWithModel(book)
        def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
        modelAndView != null
        modelAndView instanceof ModelAndView
        modelAndView.model == [book: book, extra: true]
        modelAndView.viewName == 'showWithModel'
    }

    void "Test that the respond method produces errors HTML for a domain instance that has errors and a content type of HTML"() {
        given:"A book instance"
        def book = new Book(title: "")
        book.validate()

        when:"The respond method is used to render a response"
        webRequest.actionName = 'showWithModel'
        controller.showWithModel(book)
        def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
        modelAndView != null
        modelAndView instanceof ModelAndView
        modelAndView.model == [book: book, extra: true]
        modelAndView.viewName == 'showWithModel'
    }
    
    @Issue('GRAILS-10683')
    void "Test responding to a REST call with an empty collection"() {
 
        when:
        response.format = 'json'
        controller.index()
 
        then:
        response.status == 200
        response.contentAsString == '[]'
    }
    
    @Issue('GRAILS-10683')
    @Ignore
    void "Test responding to an HTML call with an empty collection"() {
        when:
        response.format = 'html'
        controller.index()
 
        then:
        response.status == 200
    }
}

@Artefact("Controller")
class BookController {
    def show(Book b) {
        respond b
    }

    def showWithModel(Book b) {
        respond b, model: [extra: true]
    }

    def index() {
        respond Book.list()
    }

    def showWithFormats(Long id) {
        respond Book.get(id), formats:['json', 'html']
    }
}
@Entity
class Book {
    String title

    static constraints = {
        title blank:false
    }
}
