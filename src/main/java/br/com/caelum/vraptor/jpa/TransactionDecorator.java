package br.com.caelum.vraptor.jpa;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.transaction.Transactional;

import br.com.caelum.vraptor.controller.ControllerMethod;
import br.com.caelum.vraptor.interceptor.SimpleInterceptorStack;

@Decorator 
public class TransactionDecorator implements JPAInterceptor {

	@Inject @Delegate @Any
	private JPAInterceptor delegate;
	
	@Inject
	private ControllerMethod method;

	@Override
	public void intercept(SimpleInterceptorStack stack) {
		
		if(isTransactional()) {
			delegate.intercept(stack);
			return;
		}
		stack.next();
	}

	private boolean isTransactional() {
		return method.containsAnnotation(Transactional.class);
	}
}