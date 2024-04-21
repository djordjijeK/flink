package krivokapic.djordjije.kafka.producer.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;
import java.io.Serializable;


@JsonDeserialize(builder = MetricEvent.MetricEventBuilder.class)
public final class MetricEvent implements Serializable {
    private final String location;
    private final String device;
    private final String metric;
    private final double value;
    private final Instant instant;


    private MetricEvent(String location, String device, String metric, double value, Instant instant) {
        this.location = location;
        this.device = device;
        this.metric = metric;
        this.value = value;
        this.instant = instant;
    }


    @JsonProperty("location")
    public String location() {
        return this.location;
    }


    @JsonProperty("device")
    public String device() {
        return this.device;
    }


    @JsonProperty("metric")
    public String metric() {
        return this.metric;
    }


    @JsonProperty("value")
    public double value() {
        return this.value;
    }


    @JsonProperty("timestamp")
    public Instant timestamp() {
        return this.instant;
    }


    @Override
    public String toString() {
        return String.format("Location: %s | Device: %s | Metric: %s | Value: %.2f | Timestamp: %s", location, device, metric, value, instant);
    }


    public static MetricEventBuilder builder() {
        return new MetricEventBuilder();
    }


    public static class MetricEventBuilder {
        private String builderLocation;
        private String builderDevice;
        private String builderMetric;
        private double builderValue;
        private Instant builderTimestamp;


        @JsonProperty("location")
        public MetricEventBuilder location(String location) {
            if (location == null || location.isBlank()) {
                throw new IllegalStateException("Location cannot be null or empty");
            }

            builderLocation = location;
            return this;
        }


        @JsonProperty("device")
        public MetricEventBuilder device(String device) {
            if (device == null || device.isBlank()) {
                throw new IllegalStateException("Device cannot be null or empty");
            }

            builderDevice = device;
            return this;
        }


        @JsonProperty("metric")
        public MetricEventBuilder metric(String metric) {
            if (metric == null || metric.isBlank()) {
                throw new IllegalStateException("Metric cannot be null or empty");
            }

            builderMetric = metric;
            return this;
        }


        @JsonProperty("value")
        public MetricEventBuilder value(double value) {
            builderValue = value;
            return this;
        }


        @JsonProperty("timestamp")
        public MetricEventBuilder timestamp(Instant timestamp) {
            if (timestamp == null) {
                throw new IllegalStateException("Metric timestamp cannot be null");
            }

            builderTimestamp = timestamp;
            return this;
        }


        public MetricEvent build() {
            if (builderLocation == null || builderLocation.isBlank() || builderDevice == null || builderDevice.isBlank() || builderMetric == null || builderMetric.isBlank() || builderTimestamp == null) {
                throw new IllegalStateException("Metric data validation failed. Check all required properties.");
            }

            return new MetricEvent(builderLocation, builderDevice, builderMetric, builderValue, builderTimestamp);
        }
    }
}
