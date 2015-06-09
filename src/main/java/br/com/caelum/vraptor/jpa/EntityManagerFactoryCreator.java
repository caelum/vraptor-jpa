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

import java.util.Properties;

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
	@Inject
	private Environment environment;
	@Inject
	private Properties propertiesOfJPAConnection;

	
	/**
	 * Produces EntityManagerFactory.  For default  is created an connect 
	 * based on configurations of the persistence.xml. 
	 * However, if database url property  is null in persistence.xml,  
	 * the connection will created programmatically, overriding the default setting.
	 * 
	 * @return the EntityManagerFactory that will create all Entity managers
	 */
	@ApplicationScoped
	@Produces
	public EntityManagerFactory getEntityManagerFactory() {
		EntityManagerFactory factory;
		
		String persistenceUnit = environment.get("br.com.caelum.vraptor.jpa.persistenceunit", "default");
		// Try create factory from persistence.xml
		factory = Persistence.createEntityManagerFactory(persistenceUnit);
		
		// If factory is not valid, try create an connection based on propertiesOfJPAConnection
		if(!factoryIsValid(factory)){
			factory.close();
			factory = Persistence.createEntityManagerFactory("default", propertiesOfJPAConnection);
		}
		return factory;			
	}
	
	/**
	 * Checks whether the factory is valid through your params
	 * 
	 * @param factory to be analysed
	 * @return true if is valid
	 */
	private boolean factoryIsValid(EntityManagerFactory factory){
		return factory.getProperties().containsKey("hibernate.connection.url");
	}

	public void destroy(@Disposes EntityManagerFactory factory) {
		if (factory.isOpen()) {
			factory.close();
		}
	}
}
