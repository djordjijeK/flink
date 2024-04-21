package krivokapic.djordjije.kafka;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class KafkaAdminApplication {

    public static void main(String[] args) {
        String topic = "metrics";
        String brokers = "localhost:29091,localhost:29092,localhost:29093";

        KafkaAdminClient kafkaAdminClient = new KafkaAdminClient(brokers);
        kafkaAdminClient.createTopic(topic, 3, 3);
        // kafkaAdminClient.increaseTopicPartitions(topic, 10);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(kafkaAdminClient::describeTopics, 0, 60, TimeUnit.SECONDS);
    }

}
