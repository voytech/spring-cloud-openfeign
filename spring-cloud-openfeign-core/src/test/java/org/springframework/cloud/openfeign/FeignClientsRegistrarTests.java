/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Spencer Gibb
 * @author Gang Li
 * @author Michal Domagala
 * @author Szymon Linowski
 */
class FeignClientsRegistrarTests {

	@Test
	void badNameHttpPrefix() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> testGetName("http://bad_hostname"));
	}

	@Test
	void badNameHttpsPrefix() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> testGetName("https://bad_hostname"));
	}

	@Test
	void badName() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> testGetName("bad_hostname"));
	}

	@Test
	void badNameStartsWithHttp() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> testGetName("http_bad_hostname"));
	}

	@Test
	void goodName() {
		String name = testGetName("good-name");
		assertThat(name).as("name was wrong").isEqualTo("good-name");
	}

	@Test
	void goodNameHttpPrefix() {
		String name = testGetName("https://good-name");
		assertThat(name).as("name was wrong").isEqualTo("https://good-name");
	}

	@Test
	void goodNameHttpsPrefix() {
		String name = testGetName("https://goodname");
		assertThat(name).as("name was wrong").isEqualTo("https://goodname");
	}

	private String testGetName(String name) {
		FeignClientsRegistrar registrar = new FeignClientsRegistrar();
		registrar.setEnvironment(new MockEnvironment());
		return registrar.getName(Collections.singletonMap("name", name));
	}

	@Test
	void testFallback() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(FallbackTestConfig.class));
	}

	@Test
	void testFallbackFactory() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(FallbackFactoryTestConfig.class));
	}

	@Test
	void shouldPassSubLevelFeignClient() {
		AnnotationConfigApplicationContext config = new AnnotationConfigApplicationContext();
		((DefaultListableBeanFactory) config.getBeanFactory()).setAllowBeanDefinitionOverriding(false);
		config.register(TopLevelSubLevelTestConfig.class);
		assertThatCode(() -> config.refresh())
				.as("Case https://github.com/spring-cloud/spring-cloud-openfeign/issues/331 should be solved")
				.doesNotThrowAnyException();
	}

	@FeignClient(name = "fallbackTestClient", url = "http://localhost:8080/", fallback = FallbackClient.class)
	protected interface FallbackClient {

		@GetMapping("/hello")
		String fallbackTest();

	}

	@FeignClient(name = "fallbackFactoryTestClient", url = "http://localhost:8081/",
			fallbackFactory = FallbackFactoryClient.class)
	protected interface FallbackFactoryClient {

		@GetMapping("/hello")
		String fallbackFactoryTest();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableFeignClients(clients = { FeignClientsRegistrarTests.FallbackClient.class })
	protected static class FallbackTestConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableFeignClients(clients = { FeignClientsRegistrarTests.FallbackFactoryClient.class })
	protected static class FallbackFactoryTestConfig {

	}

	@EnableFeignClients(clients = { org.springframework.cloud.openfeign.feignclientsregistrar.TopLevelClient.class,
			org.springframework.cloud.openfeign.feignclientsregistrar.sub.SubLevelClient.class })
	protected static class TopLevelSubLevelTestConfig {

	}

}
