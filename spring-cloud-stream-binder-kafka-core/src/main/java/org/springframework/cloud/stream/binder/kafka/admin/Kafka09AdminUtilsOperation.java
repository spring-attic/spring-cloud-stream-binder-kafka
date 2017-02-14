/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka.admin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import kafka.utils.ZkUtils;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Soby Chacko
 */
public class Kafka09AdminUtilsOperation implements AdminUtilsOperation {

	private static Class<?> ADMIN_UTIL_CLASS;

	static {
		try {
			ADMIN_UTIL_CLASS = ClassUtils.forName("kafka.admin.AdminUtils", null);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("AdminUtils class not found", e);
		}
	}

	public void invokeAddPartitions(ZkUtils zkUtils, String topic, int numPartitions,
									String replicaAssignmentStr, boolean checkBrokerAvailable) {
		try {
			Method[] declaredMethods = ADMIN_UTIL_CLASS.getDeclaredMethods();
			Method addPartitions = null;
			for (Method m : declaredMethods) {
				if (m.getName().equals("addPartitions")) {
					addPartitions = m;
				}
			}
			if (addPartitions != null) {
				addPartitions.invoke(null, zkUtils, topic, numPartitions,
						replicaAssignmentStr, checkBrokerAvailable);
			}
			else {
				throw new InvocationTargetException(
						new RuntimeException("method not found"));
			}
		}
		catch (InvocationTargetException e) {
			ReflectionUtils.handleInvocationTargetException(e);
		}
		catch (IllegalAccessException e) {
			ReflectionUtils.handleReflectionException(e);
		}

	}

	public short errorCodeFromTopicMetadata(String topic, ZkUtils zkUtils) {
		try {
			Method fetchTopicMetadataFromZk = ReflectionUtils.findMethod(ADMIN_UTIL_CLASS, "fetchTopicMetadataFromZk", String.class, ZkUtils.class);
			Object result = fetchTopicMetadataFromZk.invoke(null, topic, zkUtils);
			Class<?> topicMetadataClass = ClassUtils.forName("kafka.api.TopicMetadata", null);
			Method errorCodeMethod = ReflectionUtils.findMethod(topicMetadataClass, "errorCode");
			return (short) errorCodeMethod.invoke(result);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("AdminUtils class not found", e);
		}
		catch (InvocationTargetException e) {
			ReflectionUtils.handleInvocationTargetException(e);
		}
		catch (IllegalAccessException e) {
			ReflectionUtils.handleReflectionException(e);
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public int partitionSize(String topic, ZkUtils zkUtils) {
		try {
			Method fetchTopicMetadataFromZk = ReflectionUtils.findMethod(ADMIN_UTIL_CLASS, "fetchTopicMetadataFromZk", String.class, ZkUtils.class);
			Object result = fetchTopicMetadataFromZk.invoke(null, topic, zkUtils);
			Class<?> topicMetadataClass = ClassUtils.forName("kafka.api.TopicMetadata", null);

			Method partitionsMetadata = ReflectionUtils.findMethod(topicMetadataClass, "partitionsMetadata");
			scala.collection.Seq<kafka.api.PartitionMetadata> partitionSize =
					(scala.collection.Seq<kafka.api.PartitionMetadata>)partitionsMetadata.invoke(result);

			return partitionSize.size();
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("AdminUtils class not found", e);
		}
		catch (InvocationTargetException e) {
			ReflectionUtils.handleInvocationTargetException(e);
		}
		catch (IllegalAccessException e) {
			ReflectionUtils.handleReflectionException(e);
		}
		return 0;

	}

	public void invokeCreateTopic(ZkUtils zkUtils, String topic, int partitions,
								int replicationFactor, Properties topicConfig) {
		try {
			Method[] declaredMethods = ADMIN_UTIL_CLASS.getDeclaredMethods();
			Method createTopic = null;
			for (Method m : declaredMethods) {
				if (m.getName().equals("createTopic")) {
					createTopic = m;
					break;
				}
			}
			if (createTopic != null) {
				createTopic.invoke(null, zkUtils, topic, partitions,
						replicationFactor, topicConfig);
			}
			else {
				throw new InvocationTargetException(
						new RuntimeException("method not found"));
			}
		}
		catch (InvocationTargetException e) {
			ReflectionUtils.handleInvocationTargetException(e);
		}
		catch (IllegalAccessException e) {
			ReflectionUtils.handleReflectionException(e);
		}
	}
}