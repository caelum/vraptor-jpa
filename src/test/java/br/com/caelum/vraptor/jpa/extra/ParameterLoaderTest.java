package br.com.caelum.vraptor.jpa.extra;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.MappedSuperclassType;
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
import br.com.caelum.vraptor.core.MethodInfo;
import br.com.caelum.vraptor.http.Parameter;
import br.com.caelum.vraptor.http.ParameterNameProvider;
import br.com.caelum.vraptor.interceptor.SimpleInterceptorStack;

public class ParameterLoaderTest {

	private @Mock EntityManager em;
	private @Mock HttpServletRequest request;
	private @Mock ParameterNameProvider provider;
	private @Mock Result result;
	private @Mock Converters converters;
	private @Mock MethodInfo methodInfo;

	private @Mock Metamodel metamodel;
	private @Mock EntityType entityType;
	private @Mock Type type;
	private @Mock SingularAttribute attribute;
	private @Mock SimpleInterceptorStack stack;
	private @Mock MappedSuperclassType mappedSuperclassType;

	private ControllerMethod method;
	private ControllerMethod methodOtherIdName;
	private ControllerMethod other;
	private ControllerMethod noId;
	private ControllerMethod methodWithoutLoad;
	private ControllerMethod methodMappedSuperClass;
	private ControllerMethod methodChild;
	private ControllerMethod methodSon;
	private ControllerMethod methodGrandSon;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		method = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("method", Entity.class));
		methodWithoutLoad = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("methodWithoutLoad"));
		methodOtherIdName = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("methodOtherIdName", EntityOtherIdName.class));
		other = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("other", OtherEntity.class, String.class));
		noId = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("noId", NoIdEntity.class));
		methodMappedSuperClass = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("methodMappedSuperClass", MappedSuperClass.class));
		methodChild = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("methodChild", Child.class));
		methodSon = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("methodSon", Son.class));
		methodGrandSon = DefaultControllerMethod.instanceFor(Resource.class, Resource.class.getMethod("methodGrandSon", GrandSon.class));

		when(converters.to(Long.class)).thenReturn(new LongConverter());
		when(converters.to(String.class)).thenReturn(new StringConverter());

		when(em.getMetamodel()).thenReturn(metamodel);
		when(metamodel.entity(any(Class.class))).thenReturn(entityType);
		when(entityType.getIdType()).thenReturn(type);
		when(attribute.getType()).thenReturn(type);
	}

	public ParameterLoader buildInterceptorUsing(ControllerMethod method) {
		return new ParameterLoader(em, request, provider, result, converters, methodInfo, method);
	}

	@Test
	public void shouldAcceptsIfHasLoadAnnotation() {
		assertTrue(buildInterceptorUsing(method).containsLoadAnnotation());
	}

	@Test
	public void shouldNotAcceptIfHasNoLoadAnnotation() {
		assertFalse(buildInterceptorUsing(methodWithoutLoad).containsLoadAnnotation());
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void shouldLoadEntityUsingId() throws Exception {
		Parameter parameter = new Parameter(0, "entity", method.getMethod());
		when(provider.parametersFor(method.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("entity.id")).thenReturn("123");

		Entity expectedEntity = new Entity();
		when(em.find(Entity.class, 123L)).thenReturn(expectedEntity);
		when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
		when(attribute.getName()).thenReturn("id");
		when(type.getJavaType()).thenReturn(Long.class);

		buildInterceptorUsing(method).intercept(stack);

		verify(request).setAttribute("entity", expectedEntity);
		verify(stack, times(1)).next();
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

		buildInterceptorUsing(methodOtherIdName).intercept(stack);

		verify(request).setAttribute("entity", expectedEntity);
		verify(stack, times(1)).next();
	}

	@Test
	public void shouldLoadEntityUsingIdOfAnyType() throws Exception {
		Parameter parameter0 = new Parameter(0, "entity", other.getMethod());
		when(provider.parametersFor(other.getMethod())).thenReturn(new Parameter[] { parameter0 });
		when(request.getParameter("entity.id")).thenReturn("123");

		OtherEntity expectedEntity = new OtherEntity();
		when(em.find(OtherEntity.class, "123")).thenReturn(expectedEntity);
		when(entityType.getDeclaredId(String.class)).thenReturn(attribute);
		when(attribute.getName()).thenReturn("id");
		when(type.getJavaType()).thenReturn(String.class);

		buildInterceptorUsing(other).intercept(stack);

		verify(request).setAttribute("entity", expectedEntity);
		verify(stack, times(1)).next();
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void shouldLoadMappedSuperClassUsingId() throws Exception {
		Parameter parameter = new Parameter(0, "mappedSuperClass", methodMappedSuperClass.getMethod());
		when(provider.parametersFor(methodMappedSuperClass.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("mappedSuperClass.id")).thenReturn("123");

		MappedSuperClass expectedEntity = new MappedSuperClass();
		when(em.find(MappedSuperClass.class, 123L)).thenReturn(expectedEntity);
		when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
		when(attribute.getName()).thenReturn("id");
		when(type.getJavaType()).thenReturn(Long.class);

		buildInterceptorUsing(methodMappedSuperClass).intercept(stack);

		verify(request).setAttribute("mappedSuperClass", expectedEntity);
		verify(stack, times(1)).next();
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void shouldLoadChildUsingId() throws Exception {
		Parameter parameter = new Parameter(0, "child", methodChild.getMethod());
		when(provider.parametersFor(methodChild.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("child.id")).thenReturn("123");

		Child expectedEntity = new Child();
		when(em.find(Child.class, 123L)).thenReturn(expectedEntity);
		when(entityType.getSupertype()).thenReturn(mappedSuperclassType);
		when(mappedSuperclassType.getDeclaredId(Long.class)).thenReturn(attribute);
		when(attribute.getName()).thenReturn("id");
		when(type.getJavaType()).thenReturn(Long.class);

		buildInterceptorUsing(methodChild).intercept(stack);

		verify(request).setAttribute("child", expectedEntity);
		verify(stack, times(1)).next();
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void shouldLoadSonUsingId() throws Exception {
		Parameter parameter = new Parameter(0, "son", methodSon.getMethod());
		when(provider.parametersFor(methodSon.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("son.id")).thenReturn("123");

		Son expectedEntity = new Son();
		when(em.find(Son.class, 123L)).thenReturn(expectedEntity);
		when(entityType.getSupertype()).thenReturn(mappedSuperclassType);
		when(mappedSuperclassType.getDeclaredId(Long.class)).thenReturn(attribute);
		when(attribute.getName()).thenReturn("id");
		when(type.getJavaType()).thenReturn(Long.class);

		buildInterceptorUsing(methodSon).intercept(stack);

		verify(request).setAttribute("son", expectedEntity);
		verify(stack, times(1)).next();
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void shouldLoadGrandSonUsingId() throws Exception {
		Parameter parameter = new Parameter(0, "grandSon", methodGrandSon.getMethod());
		when(provider.parametersFor(methodGrandSon.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("grandSon.id")).thenReturn("123");

		GrandSon expectedEntity = new GrandSon();
		when(em.find(GrandSon.class, 123L)).thenReturn(expectedEntity);
		when(entityType.getSupertype()).thenReturn(mappedSuperclassType);
		when(mappedSuperclassType.getDeclaredId(Long.class)).thenReturn(attribute);
		when(attribute.getName()).thenReturn("id");
		when(type.getJavaType()).thenReturn(Long.class);

		buildInterceptorUsing(methodGrandSon).intercept(stack);

		verify(request).setAttribute("grandSon", expectedEntity);
		verify(stack, times(1)).next();
	}

	@Test
	public void shouldOverrideMethodInfoArgsIfAny() throws Exception {
		Parameter parameter = new Parameter(0, "entity", method.getMethod());
		when(provider.parametersFor(method.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("entity.id")).thenReturn("123");

		Object[] args = {new Entity()};
		when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
		when(attribute.getName()).thenReturn("id");
		when(type.getJavaType()).thenReturn(Long.class);
		when(methodInfo.getParametersValues()).thenReturn(args);
		Entity expectedEntity = new Entity();
		when(em.find(Entity.class, 123l)).thenReturn(expectedEntity);

		buildInterceptorUsing(method).intercept(stack);

		assertThat(args[0], is((Object) expectedEntity));
		verify(methodInfo).setParameter(0, args[0]);
		verify(stack, times(1)).next();
	}

	@Test
	public void shouldSend404WhenNoIdIsSet() throws Exception {
		Parameter parameter0 = new Parameter(0, "entity", methodOtherIdName.getMethod());
		when(provider.parametersFor(method.getMethod())).thenReturn(new Parameter[]{parameter0});
		when(request.getParameter("entity.id")).thenReturn(null);
		when(entityType.getDeclaredId(Long.class)).thenReturn(attribute);
		when(attribute.getName()).thenReturn("id");
		when(type.getJavaType()).thenReturn(Long.class);

		buildInterceptorUsing(method).intercept(stack);

		verify(request, never()).setAttribute(eq("entity"), any());
		verify(result).notFound();
		verify(stack, never()).next();
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

		buildInterceptorUsing(method).intercept(stack);

		verify(request, never()).setAttribute(eq("entity"), any());
		verify(result).notFound();
		verify(stack, never()).next();
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentIfEntityDoesntHaveId() throws Exception {
		Parameter parameter = new Parameter(0, "entity", methodOtherIdName.getMethod());
		when(provider.parametersFor(noId.getMethod())).thenReturn(new Parameter[]{parameter});
		when(request.getParameter("entity.id")).thenReturn("123");
		when(entityType.getIdType()).thenReturn(null);
		fail().when(request).setAttribute(eq("entity"), any());
		fail().when(result).notFound();
		fail().when(stack).next();

		buildInterceptorUsing(noId).intercept(stack);
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
		fail().when(stack).next();

		buildInterceptorUsing(method).intercept(stack);
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

	@MappedSuperclass static class MappedSuperClass {
		@Id Long id;
	}
	static class Child extends MappedSuperClass {
	}

	static class Son extends Entity {
	}
	static class GrandSon extends Son {
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
		public void methodMappedSuperClass(@Load MappedSuperClass mappedSuperClass) {
		}
		public void methodChild(@Load Child child) {
		}
		public void methodSon(@Load Son son) {
		}
		public void methodGrandSon(@Load GrandSon grandSon) {
		}
		public void methodWithoutLoad() {
		}
	}

	private Stubber fail() {
		return doThrow(new AssertionError());
	}
}