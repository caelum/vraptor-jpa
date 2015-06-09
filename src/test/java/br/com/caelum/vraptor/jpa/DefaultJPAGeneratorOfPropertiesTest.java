package br.com.caelum.vraptor.jpa;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultJPAGeneratorOfPropertiesTest {
	private @Mock Properties propertiesOfJPAConnection;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldReturnCreatedInstance() {
		DefaultJPAGeneratorOfProperties defaultJPAGenerator = new DefaultJPAGeneratorOfProperties();
		assertEquals(true, defaultJPAGenerator.getPropertiesOfJPAConnection().isEmpty());
	}
}
