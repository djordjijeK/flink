package krivokapic.djordjije.flink.jobs;

import krivokapic.djordjije.flink.BaseFlinkJob;
import krivokapic.djordjije.kafka.producer.event.MetricEvent;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;


public class TimeWindowMonitorsJob extends BaseFlinkJob {

    @Override
    protected void createComputationGraph(DataStream<MetricEvent> dataStream) {
        dataStream.keyBy(new TimeWindowMonitorsKeySelector())
                //.window(SlidingEventTimeWindows.of(Duration.ofSeconds(30), Duration.ofSeconds(15)))
                .window(TumblingEventTimeWindows.of(Duration.ofSeconds(30)))
                .process(new Monitor())
                .print();
    }


    private static class TimeWindowMonitorsKeySelector implements KeySelector<MetricEvent, String> {
        @Override
        public String getKey(MetricEvent metricEvent) {
            return String.format("%s@%s@%s", metricEvent.location(), metricEvent.device(), metricEvent.metric());
        }
    }


    private static class Monitor extends ProcessWindowFunction<MetricEvent, String, String, TimeWindow> {

        @Override
        public void process(String key, ProcessWindowFunction<MetricEvent, String, String, TimeWindow>.Context context, Iterable<MetricEvent> iterable, Collector<String> collector) {
            String[] parts = key.split("@");
            String metricName = parts[parts.length - 1];
            double threshold = threshold(metricName);
            boolean inAlarm = true;

            StringBuilder stringBuilder = new StringBuilder("[");
            for(MetricEvent metricEvent : iterable) {
                if (metricEvent.value() < threshold) {
                    inAlarm = false;
                }
                stringBuilder.append(metricEvent.value());
                stringBuilder.append(", ");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append("]");

            String status = String.format("Device: %s | Metric: %s | Threshold: %.2f | Status: %s | Window: ", parts[1], metricName, threshold, inAlarm ? "ALARM" : "OK");
            stringBuilder.insert(0, status);

            collector.collect(stringBuilder.toString());
        }


        @Override
        public void open(OpenContext openContext) throws Exception {
            super.open(openContext);
        }

    }

    private static double threshold(String metric) {
        switch (metric) {
            case "Cpu Utilization":
                return 70f;
            case "Memory Utilization":
                return 75f;
            case "Network Latency":
                return 100f;
            default:
                throw new IllegalArgumentException("Invalid metric: " + metric);
        }
    }


    public static void main(String[] args) {
        String topic = "metrics";
        String brokers = "localhost:29091,localhost:29092,localhost:29093";

        TimeWindowMonitorsJob timeWindowMonitorsJob = new TimeWindowMonitorsJob();
        timeWindowMonitorsJob.executeLocal(brokers, topic, 3);
    }
}
