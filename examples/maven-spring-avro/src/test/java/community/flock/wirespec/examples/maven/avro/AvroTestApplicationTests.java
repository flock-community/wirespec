package community.flock.wirespec.examples.maven.avro;

import com.eventloopsoftware.*;
import com.eventloopsoftware.kafka.model.TestAvroMetadata;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@Testcontainers
class AvroTestApplicationTests {

    private static final Network NETWORK = Network.newNetwork();

    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1")).withNetwork(NETWORK);

    @Container
    private static final GenericContainer SCHEMA_CONTAINER = new GenericContainer(DockerImageName.parse("confluentinc/cp-schema-registry:7.7.1")).withNetwork(NETWORK).dependsOn(KAFKA_CONTAINER).withExposedPorts(8081).withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry").withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081").withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://" + KAFKA_CONTAINER.getNetworkAliases().get(0) + ":9092").waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    private static final Supplier<String> schemaUrl = () -> "http://" + SCHEMA_CONTAINER.getHost() + ":" + SCHEMA_CONTAINER.getFirstMappedPort();

    @Autowired
    private AvroExampleService service;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.properties.schema.registry.url", schemaUrl::get);
    }

    @Test
    void sendTest() throws InterruptedException {

        var latch = new CountDownLatch(2);

        var record = new TestAvroRecord(
                new TestAvroMetadata("321", 1L),
                new TestAvroOrder(
                        "123",
                        Optional.of(1L),
                        List.of(
                                new TestAvroRefNumber("ref1", TestAvroEnumIdentifier.REF_1),
                                new TestAvroRefNumber("ref2", TestAvroEnumIdentifier.REF_2),
                                new TestAvroRefNumber("ref3", TestAvroEnumIdentifier.REF_3)

                        ),
                        List.of(
                                new TestAvroOrderLines("11", "100"),
                                new TestAvroOrderLines("22", "200"),
                                new TestAvroOrderLines("33", "300")
                        ),
                        3L
                )
        );

        service.listen("group1", message -> {
            assertEquals(record, message);
            latch.countDown();
        });

        service.listen("group2", message -> {
            assertEquals(record, message);
            latch.countDown();
        });

        service.invoke(record);

        boolean messageConsumed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(messageConsumed);

    }
}
