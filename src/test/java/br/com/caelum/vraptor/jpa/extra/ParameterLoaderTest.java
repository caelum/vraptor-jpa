package br.com.caelum.vraptor.jpa.extra;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;

import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.controller.ControllerMethod;
import br.com.caelum.vraptor.controller.DefaultControllerMethod;
import br.com.caelum.vraptor.converter.LongConverter;
import br.com.caelum.vraptor.converter.StringConverter;
import br.com.caelum.vraptor.core.Converters;
import br.com.caelum.vraptor.events.MethodReady;
import br.com.caelum.vraptor.http.Parameter;
import br.com.caelum.vraptor.http.ParameterNameProvider;
import br.com.caelum.vraptor.view.FlashScope;

public class ParameterLoaderTest {

	private @Mock EntityManager em;
	private @Mock HttpServletRequest request;
	private @Mock ParameterNameProvider provider;
	private @Mock Result result;
	private @Mock Converters converters;
    private @Mock FlashScope flash;
    
    private @Mock Metamodel metamodel;
    private @Mock EntityType entityType;
    private @Mock Type type;
    private @Mock SingularAttribute attribute;

    private ParameterLoader observer;
    private ControllerMethod method;
    private ControllerMethod methodOtherIdName;
    private ControllerMethod other;
    private ControllerMethod noId;
    private ControllerMethod methodWithoutLoad;
    
