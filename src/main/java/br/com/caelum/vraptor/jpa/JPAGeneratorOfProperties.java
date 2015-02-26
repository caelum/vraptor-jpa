package br.com.caelum.vraptor.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * 
 * Will work like superclass to configure the JPA programmatically.
 * 
 * @author Phelipe Wener
 */
public class JPAGeneratorOfProperties {
	
	@ApplicationScoped
	@Produces
	public Map<String, String> getPropertiesOfJPAConnection(){
		return new HashMap<String, String>();
	}
}
