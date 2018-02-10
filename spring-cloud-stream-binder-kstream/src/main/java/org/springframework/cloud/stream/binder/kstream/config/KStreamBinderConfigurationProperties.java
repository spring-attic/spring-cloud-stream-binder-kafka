/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;

/**
 * @author Soby Chacko
 */
public class KStreamBinderConfigurationProperties extends KafkaBinderConfigurationProperties {

	public enum SerdeError {
		logAndContinue,
		logAndFail,
		sendToDlq
	}

	private String applicationId = "default";

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	/**
	 * {@link org.apache.kafka.streams.errors.DeserializationExceptionHandler} to use
	 * when there is a Serde error. {@link KStreamBinderConfigurationProperties.SerdeError}
	 * values are used to provide the exception handler on consumer binding.
	 */
	private KStreamBinderConfigurationProperties.SerdeError serdeError;

	public KStreamBinderConfigurationProperties.SerdeError getSerdeError() {
		return serdeError;
	}

	public void setSerdeError(KStreamBinderConfigurationProperties.SerdeError serdeError) {
		this.serdeError = serdeError;
	}

}
