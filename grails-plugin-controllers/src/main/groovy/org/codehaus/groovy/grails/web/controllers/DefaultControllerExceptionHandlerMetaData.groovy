package org.codehaus.groovy.grails.web.controllers

import groovy.transform.Immutable

@Immutable(knownImmutableClasses = [Class])
class DefaultControllerExceptionHandlerMetaData implements ControllerExceptionHandlerMetaData {
    String methodName
    Class<? extends Exception> exceptionType
}
