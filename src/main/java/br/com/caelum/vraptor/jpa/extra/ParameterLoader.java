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

import java.io.Serializable;

import javax.enterprise.event.Observes;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.servlet.http.HttpServletRequest;

import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.controller.ControllerMethod;
import br.com.caelum.vraptor.converter.Converter;
import br.com.caelum.vraptor.core.Converters;
import br.com.caelum.vraptor.events.MethodReady;
import br.com.caelum.vraptor.http.Parameter;
import br.com.caelum.vraptor.http.ParameterNameProvider;
import br.com.caelum.vraptor.view.FlashScope;

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

	public void load(@Observes MethodReady event) {
		ControllerMethod method = event.getControllerMethod();
		Object[] args = flash.consumeParameters(method);
		Parameter[] parameters = provider.parametersFor(method.getMethod());

		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			if (parameter.isAnnotationPresent(Load.class)) {
				Object loaded = load(parameter.getName(), parameter.getType());

				if (loaded == null) {
					result.notFound();
					return;
				}

				if (args != null) {
					args[i] = loaded;
				} else {
					request.setAttribute(parameter.getName(), loaded);
				}
			}
		}
		flash.includeParameters(method, args);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> Object load(String name, Class type) {
		final SingularAttribute<?, ?> idProperty = getIdProperty(type);

		final String parameter = request.getParameter(name + "." + idProperty.getName());
		if (parameter == null) {
			return null;
		}

		Converter<?> converter = converters.to(idProperty.getType().getJavaType());
		checkArgument(converter != null, "Entity %s id type %s must have a converter", type.getSimpleName(), idProperty.getType());

		Serializable id = (Serializable) converter.convert(parameter, type);
		return em.find(type, id);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> SingularAttribute<?, ?> getIdProperty(final Class type) {
		IdentifiableType entity = em.getMetamodel().entity(type);

		Type<?> idType = entity.getIdType();
		checkArgument(idType != null, "Entity %s must have an id property for @Load.", type.getSimpleName());

		if (hasSupertype(entity)) {
			entity = entity.getSupertype();
		}

		return entity.getDeclaredId(idType.getJavaType());
	}

	private <T> boolean hasSupertype(IdentifiableType<? super T> entity) {
		return entity.getSupertype() != null;
	}
}
