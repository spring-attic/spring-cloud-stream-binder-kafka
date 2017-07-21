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
package org.springframework.cloud.stream.binder.kstream;

import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.kstream.annotations.KStreamProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KStreamBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.handler.annotation.SendTo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 */
public class KStreamInteractiveQueryIntegrationTests {

	@ClassRule
	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, "counts-id");

	private static Consumer<String, String> consumer;

	@BeforeClass
	public static void setUp() throws Exception {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("group-id", "false", embeddedKafka);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
		consumer = cf.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "counts-id");
	}

	@AfterClass
	public static void tearDown() {
		consumer.close();
	}

	@Test
	public void testKstreamBinderWithPojoInputAndStringOuput() throws Exception {
		SpringApplication app = new SpringApplication(ProductCountApplication.class);
		app.setWebEnvironment(false);
		ConfigurableApplicationContext context = app.run("--server.port=0",
				"--spring.cloud.stream.bindings.input.destination=foos",
				"--spring.cloud.stream.bindings.output.destination=counts-id",
				"--spring.cloud.stream.kstream.binder.streamConfiguration.commit.interval.ms=1000",
				"--spring.cloud.stream.kstream.binder.streamConfiguration.key.serde=org.apache.kafka.common.serialization.Serdes$StringSerde",
				"--spring.cloud.stream.kstream.binder.streamConfiguration.value.serde=org.apache.kafka.common.serialization.Serdes$StringSerde",
				"--spring.cloud.stream.bindings.output.producer.headerMode=raw",
				"--spring.cloud.stream.bindings.output.producer.useNativeEncoding=true",
				"--spring.cloud.stream.bindings.input.consumer.headerMode=raw",
				"--spring.cloud.stream.kstream.binder.brokers=" + embeddedKafka.getBrokersAsString(),
				"--spring.cloud.stream.kstream.binder.zkNodes=" + embeddedKafka.getZookeeperConnectionString());
		receiveAndValidateFoo(context);
		context.close();
	}

	private void receiveAndValidateFoo(ConfigurableApplicationContext context) throws Exception{
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		DefaultKafkaProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf, true);
		template.setDefaultTopic("foos");
		template.sendDefault("{\"id\":\"123\"}");
		ConsumerRecord<String, String> cr = KafkaTestUtils.getSingleRecord(consumer, "counts-id");
		assertThat(cr.value().contains("Count for product with ID 123: 1")).isTrue();

		ProductCountApplication.Foo foo = context.getBean(ProductCountApplication.Foo.class);
		assertThat(foo.getProductStock("123").equals(1L));
	}

	@EnableBinding(KStreamProcessor.class)
	@EnableAutoConfiguration
	public static class ProductCountApplication {

		@Autowired
		private KStreamBuilderFactoryBean kStreamBuilderFactoryBean;

		@StreamListener("input")
		@SendTo("output")
		public KStream<?, String> process(KStream<?, Product> input) {

			return input
					.filter((key, product) -> product.getId().equals("123"))
					.map((k,v) -> new KeyValue<>(v.getId(), v))
					.groupByKey(new Serdes.StringSerde(), new JsonSerde<>(Product.class))
					.count("prod-id-count-store")
					.toStream()
					.map((w,c) -> new KeyValue<>(null, ("Count for product with ID 123: " + c)));
		}

		@Bean
		public Foo foo(KStreamBuilderFactoryBean kStreamBuilderFactoryBean) {
			return new Foo(kStreamBuilderFactoryBean);
		}


		static class Foo {
			KStreamBuilderFactoryBean kStreamBuilderFactoryBean;

			Foo(KStreamBuilderFactoryBean kStreamBuilderFactoryBean) {
				this.kStreamBuilderFactoryBean = kStreamBuilderFactoryBean;
			}

			public Long getProductStock(String id) {
				KafkaStreams streams = kStreamBuilderFactoryBean.getKafkaStreams();
				ReadOnlyKeyValueStore<String, Long> keyValueStore =
						streams.store("prod-id-count-store", QueryableStoreTypes.keyValueStore());
				return keyValueStore.get(id);
			}
		}

	}

	static class Product {

		String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
