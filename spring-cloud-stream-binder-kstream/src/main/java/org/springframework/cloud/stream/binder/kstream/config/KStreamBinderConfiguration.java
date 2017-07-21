/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.stream.binder.kstream.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.common.utils.AppInfoParser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.admin.AdminUtilsOperation;
import org.springframework.cloud.stream.binder.kafka.admin.Kafka09AdminUtilsOperation;
import org.springframework.cloud.stream.binder.kafka.admin.Kafka10AdminUtilsOperation;
import org.springframework.cloud.stream.binder.kafka.provisioning.KafkaTopicProvisioner;
import org.springframework.cloud.stream.binder.kstream.KStreamBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Marius Bogoevici
 */
@Configuration
@EnableConfigurationProperties(KStreamExtendedBindingProperties.class)
public class KStreamBinderConfiguration {

	@Autowired(required = false)
	private AdminUtilsOperation adminUtilsOperation;

	private static final Log logger = LogFactory.getLog(KStreamBinderConfiguration.class);

	@Bean
	public KafkaTopicProvisioner provisioningProvider(KStreamBinderProperties kStreamBinderProperties) {
		return new KafkaTopicProvisioner(kStreamBinderProperties, adminUtilsOperation);
	}

	@Bean
	public KStreamBinder kStreamBinder(KStreamBinderProperties kStreamBinderProperties, KafkaTopicProvisioner kafkaTopicProvisioner, KStreamExtendedBindingProperties kStreamExtendedBindingProperties) {
		return new KStreamBinder(kStreamBinderProperties, kafkaTopicProvisioner, kStreamExtendedBindingProperties);
	}

	@Bean(name = "adminUtilsOperation")
	@Conditional(Kafka09Present.class)
	@ConditionalOnClass(name = "kafka.admin.AdminUtils")
	public AdminUtilsOperation kafka09AdminUtilsOperation() {
		logger.info("AdminUtils selected: Kafka 0.9 AdminUtils");
		return new Kafka09AdminUtilsOperation();
	}

	@Bean(name = "adminUtilsOperation")
	@Conditional(Kafka10Present.class)
	@ConditionalOnClass(name = "kafka.admin.AdminUtils")
	public AdminUtilsOperation kafka10AdminUtilsOperation() {
		logger.info("AdminUtils selected: Kafka 0.10 AdminUtils");
		return new Kafka10AdminUtilsOperation();
	}

	static class Kafka10Present implements Condition {

		@Override
		public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
			return AppInfoParser.getVersion().startsWith("0.10");
		}
	}

	static class Kafka09Present implements Condition {

		@Override
		public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
			return AppInfoParser.getVersion().startsWith("0.9");
		}
	}
}
