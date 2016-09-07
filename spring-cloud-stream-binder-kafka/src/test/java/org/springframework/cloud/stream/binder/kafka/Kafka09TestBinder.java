/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka;

import org.springframework.cloud.stream.binder.kafka.admin.Kafka09AdminUtilsOperation;
import org.springframework.cloud.stream.binder.kafka.configuration.KafkaBinderConfigurationProperties;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;

/**
 * Test support class for {@link KafkaMessageChannelBinder}. Creates a binder that uses
 * an embedded Kafka cluster.
 * @author Eric Bottard
 * @author Marius Bogoevici
 * @author David Turanski
 * @author Gary Russell
 * @author Soby Chacko
 */
public class Kafka09TestBinder extends AbstractKafkaTestBinder {

	public Kafka09TestBinder(KafkaBinderConfigurationProperties binderConfiguration) {
		try {
			KafkaMessageChannelBinder binder = new KafkaMessageChannelBinder(binderConfiguration);
			binder.setCodec(getCodec());
			ProducerListener producerListener = new LoggingProducerListener();
			binder.setProducerListener(producerListener);
			GenericApplicationContext context = new GenericApplicationContext();
			context.refresh();
			binder.setApplicationContext(context);
			binder.setAdminUtilsOperation(new Kafka09AdminUtilsOperation());
			binder.afterPropertiesSet();
			this.setBinder(binder);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
