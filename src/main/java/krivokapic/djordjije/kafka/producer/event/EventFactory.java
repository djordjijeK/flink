package krivokapic.djordjije.kafka.producer.event;


public interface EventFactory<T> {
    T generateNextEvent();
}
