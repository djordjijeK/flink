package krivokapic.djordjije.kafka.producer;

import krivokapic.djordjije.kafka.producer.event.MetricEvent;
import krivokapic.djordjije.kafka.producer.event.MetricEventFactory;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class ProducerApplication {

    public static void main(String[] args) {
        String topic = "metrics";
        String brokers = "localhost:29091,localhost:29092,localhost:29093";

        Properties producerConfiguration = new Properties();
        producerConfiguration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        producerConfiguration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerConfiguration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerConfiguration.put(ProducerConfig.ACKS_CONFIG, "all");
        producerConfiguration.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerConfiguration.put(ProducerConfig.LINGER_MS_CONFIG, 100);

        Producer<MetricEvent> producer = KafkaProducer.builder()
                .properties(producerConfiguration)
                .topic(topic)
                .eventFactory(MetricEventFactory.create())
                .threads(2)
                .maxJitter(1_500)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();

        producer.start();
    }

}