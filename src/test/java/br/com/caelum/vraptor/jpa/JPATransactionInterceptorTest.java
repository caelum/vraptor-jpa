package br.com.caelum.vraptor.jpa;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import br.com.caelum.vraptor.controller.ControllerMethod;
import br.com.caelum.vraptor.http.MutableResponse;
import br.com.caelum.vraptor.interceptor.SimpleInterceptorStack;
import br.com.caelum.vraptor.jpa.event.AfterCommit;
import br.com.caelum.vraptor.jpa.event.AfterRollback;
import br.com.caelum.vraptor.jpa.event.BeforeCommit;
import br.com.caelum.vraptor.validator.Validator;

public class JPATransactionInterceptorTest {

	@Mock private BeanManager beanManager;
    @Mock private EntityManager entityManager;
    @Mock private SimpleInterceptorStack stack;
    @Mock private ControllerMethod method;
    @Mock private EntityTransaction transaction;
	@Mock private Validator validator;
	@Mock private MutableResponse response;
	
	private JPAInterceptor interceptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        interceptor = new JPATransactionInterceptor(beanManager, entityManager, validator, response);

        // Returns false when transaction.isActive() is called after committing the transaction.
        doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				when(transaction.isActive()).thenReturn(false);
				return null;
			}
		}).when(transaction).commit();
    }

    @Test
    public void shouldStartAndCommitTransaction() throws Exception {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        when(validator.hasErrors()).thenReturn(false);

        interceptor.intercept(stack);

        InOrder callOrder = inOrder(beanManager, entityManager, transaction, stack);
        callOrder.verify(entityManager).getTransaction();
        callOrder.verify(transaction).begin();
        callOrder.verify(stack).next();
        callOrder.verify(beanManager).fireEvent(isA(BeforeCommit.class));
        callOrder.verify(transaction).commit();
        callOrder.verify(beanManager).fireEvent(isA(AfterCommit.class));

        verify(beanManager, never()).fireEvent(isA(AfterRollback.class));
    }

    @Test
    public void shouldRollbackTransactionIfStillActiveWhenExecutionFinishes() throws Exception {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        when(validator.hasErrors()).thenReturn(true);

        interceptor.intercept(stack);

        verify(transaction).rollback();

        verify(beanManager).fireEvent(isA(BeforeCommit.class));
        verify(beanManager, never()).fireEvent(isA(AfterCommit.class));
        verify(beanManager).fireEvent(isA(AfterRollback.class));
    }

    @Test
    public void shouldRollbackIfValidatorHasErrors() {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        when(validator.hasErrors()).thenReturn(true);

        interceptor.intercept(stack);

        verify(transaction).rollback();

        verify(beanManager).fireEvent(isA(BeforeCommit.class));
        verify(beanManager, never()).fireEvent(isA(AfterCommit.class));
        verify(beanManager).fireEvent(isA(AfterRollback.class));
    }
    
    @Test
    public void shouldCommitIfValidatorHasNoErrors() {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        when(validator.hasErrors()).thenReturn(false);

        interceptor.intercept(stack);

        verify(transaction).commit();

        verify(beanManager).fireEvent(isA(BeforeCommit.class));
        verify(beanManager).fireEvent(isA(AfterCommit.class));
        verify(beanManager, never()).fireEvent(isA(AfterRollback.class));
    }
    
    @Test
    public void doNothingIfHasNoActiveTransation() {

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(false);

        interceptor.intercept(stack);

        verify(transaction, never()).rollback();
        verify(transaction, never()).commit();

        verify(beanManager, never()).fireEvent(isA(BeforeCommit.class));
        verify(beanManager, never()).fireEvent(isA(AfterCommit.class));
        verify(beanManager, never()).fireEvent(isA(AfterRollback.class));
    }
    
    @Test
	public void shouldConfigureARedirectListenerWhenTransactionIsCommited() {

		when(entityManager.getTransaction()).thenReturn(transaction);
		when(transaction.isActive()).thenReturn(true);
        when(validator.hasErrors()).thenReturn(false);

		interceptor.intercept(stack);

		verify(response).addRedirectListener(any(MutableResponse.RedirectListener.class));

        verify(beanManager).fireEvent(isA(BeforeCommit.class));
        verify(beanManager).fireEvent(isA(AfterCommit.class));
        verify(beanManager, never()).fireEvent(isA(AfterRollback.class));
	}
    
    @Test
    public void shouldConfigureARedirectListenerWhenTransactionIsRolledback() {
    	
    	when(entityManager.getTransaction()).thenReturn(transaction);
    	when(transaction.isActive()).thenReturn(true);
    	when(validator.hasErrors()).thenReturn(true);
    	
    	interceptor.intercept(stack);
    	
    	verify(response).addRedirectListener(any(MutableResponse.RedirectListener.class));
    	
    	verify(beanManager).fireEvent(isA(BeforeCommit.class));
    	verify(beanManager, never()).fireEvent(isA(AfterCommit.class));
    	verify(beanManager).fireEvent(isA(AfterRollback.class));
    }
}
