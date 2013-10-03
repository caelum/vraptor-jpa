/***
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.caelum.vraptor.util.jpa;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import br.com.caelum.vraptor.AroundCall;
import br.com.caelum.vraptor.Intercepts;
import br.com.caelum.vraptor.Validator;
import br.com.caelum.vraptor.http.MutableResponse;
import br.com.caelum.vraptor.interceptor.SimpleInterceptorStack;

/**
 * An interceptor that manages Entity Manager Transaction. All requests are intercepted
 * and a transaction is created before execution. If the request has no erros, the transaction
 * will commited, or a rollback occurs otherwise.
 * 
 * @author Lucas Cavalcanti
 */
@Intercepts
public class JPATransactionInterceptor {

	private final EntityManager manager;
	private final Validator validator;
	private final MutableResponse response;

	/**
	 * @deprecated CDI eyes only.
	 */
	protected JPATransactionInterceptor() {
		this(null, null, null);
	}
	
	@Inject
	public JPATransactionInterceptor(EntityManager manager, Validator validator, MutableResponse response) {
		this.manager = manager;
		this.validator = validator;
		this.response = response;
	}

	@AroundCall
	public void intercept(SimpleInterceptorStack stack) {
		
		addRedirectListener();
		
		EntityTransaction transaction = null;
		try {
			transaction = manager.getTransaction();
			transaction.begin();
			
			stack.next();
			
			commit(transaction);
			
		} finally {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
		}
	}
	
	private void commit(EntityTransaction transaction) {
		if (!validator.hasErrors() && transaction.isActive()) {
			transaction.commit();
		}
	}

	/**
	 * We force the commit before the redirect, this way we can abort the
	 * redirect if a database error occurs.
	 */
	private void addRedirectListener() {
		response.addRedirectListener(new MutableResponse.RedirectListener() {
			@Override
			public void beforeRedirect() {
				commit(manager.getTransaction());
			}
		});
	}
}
