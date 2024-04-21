package krivokapic.djordjije.kafka.producer;

import krivokapic.djordjije.kafka.producer.event.EventFactory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.Closeable;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;


abstract class BaseProducer<T> implements Closeable, Producer<T> {
    private final static Logger logger = LogManager.getLogger(BaseProducer.class);

    private final int threads;
    private final int maxJitter;

    private final Random random;

    private final LongAdder eventsProduced;
    private final TimeUnit timeUnit;
    private final EventFactory<T> eventFactory;
    private final ScheduledExecutorService scheduledExecutorService;

    private Instant start;


    protected BaseProducer(EventFactory<T> eventFactory, int threads, int maxJitter, TimeUnit timeUnit) {
        this.threads = threads;
        this.maxJitter = maxJitter;

        this.random = new Random();

        this.timeUnit = timeUnit;
        this.eventFactory = eventFactory;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(threads);

        this.start = Instant.MIN;
        this.eventsProduced = new LongAdder();
    }


    @Override
    public void close() {
        try {
            logger.info("Closing the producer...");
            this.scheduledExecutorService.shutdown();
            logger.info("Producer successfully closed");
        } catch (Exception e) {
            logger.error("Failed to close the producer", e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public void start() {
        this.start = Instant.now();

        for (int i = 0; i < this.threads; i++) {
            this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                T event = this.eventFactory.generateNextEvent();
                logger.debug("Generated event: {}", event);

                this.eventsProduced.increment();
                send(event);
            }, 0, this.random.nextInt(this.maxJitter) + 1, this.timeUnit);
        }
    }
}
