/***
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package br.com.caelum.vraptor.jpa.extra;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.isEmpty;
import static java.util.Arrays.asList;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import javax.enterprise.event.Observes;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.servlet.http.HttpServletRequest;

import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.controller.ControllerMethod;
import br.com.caelum.vraptor.core.Converters;
import br.com.caelum.vraptor.events.ControllerMethodDiscovered;
import br.com.caelum.vraptor.http.Parameter;
import br.com.caelum.vraptor.http.ParameterNameProvider;
import br.com.caelum.vraptor.view.FlashScope;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Observer that loads given entity from the database.
 *
 * @author Lucas Cavalcanti
 * @author Cecilia Fernandes
 * @author Otávio Scherer Garcia
 * @since 3.3.2
 */
public class ParameterLoader {

	private final EntityManager em;
	private final HttpServletRequest request;
	private final ParameterNameProvider provider;
	private final Result result;
	private final Converters converters;
	private final FlashScope flash;

	public ParameterLoader(EntityManager em, HttpServletRequest request, ParameterNameProvider provider,
			Result result, Converters converters, FlashScope flash) {
		this.em = em;
		this.request = request;
		this.provider = provider;
		this.result = result;
		this.converters = converters;
		this.flash = flash;
	}

	public boolean containsLoadAnnotation(ControllerMethod method) {
		return any(asList(method.getMethod().getParameterAnnotations()), hasAnnotation(Load.class));
	}

	public void load(@Observes ControllerMethodDiscovered event){
		
		ControllerMethod method = event.getControllerMethod();
		
		if (!containsLoadAnnotation(method)) return;
		
		Annotation[][] annotations = method.getMethod().getParameterAnnotations();

		Parameter[] parameters = provider.parametersFor(method.getMethod());
		Class<?>[] types = method.getMethod().getParameterTypes();

		Object[] args = flash.consumeParameters(method);

		for (int i = 0; i < parameters.length; i++) {
			if (hasLoadAnnotation(annotations[i])) {
				Object loaded = load(parameters[i].getName(), types[i]);

				if (loaded == null) {
					result.notFound();
					return;
				}

				if (args != null) {
					args[i] = loaded;
				} else {
					request.setAttribute(parameters[i].getName(), loaded);
				}
			}
		}
		flash.includeParameters(method, args);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> Object load(String name, Class type) {
		EntityType<T> entity = em.getMetamodel().entity(type);
		
		Type<?> idType = entity.getIdType();
		checkArgument(idType != null, "Entity %s must have an id property for @Load.", type.getSimpleName());
		
		SingularAttribute idProperty = entity.getDeclaredId(idType.getJavaType());
		String parameter = request.getParameter(name + "." + idProperty.getName());
		if (parameter == null) {
			return null;
		}
		
		br.com.caelum.vraptor.converter.Converter<?> converter = converters.to(idType.getJavaType());
		checkArgument(converter != null, "Entity %s id type %s must have a converter", type.getSimpleName(), idType);

		Serializable id = (Serializable) converter.convert(parameter, type);
		return em.find(type, id);
	}

	private boolean hasLoadAnnotation(Annotation[] annotation) {
		return !isEmpty(Iterables.filter(asList(annotation), Load.class));
	}

	public static Predicate<Annotation[]> hasAnnotation(final Class<?> annotation) {
		return new Predicate<Annotation[]>() {
			public boolean apply(Annotation[] param) {
				return any(asList(param), instanceOf(annotation));
			}
		};
	}
}