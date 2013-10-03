package br.com.caelum.vraptor.util.jpa;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import br.com.caelum.vraptor.Validator;
import br.com.caelum.vraptor.controller.ControllerMethod;
import br.com.caelum.vraptor.http.MutableResponse;
import br.com.caelum.vraptor.interceptor.SimpleInterceptorStack;

public class JPATransactionInterceptorTest {

    @Mock private EntityManager entityManager;
    @Mock private SimpleInterceptorStack stack;
    @Mock private ControllerMethod method;
    @Mock private EntityTransaction transaction;
	@Mock private Validator validator;
	@Mock private MutableResponse response;
	
	private JPATransactionInterceptor interceptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        interceptor = new JPATransactionInterceptor(entityManager, validator, response);
    }

    @Test
    public void shouldStartAndCommitTransaction() throws Exception {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);

        interceptor.intercept(stack);

        InOrder callOrder = inOrder(entityManager, transaction, stack);
        callOrder.verify(entityManager).getTransaction();
        callOrder.verify(transaction).begin();
        callOrder.verify(stack).next();
        callOrder.verify(transaction).commit();
    }

    @Test
    public void shouldRollbackTransactionIfStillActiveWhenExecutionFinishes() throws Exception {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);

        interceptor.intercept(stack);

        verify(transaction).rollback();
    }

    @Test
    public void shouldRollbackIfValidatorHasErrors() {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        when(validator.hasErrors()).thenReturn(true);

        interceptor.intercept(stack);

        verify(transaction).rollback();
    }
    
    @Test
    public void shouldCommitIfValidatorHasNoErrors() {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        when(validator.hasErrors()).thenReturn(false);

        interceptor.intercept(stack);

        verify(transaction).commit();
    }
    
    @Test
    public void doNothingIfHasNoActiveTransation() {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(false);

        interceptor.intercept(stack);

        verify(transaction, never()).rollback();
    }
    
    @Test
	public void shouldConfigureARedirectListener() {

		when(entityManager.getTransaction()).thenReturn(transaction);

		interceptor.intercept(stack);

		verify(response).addRedirectListener(any(MutableResponse.RedirectListener.class));
	}
}
