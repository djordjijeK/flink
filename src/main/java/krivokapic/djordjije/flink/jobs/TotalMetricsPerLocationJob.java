package krivokapic.djordjije.flink.jobs;

import krivokapic.djordjije.flink.BaseFlinkJob;
import krivokapic.djordjije.kafka.producer.event.MetricEvent;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;

import java.time.Duration;


public class TotalMetricsPerLocationJob extends BaseFlinkJob {

    @Override
    protected void createComputationGraph(DataStream<MetricEvent> dataStream) {
        dataStream.map(new ToTupleMapper())
                .keyBy(tuple -> tuple.f0)
                .window(TumblingEventTimeWindows.of(Duration.ofSeconds(10))) // total metrics per location per 10 seconds
                .sum(1)
                .print();
    }


    private static class ToTupleMapper implements MapFunction<MetricEvent, Tuple2<String, Integer>> {
        @Override
        public Tuple2<String, Integer> map(MetricEvent metricEvent) {
            return new Tuple2<>(metricEvent.location(), 1);
        }
    }


    public static void main(String[] args) {
        String topic = "metrics";
        String brokers = "localhost:29091,localhost:29092,localhost:29093";

        TotalMetricsPerLocationJob totalMetricsPerLocationJob = new TotalMetricsPerLocationJob();
        totalMetricsPerLocationJob.executeLocal(brokers, topic, 3);
    }

}
