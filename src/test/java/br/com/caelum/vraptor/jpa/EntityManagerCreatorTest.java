package br.com.caelum.vraptor.jpa;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EntityManagerCreatorTest {

    private @Mock EntityManagerFactory factory;
    private @Mock EntityManager entityManager;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void shouldReturnCreatedInstance() {
        when(factory.createEntityManager()).thenReturn(entityManager);
        
        EntityManagerCreator creator = new EntityManagerCreator(factory);
        
        assertEquals(entityManager, creator.getEntityManager());
    }
    
    @Test
    public void shouldCloseSessionOnDestroy() {
        when(factory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.isOpen()).thenReturn(true);
        
        EntityManagerCreator creator = new EntityManagerCreator(factory);
        creator.destroy(creator.getEntityManager());
        
        verify(entityManager).close();
    }
}
