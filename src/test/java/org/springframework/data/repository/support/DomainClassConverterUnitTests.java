/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.DummyEntityInformation;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.core.support.DummyRepositoryInformation;

/**
 * Unit test for {@link DomainClassConverter}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DomainClassConverterUnitTests {

	static final User USER = new User();

	@SuppressWarnings("rawtypes")
	DomainClassConverter converter;

	TypeDescriptor sourceDescriptor;
	TypeDescriptor targetDescriptor;

	@Mock
	DefaultConversionService service;

	@Before
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setUp() {

		EntityInformation<User, Serializable> information = new DummyEntityInformation<User>(User.class);
		RepositoryInformation repositoryInformation = new DummyRepositoryInformation(UserRepository.class);

		converter = new DomainClassConverter(service);

		sourceDescriptor = TypeDescriptor.valueOf(String.class);
		targetDescriptor = TypeDescriptor.valueOf(User.class);
	}

	@Test
	public void matchFailsIfNoDaoAvailable() throws Exception {

		converter.setApplicationContext(new GenericApplicationContext());
		assertMatches(false);
	}

	@Test
	public void matchesIfConversionInBetweenIsPossible() throws Exception {

		converter.setApplicationContext(initContextWithRepo());

		when(service.canConvert(String.class, Long.class)).thenReturn(true);

		assertMatches(true);
	}

	@Test
	public void matchFailsIfNoIntermediateConversionIsPossible() throws Exception {

		converter.setApplicationContext(initContextWithRepo());

		when(service.canConvert(String.class, Long.class)).thenReturn(false);

		assertMatches(false);
	}

	/**
	 * @see DATACMNS-233
	 */
	public void returnsNullForNullSource() {
		assertThat(converter.convert(null, sourceDescriptor, targetDescriptor), is(nullValue()));
	}

	/**
	 * @see DATACMNS-233
	 */
	public void returnsNullForEmptyStringSource() {
		assertThat(converter.convert("", sourceDescriptor, targetDescriptor), is(nullValue()));
	}

	private void assertMatches(boolean matchExpected) {

		assertThat(converter.matches(sourceDescriptor, targetDescriptor), is(matchExpected));
	}

	@Test
	public void convertsStringToUserCorrectly() throws Exception {

		ApplicationContext context = initContextWithRepo();
		converter.setApplicationContext(context);

		when(service.canConvert(String.class, Long.class)).thenReturn(true);
		when(service.convert(anyString(), eq(Long.class))).thenReturn(1L);

		converter.convert("1", sourceDescriptor, targetDescriptor);

		UserRepository bean = context.getBean(UserRepository.class);
		UserRepository repo = (UserRepository) ((Advised) bean).getTargetSource().getTarget();

		verify(repo, times(1)).findOne(1L);
	}

	/**
	 * @see DATACMNS-133
	 */
	@Test
	public void discoversFactoryAndRepoFromParentApplicationContext() {

		ApplicationContext parent = initContextWithRepo();
		ApplicationContext context = new GenericApplicationContext(parent);

		when(service.canConvert(String.class, Long.class)).thenReturn(true);

		converter.setApplicationContext(context);
		assertThat(converter.matches(sourceDescriptor, targetDescriptor), is(true));
	}

	private ApplicationContext initContextWithRepo() {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DummyRepositoryFactoryBean.class);
		builder.addPropertyValue("repositoryInterface", UserRepository.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("provider", builder.getBeanDefinition());

		return new GenericApplicationContext(factory);
	}

	private static class User {

	}

	private static interface UserRepository extends CrudRepository<User, Long> {

	}
}
