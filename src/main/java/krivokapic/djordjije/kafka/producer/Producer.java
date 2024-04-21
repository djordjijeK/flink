package krivokapic.djordjije.kafka.producer;


public interface Producer<T> {
    void send(T event);

    void start();
}
