package krivokapic.djordjije.flink.jobs;

import krivokapic.djordjije.flink.BaseFlinkJob;
import krivokapic.djordjije.kafka.producer.event.MetricEvent;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.time.Duration;
import java.util.List;
import java.util.ArrayList;


public class TotalMetricsPerLocationPerDeviceJob extends BaseFlinkJob {

    @Override
    protected void createComputationGraph(DataStream<MetricEvent> dataStream) {
        dataStream.keyBy(MetricEvent::location)
                .map(new TotalMetricsPerLocationPerDeviceMapper())
                .print();
    }

    private static class TotalMetricsPerLocationPerDeviceMapper extends RichMapFunction<MetricEvent, Tuple4<String, String, Integer,  List<String>>> {
        private transient MapState<String, List<String>> locationState;


        @Override
        public Tuple4<String, String, Integer, List<String>> map(MetricEvent metricEvent) throws Exception {
            String metricDescription = String.format("%s = %.2f", metricEvent.metric(), metricEvent.value());

            if (locationState.get(metricEvent.device()) == null) {
                List<String> metrics = new ArrayList<>();
                metrics.add(metricDescription);

                locationState.put(metricEvent.device(), metrics);
            } else {
                locationState.get(metricEvent.device()).add(metricDescription);
            }

            List<String> metrics = locationState.get(metricEvent.device());
            return new Tuple4<>(metricEvent.location(), metricEvent.device(), metrics.size(), metrics);
        }


        @Override
        public void open(Configuration parameters) {
            StateTtlConfig stateTtlConfig = StateTtlConfig.newBuilder(Duration.ofSeconds(30))
                    .setUpdateType(StateTtlConfig.UpdateType.OnReadAndWrite)
                    .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                    .build();

            MapStateDescriptor<String, List<String>> descriptor = new MapStateDescriptor<>(
                    "LocationState",
                    TypeInformation.of(new TypeHint<>() {}),
                    TypeInformation.of(new TypeHint<>() {})
            );
            descriptor.enableTimeToLive(stateTtlConfig);

            locationState = getRuntimeContext().getMapState(descriptor);
        }
    }


    public static void main(String[] args) {
        String topic = "metrics";
        String brokers = "localhost:29091,localhost:29092,localhost:29093";

        TotalMetricsPerLocationPerDeviceJob totalMetricsPerLocationPerDeviceJob = new TotalMetricsPerLocationPerDeviceJob();
        totalMetricsPerLocationPerDeviceJob.executeLocal(brokers, topic, 3);
    }

}
