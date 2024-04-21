package krivokapic.djordjije.kafka.producer.event;

import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.Locale;
import java.util.HashMap;
import java.time.Instant;


public final class MetricEventFactory implements EventFactory<MetricEvent> {
    private static final int OK = 0;
    private static final int SHORT_ALARM = 1;
    private static final int LONG_ALARM = 2;

    private static final List<String> DEFAULT_LOCATIONS = List.of(
            "ATL1", "PEK2", "DXB1",
            "LAX3", "HND3", "GRU1",
            "LHR2", "CDG2", "AMS3",
            "DEL4", "CAN5", "FRA2",
            "IST1", "SIN2", "SFO1",
            "JFK1", "DFW3", "SYD2",
            "NRT1", "YYZ3"
    );

    private static final List<String> DEVICES = List.of("device-1", "device-2", "device-3");

    private static final List<String> METRICS = List.of("Cpu Utilization", "Memory Utilization", "Network Latency");

    private static final double[][] TRANSITION_MATRIX = {
            // OK, SHORT ALARM, LONG ALARM
            {0.95, 0.02, 0.03},  // Transitions from OK
            {0.85, 0.15, 0.00},  // Transitions from short ALARM
            {0.10, 0.00, 0.90}   // Transitions from long ALARM
    };

    private final Random random;
    private final List<String> locations;
    private final Map<String, State> statesCache;


    public static MetricEventFactory create() {
        return new MetricEventFactory();
    }


    public static MetricEventFactory create(List<String> locations) {
        return new MetricEventFactory(locations);
    }


    private MetricEventFactory() {
        this.random = new Random();
        this.statesCache = new HashMap<>();

        this.locations = DEFAULT_LOCATIONS;
    }


    private MetricEventFactory(List<String> locations) {
        this.random = new Random();
        this.statesCache = new HashMap<>();

        this.locations = locations;
    }


    @Override
    public MetricEvent generateNextEvent() {
        String location = selectRandomElement(this.locations);
        String device = String.format("%s-%s", location.toLowerCase(Locale.ROOT), selectRandomElement(DEVICES));
        String metric = selectRandomElement(METRICS);

        String cacheKey = String.format("%s|%s|%s", location, device, metric);
        State previousState = statesCache.getOrDefault(cacheKey, null);

        double value = 0.0;
        switch (metric) {
            case "Cpu Utilization":
                if (previousState == null) {
                    value = generateCpuUtilizationData(new State(0, OK));
                    statesCache.put(cacheKey, new State(value, OK));
                } else {
                    int currentState = getNextState(previousState.status);

                    if (currentState == OK) {
                        value = generateCpuUtilizationData(previousState);
                    } else if (currentState == SHORT_ALARM || currentState == LONG_ALARM) {
                        value = generateCpuUtilizationData(previousState);
                    }

                    statesCache.put(cacheKey, new State(value, currentState));
                }
                break;
            case "Memory Utilization":
                if (previousState == null) {
                    value = generateMemoryUtilizationData(new State(0, OK));
                    statesCache.put(cacheKey, new State(value, OK));
                } else {
                    int currentState = getNextState(previousState.status);

                    if (currentState == OK) {
                        value = generateMemoryUtilizationData(previousState);
                    } else if (currentState == SHORT_ALARM || currentState == LONG_ALARM) {
                        value = generateMemoryUtilizationData(previousState);
                    }

                    statesCache.put(cacheKey, new State(value, currentState));
                }
                break;
            case "Network Latency":
                if (previousState == null) {
                    value = generateNetworkLatencyData(new State(0, OK));
                    statesCache.put(cacheKey, new State(value, OK));
                } else {
                    int currentState = getNextState(previousState.status);

                    if (currentState == OK) {
                        value = generateNetworkLatencyData(previousState);
                    } else if (currentState == SHORT_ALARM || currentState == LONG_ALARM) {
                        value = generateNetworkLatencyData(previousState);
                    }

                    statesCache.put(cacheKey, new State(value, currentState));
                }
                break;
        }

        return MetricEvent.builder()
                .location(location)
                .device(device)
                .metric(metric)
                .value(value)
                .timestamp(random.nextDouble() <= 0.03 ? Instant.now().minusSeconds(30) : Instant.now())
                .build();
    }


    private double generateCpuUtilizationData(State state) {
        double value = state.status == OK ? 75 * random.nextDouble() : 75 + (25 * random.nextDouble());
        return Math.round(value * 100.0) / 100.0;
    }


    private double generateNetworkLatencyData(State state) {
        double value = state.status == OK ? 100 * random.nextDouble() : 100 + (400 * random.nextDouble());
        return Math.round(value * 100.0) / 100.0;
    }


    private double generateMemoryUtilizationData(State state) {
        double value = state.value;
        int transitionToStatus = state.status;

        double maxIncrementOk = 1;
        double maxIncrementAlarm = 3.5;
        double decrementFactor = 7;

        if (transitionToStatus == OK) {
            double proposedValue = value + (random.nextDouble() * maxIncrementOk);
            value = Math.min(proposedValue, 70);
        } else if (transitionToStatus == SHORT_ALARM || transitionToStatus == LONG_ALARM) {
            double proposedValue = value + (random.nextDouble() * maxIncrementAlarm);
            value = Math.min(proposedValue, 100);
        }

        if (random.nextDouble() < 0.10) {
            double proposedValue = value - (random.nextDouble() * decrementFactor);
            value = Math.max(proposedValue, 0);
        }

        return value;
    }


    private String selectRandomElement(List<String> list) {
        return list.get(random.nextInt(list.size()));
    }


    private int getNextState(int currentState) {
        double rand = random.nextDouble();
        double cumulativeProbability = 0.0;

        for (int i = 0; i < TRANSITION_MATRIX[currentState].length; i++) {
            cumulativeProbability += TRANSITION_MATRIX[currentState][i];

            if (rand < cumulativeProbability) {
                return i;
            }
        }

        return currentState;
    }


    private static class State {
        public double value;
        public int status;


        public State(double value, int status) {
            this.value = value;
            this.status = status;
        }
    }
}