    @Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		observer = new ParameterLoader(em, request, provider, result, converters, flash);
        method = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("method", Entity.class));
        methodWithoutLoad = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("methodWithoutLoad"));
        methodOtherIdName = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("methodOtherIdName", EntityOtherIdName.class));
        other = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("other", OtherEntity.class, String.class));
        noId = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("noId", NoIdEntity.class));

		when(converters.to(Long.class)).thenReturn(new LongConverter());
		when(converters.to(String.class)).thenReturn(new StringConverter());
		
        when(em.getMetamodel()).thenReturn(metamodel);
        when(metamodel.entity(any(Class.class))).thenReturn(entityType);
        when(entityType.getIdType()).thenReturn(type);
	}
    
    @Test
    public void shouldAcceptsIfHasLoadAnnotation() {
        assertTrue(observer.containsLoadAnnotation(method));
    }

    @Test
    public void shouldNotAcceptIfHasNoLoadAnnotation() {
        assertFalse(observer.containsLoadAnnotation(methodWithoutLoad));
    }
    
    @Test
    @SuppressWarnings({ "unchecked" })
	public void shouldLoadEntityUsingId() throws Exception {
    	Parameter parameter = new Parameter(0, "entity", methodOtherIdName.getMethod());
    	when(provider.parametersFor(method.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("entity.id")).thenReturn("123");
		Entity expectedEntity = new Entity();
		when(em.find(Entity.class, 123L)).thenReturn(expectedEntity);
        when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
        when(attribute.getName()).thenReturn("id");
        when(type.getJavaType()).thenReturn(Long.class);
		observer.load(new MethodReady(method));
		verify(request).setAttribute("entity", expectedEntity);
	}

    @Test
    public void shouldLoadEntityUsingOtherIdName() throws Exception {
    	Parameter parameter = new Parameter(0, "entity", methodOtherIdName.getMethod());
    	when(provider.parametersFor(methodOtherIdName.getMethod())).thenReturn(new Parameter[]{parameter});
        when(request.getParameter("entity.otherIdName")).thenReturn("456");
        EntityOtherIdName expectedEntity = new EntityOtherIdName();
        when(em.find(EntityOtherIdName.class, 456L)).thenReturn(expectedEntity);
        when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
        when(attribute.getName()).thenReturn("otherIdName");
        when(type.getJavaType()).thenReturn(Long.class);
		observer.load(new MethodReady(methodOtherIdName));
        verify(request).setAttribute("entity", expectedEntity);
    }
    
	@Test
	public void shouldLoadEntityUsingIdOfAnyType() throws Exception {
    	Parameter parameter0 = new Parameter(0, "entity", methodOtherIdName.getMethod());
    	Parameter parameter1 = new Parameter(0, "ignored", methodOtherIdName.getMethod());
    	when(provider.parametersFor(other.getMethod())).thenReturn(new Parameter[]{parameter0, parameter1});
		when(request.getParameter("entity.id")).thenReturn("123");
		when(request.getParameter("ignored")).thenReturn("bar");
		OtherEntity expectedEntity = new OtherEntity();
		when(em.find(OtherEntity.class, "123")).thenReturn(expectedEntity);
        when(entityType.getDeclaredId(String.class)).thenReturn(attribute);
        when(attribute.getName()).thenReturn("id");
        when(type.getJavaType()).thenReturn(String.class);
		observer.load(new MethodReady(other));
		verify(request).setAttribute("entity", expectedEntity);
	}

	@Test
	public void shouldOverrideFlashScopedArgsIfAny() throws Exception {
    	Parameter parameter = new Parameter(0, "entity", method.getMethod());
    	when(provider.parametersFor(method.getMethod())).thenReturn(new Parameter[]{parameter});
    	when(request.getParameter("entity.id")).thenReturn("123");
        Object[] args = {new Entity()};
        when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
        when(attribute.getName()).thenReturn("id");
        when(type.getJavaType()).thenReturn(Long.class);
        when(flash.consumeParameters(method)).thenReturn(args);
        Entity expectedEntity = new Entity();
		when(em.find(Entity.class, 123l)).thenReturn(expectedEntity);
		observer.load(new MethodReady(method));
		assertThat(args[0], is((Object) expectedEntity));
        verify(flash).includeParameters(method, args);
	}

	@Test
	public void shouldSend404WhenNoIdIsSet() throws Exception {
    	Parameter parameter0 = new Parameter(0, "entity", methodOtherIdName.getMethod());
    	when(provider.parametersFor(method.getMethod())).thenReturn(new Parameter[]{parameter0});
		when(request.getParameter("entity.id")).thenReturn(null);
        when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
        when(attribute.getName()).thenReturn("id");
        when(type.getJavaType()).thenReturn(Long.class);
		observer.load(new MethodReady(method));
		verify(request, never()).setAttribute(eq("entity"), any());
		verify(result).notFound();
	}

	@Test
	public void shouldSend404WhenIdDoesntExist() throws Exception {
    	Parameter parameter = new Parameter(0, "entity", methodOtherIdName.getMethod());
    	when(provider.parametersFor(method.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("entity.id")).thenReturn("123");
		when(em.find(Entity.class, 123l)).thenReturn(null);
        when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
        when(attribute.getName()).thenReturn("id");
        when(type.getJavaType()).thenReturn(Long.class);
		observer.load(new MethodReady(method));
		verify(request, never()).setAttribute(eq("entity"), any());
		verify(result).notFound();
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentIfEntityDoesntHaveId() throws Exception {
		Parameter parameter = new Parameter(0, "entity", methodOtherIdName.getMethod());
    	when(provider.parametersFor(noId.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("entity.id")).thenReturn("123");
        when(entityType.getIdType()).thenReturn(null);
		fail().when(request).setAttribute(eq("entity"), any());
		fail().when(result).notFound();
		observer.load(new MethodReady(noId));
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentIfIdIsNotConvertable() throws Exception {
		Parameter parameter = new Parameter(0, "entity", methodOtherIdName.getMethod());
    	when(provider.parametersFor(method.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("entity.id")).thenReturn("123");
		when(converters.to(Long.class)).thenReturn(null);
        when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
        when(attribute.getName()).thenReturn("id");
        when(type.getJavaType()).thenReturn(Long.class);
		fail().when(request).setAttribute(eq("entity"), any());
		fail().when(result).notFound();
		observer.load(new MethodReady(method));
	}


    static class Entity {
        @Id Long id;
    }
    static class OtherEntity {
        @Id String id;
    }
    static class NoIdEntity {
    }
    
    static class EntityOtherIdName {
        @Id Long otherIdName;
    }
    
    static class Resource {
        public void method(@Load Entity entity) {
        }
        public void other(@Load OtherEntity entity, String ignored) {
        }
        public void noId(@Load NoIdEntity entity) {
        }
        public void methodOtherIdName(@Load EntityOtherIdName entity) {
        }
        public void methodWithoutLoad() {
        }
    }
    
	private Stubber fail() {
		return doThrow(new AssertionError());
	}
}
