package org.codehaus.groovy.grails.web.controllers;

public interface ControllerExceptionHandlerMetaData {
    Class<? extends Exception> getExceptionType();
    String getMethodName();
}
