package krivokapic.djordjije.flink;

import krivokapic.djordjije.kafka.producer.event.MetricEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.Duration;


public abstract class BaseFlinkJob {
    private static final Logger logger = LogManager.getLogger(BaseFlinkJob.class);
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    protected abstract void createComputationGraph(DataStream<MetricEvent> dataStream);


    public void executeLocal(String brokers, String topic, int parallelism) {
        StreamExecutionEnvironment streamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        streamExecutionEnvironment.setParallelism(parallelism);

        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((event, y) -> {
                    try {
                        MetricEvent metricEvent = objectMapper.readValue(event, MetricEvent.class);
                        return metricEvent.timestamp().toEpochMilli();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });

        execute(streamExecutionEnvironment, brokers, topic, watermarkStrategy);
    }


    public void executeOnCluster(String host, int port, String brokers, String topic, int parallelism) {
        StreamExecutionEnvironment streamExecutionEnvironment = StreamExecutionEnvironment.createRemoteEnvironment(host, port, "build/libs/flink-job-1.0.jar");
        streamExecutionEnvironment.setParallelism(parallelism);

        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((event, y) -> {
                    try {
                        MetricEvent metricEvent = objectMapper.readValue(event, MetricEvent.class);
                        return metricEvent.timestamp().toEpochMilli();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });

        execute(streamExecutionEnvironment, brokers, topic, watermarkStrategy);
    }


    private void execute(
        StreamExecutionEnvironment streamExecutionEnvironment,
        String brokers,
        String topic,
        WatermarkStrategy<String> watermarkStrategy
    ) {
        DataStream<MetricEvent> dataStream = streamExecutionEnvironment.fromSource(createKafkaSource(brokers, topic), watermarkStrategy, "Kafka Source")
                .map((MapFunction<String, MetricEvent>) json -> objectMapper.readValue(json, MetricEvent.class));

        createComputationGraph(dataStream);

        try {
            streamExecutionEnvironment.execute();
        } catch (Exception e) {
            logger.error("Failed to start the job", e);
        }
    }


    private static KafkaSource<String> createKafkaSource(String brokers, String topic) {
        return KafkaSource.<String>builder()
                .setBootstrapServers(brokers)
                .setTopics(topic)
                .setGroupId("FlinkKafkaSource")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setProperty("partition.discovery.interval.ms", "10000")
                .build();
    }
}
