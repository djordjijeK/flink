package krivokapic.djordjije.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeLogDirsResult;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Properties;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;


public class KafkaAdminClient {
    private static final Logger logger = LogManager.getLogger(KafkaAdminClient.class);


    private final AdminClient kafkaAdminClient;


    public KafkaAdminClient(String bootstrapServers) {
        Properties adminClientConfiguration = new Properties();
        adminClientConfiguration.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.kafkaAdminClient = AdminClient.create(adminClientConfiguration);
    }


    public void createTopic(String topicName, int partitions, int replicationFactor) {
        if (this.topicExists(topicName)) {
            logger.debug("Topic {} already exists", topicName);
            return;
        }

        try {
            NewTopic newTopic = new NewTopic(topicName, partitions, (short) replicationFactor);
            KafkaFuture<Void> future = this.kafkaAdminClient.createTopics(Collections.singleton(newTopic)).all();

            future.get();

            logger.info("Created new topic {}", topicName);
        } catch (ExecutionException e) {
            logger.error("Execution exception while creating topic", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for topic creation result", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("Error creating Kafka topic", e);
            throw new RuntimeException(e);
        }
    }


    public void describeTopics() {
        try {
            logger.info("Describing cluster topics...");
            Map<String, TopicDescription> topics = this.kafkaAdminClient.describeTopics(this.kafkaAdminClient.listTopics().names().get()).allTopicNames().get();

            List<Integer> brokerIds = this.kafkaAdminClient.describeCluster().nodes().get().stream()
                    .map(Node::id)
                    .collect(Collectors.toList());

            DescribeLogDirsResult logDirsResult = this.kafkaAdminClient.describeLogDirs(brokerIds);
            Map<TopicPartition, Long> partitionSizes = new HashMap<>();

            logDirsResult.descriptions().forEach((broker, future) -> {
                try {
                    future.get().forEach((logDir, logDirInfo) -> {
                        logDirInfo.replicaInfos().forEach((topicPartitionReplica, replicaInfo) -> {
                            partitionSizes.put(topicPartitionReplica, replicaInfo.size());
                        });
                    });
                } catch (ExecutionException e) {
                    logger.error(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error(e);
                }
            });

            topics.forEach((topicName, description) -> {
                StringBuilder topicDetails = new StringBuilder();
                topicDetails.append("Topic: ").append(topicName).append("\n");

                description.partitions().forEach(partitionInfo -> {
                    TopicPartition topicPartition = new TopicPartition(topicName, partitionInfo.partition());
                    Long size = partitionSizes.getOrDefault(topicPartition, 0L);

                    topicDetails.append(String.format("\tPartition: %d (Size: %d bytes) | Leader: %s | Replicas: %s | In Sync Replicas: %s\n",
                            partitionInfo.partition(),
                            size,
                            String.format("%s:%s", partitionInfo.leader().host(), partitionInfo.leader().port()),
                            partitionInfo.replicas().stream().map(node -> String.format("%s:%s", node.host(), node.port())).collect(Collectors.joining(", ")),
                            partitionInfo.isr().stream().map(node -> String.format("%s:%s", node.host(), node.port())).collect(Collectors.joining(", "))
                    ));
                });

                logger.info(topicDetails.toString());
            });
        } catch (ExecutionException e) {
            logger.error("Execution exception while describing topics", e);
            throw new RuntimeException(e);
        } catch (
                InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting to describe topics", e);
            throw new RuntimeException(e);
        }
    }


    public void increaseTopicPartitions(String topic, int partitions) {
        try {
            CreatePartitionsResult createPartitionsResult = this.kafkaAdminClient.createPartitions(Map.of(topic, NewPartitions.increaseTo(partitions)));
            KafkaFuture<Void> future = createPartitionsResult.all();
            future.get();
        } catch (ExecutionException e) {
            logger.error("Execution exception while changing topic {} partitions to {}", topic, partitions, e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while changing topic {} partitions to {}", topic, partitions, e);
            throw new RuntimeException(e);
        }
    }


    public void deleteTopic(String topicName) {
        if (!this.topicExists(topicName)) {
            logger.debug("Topic {} does not exist", topicName);
            return;
        }

        try {
            DeleteTopicsResult deleteTopicsResult = this.kafkaAdminClient.deleteTopics(Collections.singleton(topicName));
            KafkaFuture<Void> future = deleteTopicsResult.all();
            future.get();

            logger.info("Deleted topic {}", topicName);
        } catch (ExecutionException e) {
            logger.error("Execution exception while deleting topic", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for topic deletion result", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("Error deleting Kafka topic", e);
            throw new RuntimeException(e);
        }
    }


    public boolean topicExists(String topicName) {
        try {
            ListTopicsResult listTopicsResult = this.kafkaAdminClient.listTopics();
            Set<String> topics = listTopicsResult.names().get();

            logger.debug("Retrieved list of topics from the cluster: {}", topics);

            return topics.contains(topicName);
        } catch (Exception e) {
            logger.error("Failed to retrieve list of topics from the cluster", e);
            throw new RuntimeException(e);
        }
    }
}
