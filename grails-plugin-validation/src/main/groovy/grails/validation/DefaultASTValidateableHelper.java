/*
 * Copyright 2011 the original author or authors.
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
package grails.validation;

import grails.util.Holders;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator;
import org.codehaus.groovy.grails.web.plugins.support.ValidationSupport;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class DefaultASTValidateableHelper implements ASTValidateableHelper{


    public void injectValidateableCode(ClassNode classNode) {
        FieldNode field = classNode.getField("$api_validate");
        if (field == null || !field.getDeclaringClass().equals(classNode)) {
            classNode.addField("$api_validate",
                Modifier.PRIVATE, new ClassNode(ValidationApi.class),
                new ConstructorCallExpression(new ClassNode(ValidationApi.class), new ArgumentListExpression()));
        }
        GrailsASTUtils.addDelegateInstanceMethods( classNode, new ClassNode(ValidationApi.class), new VariableExpression("$api_validate"), null);
    }
}

class ValidationApi {
    private ValidationErrors errors;
    private Map<String, ?> constraints;
    
    @GrailsASTUtils.ForceMethodOverride
    public Map<String, ?> getConstraints(Object instance) {
        if(constraints == null) {
            BeanFactory webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(Holders.getServletContext());
            ConstraintsEvaluator evaluator = webApplicationContext.getBean(ConstraintsEvaluator.BEAN_NAME, ConstraintsEvaluator.class);
            constraints = evaluator.evaluate(instance.getClass());
        }
        return constraints;
    }
    public boolean validate(Object instance) {
        return validate(instance, null);
    }
    
    public boolean validate(Object instance, List<String> args) {
        return ValidationSupport.validateInstance(instance, args);
    }
    
    public ValidationErrors getErrors(Object instance) {
        if(errors == null) {
            errors = new ValidationErrors(instance);
        }
        return errors;
    }

    public void setErrors(Object instance, ValidationErrors errors) {
        this.errors = errors;
    }
    
    public boolean hasErrors(Object instance) {
        boolean hasErrors = false;
        if(errors != null) {
            return errors.hasErrors();
        }
        return hasErrors;
    }
    
    public void clearErrors(Object instance) {
        errors = null;
    }
}
