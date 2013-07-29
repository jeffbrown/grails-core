package org.codehaus.groovy.grails.web.controllers

import grails.artefact.Artefact
import grails.test.mixin.TestFor

import java.sql.BatchUpdateException
import java.sql.SQLException

import spock.lang.Specification

@TestFor(ErrorHandlersController)
class ControllerErrorHandlerSpec extends Specification {
    
    void 'Test getExceptionHandlerMethodFor method'() {
        when:
        def method = controller.getExceptionHandlerMethodFor(IllegalArgumentException)
        
        then:
        method == null
        
        when:
        method = controller.getExceptionHandlerMethodFor(SQLException)
        def returnValue = method.invoke(controller, new SQLException())
        
        then:
        1 == returnValue
        
        when:
        method = controller.getExceptionHandlerMethodFor(BatchUpdateException)
        returnValue = method.invoke(controller, new BatchUpdateException())
        
        then:
        2 == returnValue
        
        when:
        method = controller.getExceptionHandlerMethodFor(NumberFormatException)
        returnValue = method.invoke(controller, new NumberFormatException())
        
        then:
        3 == returnValue
        
        when:
        controller.getExceptionHandlerMethodFor(MyCommand)
        
        then:
        thrown IllegalArgumentException
    }
}

@Artefact('Controller')
class ErrorHandlersController {
    def someAction() {}
    def someActionWithCommandObject(MyCommand command) {}
    
    def handleSQLException(SQLException e) {
        1
    }
    
    // BatchUpdateException extends SQLException
    def handleSQLException(BatchUpdateException e) {
        2
    }
    
    def handleSomeNumberFormatException(NumberFormatException e) {
        3
    }
        
}

class MyCommand {
    String name
    static constraints = {
        name size: 5..15
    }
}
