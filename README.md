## Apache Flink

- Apache Flink is a distributed data processing system for stateful computations over unbounded and bounded data streams.


-  Flink operates in a distributed computing environment and consists of a cluster of nodes. Each cluster is composed of a JobManager and one or more TaskManagers:

    - **JobManager:** Controls the execution of jobs, allocates tasks to TaskManagers, coordinates checkpoints, and handles failures and recovery. It acts as the master node in the Flink cluster.

    - **TaskManager:** Executes tasks (sub-parts of a job), maintains buffers for data exchange between tasks, and reports status updates back to the JobManager. Each TaskManager can execute multiple tasks concurrently, depending on the available resources.


- Flink processes data streams as continuous flows, which can be either unbounded (without a defined end, such as live sensor data) or bounded (with a defined start and end, like batch files). 

    - **Windows:** Flink provides powerful windowing capabilities to manage streams by time, count, or session activity, enabling the development of complex temporal data patterns.

    - **State Management:** Flink’s stateful processing capabilities allow it to remember past events and based on that to compute aggregations or join streams. It supports various state backends (like RocksDB or in-memory) that can scale and recover automatically.


- Flink provides strong fault tolerance through its distributed snapshot mechanism, which periodically captures the state of all tasks in a job. In the event of a failure, Flink can restore the state and resume operations from the last successful snapshot, minimizing data loss.

    - **Checkpoints:** Configurable and can be fine-tuned for frequency and consistency according to the requirements of the job, balancing between performance and fault tolerance.


------

Create kafka cluster:

```bash
$ docker compose --profile kafka-cluster up
```

Start the kafka admin application:

```java
public class KafkaAdminApplication {

    public static void main(String[] args) {
        String topic = "metrics";
        String brokers = "localhost:29091,localhost:29092,localhost:29093";

        KafkaAdminClient kafkaAdminClient = new KafkaAdminClient(brokers);
        kafkaAdminClient.createTopic(topic, 3, 3);
        // kafkaAdminClient.increaseTopicPartitions(topic, 10);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(kafkaAdminClient::describeTopics, 0, 60, TimeUnit.SECONDS);
    }

}
```

Start the kafka producer:

```java
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
```

Start Flink cluster:

```bash
$ docker compose --profile flink-cluster up
```

Start the jobs from the `src/main/java/krivokapic/djordjije/flink/jobs` directory.
To execute jobs on cluster:

```java
String topic = "metrics";
String clusterBrokers = "kafka-1:29091,kafka-2:29092,kafka-3:29093";
<job>.executeOnCluster("localhost", 38081, clusterBrokers, topic, 3);
```