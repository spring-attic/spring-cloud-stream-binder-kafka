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

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;

import org.springframework.cloud.stream.binder.AbstractBinder;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.DefaultBinding;
import org.springframework.cloud.stream.binder.EmbeddedHeaderUtils;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.binder.HeaderMode;
import org.springframework.cloud.stream.binder.MessageValues;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaExtendedBindingProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaProducerProperties;
import org.springframework.cloud.stream.binder.kafka.provisioning.KafkaTopicProvisioner;
import org.springframework.cloud.stream.binder.kstream.config.KStreamBinderProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * @author Marius Bogoevici
 */
public class KStreamBinder extends
		AbstractBinder<KStream<Object, Object>, ExtendedConsumerProperties<KafkaConsumerProperties>, ExtendedProducerProperties<KafkaProducerProperties>>
		implements ExtendedPropertiesBinder<KStream<Object, Object>, KafkaConsumerProperties, KafkaProducerProperties> {

	private String[] headers;

	private final KafkaTopicProvisioner kafkaTopicProvisioner;

	private final KafkaExtendedBindingProperties kafkaExtendedBindingProperties;

	public KStreamBinder(KStreamBinderProperties kStreamBinderProperties, KafkaTopicProvisioner kafkaTopicProvisioner,
						KafkaExtendedBindingProperties kafkaExtendedBindingProperties) {
		this.headers = EmbeddedHeaderUtils.headersToEmbed(kStreamBinderProperties.getHeaders());
		this.kafkaTopicProvisioner = kafkaTopicProvisioner;
		this.kafkaExtendedBindingProperties = kafkaExtendedBindingProperties;
	}

	@Override
	protected Binding<KStream<Object, Object>> doBindConsumer(String name, String group,
			KStream<Object, Object> inputTarget, ExtendedConsumerProperties<KafkaConsumerProperties> properties) {
		this.kafkaTopicProvisioner.provisionConsumerDestination(name, group, properties);
		return new DefaultBinding<>(name, group, inputTarget, null);
	}

	@Override
	protected Binding<KStream<Object, Object>> doBindProducer(String name, KStream<Object, Object> outboundBindTarget,
			ExtendedProducerProperties<KafkaProducerProperties> properties) {
		this.kafkaTopicProvisioner.provisionProducerDestination(name, properties);
		if (HeaderMode.embeddedHeaders.equals(properties.getHeaderMode())) {
			outboundBindTarget = outboundBindTarget.map(new KeyValueMapper<Object, Object, KeyValue<Object, Object>>() {
				@Override
				public KeyValue<Object, Object> apply(Object k, Object v) {
					if (v instanceof Message) {
						try {
							return new KeyValue<>(k, (Object)KStreamBinder.this.serializeAndEmbedHeadersIfApplicable((Message<?>) v));
						}
						catch (Exception e) {
							throw new IllegalArgumentException(e);
						}
					}
					else {
						throw new IllegalArgumentException("Wrong type of message " + v);
					}
				}
			});
		}
		else {
			if (!properties.isUseNativeEncoding()) {
				outboundBindTarget = outboundBindTarget
						.map(new KeyValueMapper<Object, Object, KeyValue<Object, Object>>() {
							@Override
							public KeyValue<Object, Object> apply(Object k, Object v) {
								return KeyValue.pair(k, (Object)KStreamBinder.this.serializePayloadIfNecessary((Message<?>) v));
							}
						});
			}
			else {
				outboundBindTarget = outboundBindTarget
						.map(new KeyValueMapper<Object, Object, KeyValue<Object, Object>>() {
							@Override
							public KeyValue<Object, Object> apply(Object k, Object v) {
								return KeyValue.pair(k, ((Message<?>) v).getPayload());
							}
						});
			}
		}
		if (!properties.isUseNativeEncoding()) {
			outboundBindTarget.map(new KeyValueMapper<Object, Object, KeyValue<byte[], byte[]>>() {
				@Override
				public KeyValue<byte[], byte[]> apply(Object k, Object v) {
					return new KeyValue<>((byte[]) k, (byte[]) v);
				}
			}).to(Serdes.ByteArray(),
					Serdes.ByteArray(), name);
		}
		else {
			outboundBindTarget.to(name);
		}
		return new DefaultBinding<>(name, null, outboundBindTarget, null);
	}

	private byte[] serializeAndEmbedHeadersIfApplicable(Message<?> message) throws Exception {
		MessageValues transformed = serializePayloadIfNecessary(message);
		byte[] payload;

		Object contentType = transformed.get(MessageHeaders.CONTENT_TYPE);
		// transform content type headers to String, so that they can be properly embedded
		// in JSON
		if (contentType instanceof MimeType) {
			transformed.put(MessageHeaders.CONTENT_TYPE, contentType.toString());
		}
		Object originalContentType = transformed.get(BinderHeaders.BINDER_ORIGINAL_CONTENT_TYPE);
		if (originalContentType instanceof MimeType) {
			transformed.put(BinderHeaders.BINDER_ORIGINAL_CONTENT_TYPE, originalContentType.toString());
		}
		payload = EmbeddedHeaderUtils.embedHeaders(transformed, headers);
		return payload;
	}

	@Override
	public KafkaConsumerProperties getExtendedConsumerProperties(String channelName) {
		return this.kafkaExtendedBindingProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public KafkaProducerProperties getExtendedProducerProperties(String channelName) {
		return this.kafkaExtendedBindingProperties.getExtendedProducerProperties(channelName);
	}

}
