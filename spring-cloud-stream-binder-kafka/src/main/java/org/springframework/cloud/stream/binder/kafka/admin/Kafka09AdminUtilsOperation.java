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

import java.util.Properties;

import kafka.admin.AdminUtils;
import kafka.api.TopicMetadata;
import kafka.utils.ZkUtils;

/**
 * @author Soby Chacko
 */
public class Kafka09AdminUtilsOperation implements AdminUtilsOperation {

	public void invokeAddPartitions(ZkUtils zkUtils, String topic, int numPartitions,
									String replicaAssignmentStr, boolean checkBrokerAvailable) {
		AdminUtils.addPartitions(zkUtils, topic, numPartitions,
				replicaAssignmentStr, checkBrokerAvailable);
	}

	public short errorCodeFromTopicMetadata(String topic, ZkUtils zkUtils) {
		TopicMetadata topicMetadata = AdminUtils.fetchTopicMetadataFromZk(topic, zkUtils);
		return topicMetadata.errorCode();
	}

	public int partitionSize(String topic, ZkUtils zkUtils) {
		TopicMetadata topicMetadata = AdminUtils.fetchTopicMetadataFromZk(topic, zkUtils);
		return topicMetadata.partitionsMetadata().size();
	}

	public void invokeCreateTopic(ZkUtils zkUtils, String topic, int partitions,
								int replicationFactor, Properties topicConfig) {
		AdminUtils.createTopic(zkUtils, topic, partitions, replicationFactor,
				topicConfig);
	}
}
