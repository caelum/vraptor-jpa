package br.com.caelum.vraptor.jpa;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManagerFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EntityManagerFactoryCreatorTest {
    
    private @Mock EntityManagerFactory factory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldReturnCreatedInstance() {
        EntityManagerFactoryCreator creator = new EntityManagerFactoryCreator();
        EntityManagerFactoryCreator spy = spy(creator);
        
        doReturn(factory).when(spy).getEntityManagerFactory();
        
        spy.getEntityManagerFactory();
        
        assertEquals(factory, spy.getEntityManagerFactory());
    }

    @Test
    public void shouldCloseFactoryOnDestroy() {
        EntityManagerFactoryCreator creator = new EntityManagerFactoryCreator();
        EntityManagerFactoryCreator spy = spy(creator);
        
        doReturn(factory).when(spy).getEntityManagerFactory();
        when(factory.isOpen()).thenReturn(true);
        
        spy.destroy(factory);
        
        verify(factory).close();
    }
}
