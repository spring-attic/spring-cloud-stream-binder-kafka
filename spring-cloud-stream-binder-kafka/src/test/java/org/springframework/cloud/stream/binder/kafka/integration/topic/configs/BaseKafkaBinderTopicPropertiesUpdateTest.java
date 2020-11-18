/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka.integration.topic.configs;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Heiko Does
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		classes = BaseKafkaBinderTopicPropertiesUpdateTest.TopicAutoConfigsTestConfig.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
		"spring.cloud.stream.kafka.bindings.standard-out.producer.topic.properties.retention.ms=9001",
		"spring.cloud.stream.kafka.default.producer.topic.properties.retention.ms=-1",
		"spring.cloud.stream.kafka.bindings.standard-in.consumer.topic.properties.retention.ms=9001",
		"spring.cloud.stream.kafka.default.consumer.topic.properties.retention.ms=-1"
})
@DirtiesContext
public abstract class BaseKafkaBinderTopicPropertiesUpdateTest {

	private static final String KAFKA_BROKERS_PROPERTY = "spring.cloud.stream.kafka.binder.brokers";

	@ClassRule
	public static EmbeddedKafkaRule kafkaEmbedded = new EmbeddedKafkaRule(1, true, "standard-in", "standard-out");

	@BeforeClass
	public static void setup() {
		System.setProperty(KAFKA_BROKERS_PROPERTY,
				kafkaEmbedded.getEmbeddedKafka().getBrokersAsString());
	}

	@AfterClass
	public static void clean() {
		System.clearProperty(KAFKA_BROKERS_PROPERTY);
	}

	@EnableBinding(CustomBindingForTopicPropertiesUpdateTesting.class)
	@EnableAutoConfiguration
	public static class TopicAutoConfigsTestConfig {

		@StreamListener("standard-in")
		@SendTo("standard-out")
		public String process(String payload) {
			return payload;
		}
	}

	interface CustomBindingForTopicPropertiesUpdateTesting {

		@Input("standard-in")
		SubscribableChannel standardIn();

		@Output("standard-out")
		MessageChannel standardOut();
	}

}
