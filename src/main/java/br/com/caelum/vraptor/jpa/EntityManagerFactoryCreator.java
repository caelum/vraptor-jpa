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
package br.com.caelum.vraptor.jpa;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import br.com.caelum.vraptor.environment.Environment;

/**
 * An {@link EntityManager} producer, that creates an instance for each request.
 * 
 * @author Lucas Cavalcanti
 * @author Otávio Garcia
 */
public class EntityManagerFactoryCreator {

	private Environment environment;
	private final Map<String, String> propertiesOfJPAConnection;
	
	/**
	 * @deprecated CDI eyes only.
	 */
	public EntityManagerFactoryCreator() {
		this(null, null);
	}

	@Inject
	public EntityManagerFactoryCreator(Environment environment, Map<String, String> propertiesOfJPAConnection) {
		this.environment = environment;
		this.propertiesOfJPAConnection = propertiesOfJPAConnection;
	}
	
	/**
	 * Produces the factory that will create EntityManager. If none
	 * propertiesOfJPAConnection is inserted, will be created an default connect
	 * based in configurations of the persistence.xml. However, if a setting is
	 * entered, the connection is created programmatically, overriding the
	 * default settings.
	 * 
	 * @return the EntityManagerFactory that will create all Entity managers
	 */
	@ApplicationScoped
	@Produces
	public EntityManagerFactory getEntityManagerFactory() {
		if (propertiesOfJPAConnection.isEmpty()) {
			String persistenceUnit = environment.get("br.com.caelum.vraptor.jpa.persistenceunit", "default");
			return Persistence.createEntityManagerFactory(persistenceUnit);
		} else {
			return Persistence.createEntityManagerFactory("default", propertiesOfJPAConnection);
		}
	}

	public void destroy(@Disposes EntityManagerFactory factory) {
		if (factory.isOpen()) {
			factory.close();
		}
	}
}
