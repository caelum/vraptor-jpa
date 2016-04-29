package br.com.caelum.vraptor.jpa;

import java.util.Properties;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.interceptor.Interceptor;

/**
 * An Default implementation of {@link JPAGeneratorOfProperties} to inject if neither
 * other generator was injected in {@link EntityManagerFactoryCreator}
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class DefaultJPAGeneratorOfProperties implements JPAGeneratorOfProperties{
	
	@Produces
	public Properties getPropertiesOfJPAConnection() {
		return new Properties();
	}
}
