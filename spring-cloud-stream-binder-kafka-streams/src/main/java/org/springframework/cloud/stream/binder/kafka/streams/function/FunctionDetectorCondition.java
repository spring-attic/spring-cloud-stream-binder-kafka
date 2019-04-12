/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka.streams.function;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Custom {@link org.springframework.context.annotation.Condition} that detects the presence
 * of java.util.Function|Consumer beans. Used for Kafka Streams function support.
 *
 * @author Soby Chakco
 * @since 2.2.0
 */
public class FunctionDetectorCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		if (context != null &&  context.getBeanFactory() != null) {
			final Map<String, Function> functionTypes = context.getBeanFactory().getBeansOfType(Function.class);
			final Map<String, Consumer> consumerTypes = context.getBeanFactory().getBeansOfType(Consumer.class);

			if (!functionTypes.isEmpty() || !consumerTypes.isEmpty()) {
				return ConditionOutcome.match("Matched. Function/Consumer beans found");
			}
			else {
				return ConditionOutcome.noMatch("No match. No Function/Consumer beans found");
			}
		}
		return ConditionOutcome.noMatch("No match. No Function/Consumer beans found");
	}
}
