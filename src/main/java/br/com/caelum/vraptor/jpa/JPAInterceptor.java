package br.com.caelum.vraptor.jpa;

import br.com.caelum.vraptor.interceptor.SimpleInterceptorStack;

public interface JPAInterceptor {

	void intercept(SimpleInterceptorStack stack);

}