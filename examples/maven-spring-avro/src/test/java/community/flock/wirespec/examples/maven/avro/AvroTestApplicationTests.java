package community.flock.wirespec.examples.maven.avro;

import com.eventloopsoftware.kafka.model.TestAvroEnumIdentifier;
import com.eventloopsoftware.kafka.model.TestAvroOrder;
import com.eventloopsoftware.kafka.model.TestAvroOrderLines;
import com.eventloopsoftware.kafka.model.TestAvroRecord;
import com.eventloopsoftware.kafka.model.TestAvroRefNumber;
import com.eventloopsoftware.kafka.metadata.model.TestAvroMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
                        "QR Code".getBytes(),
                        Optional.of(1L),
                        List.of(
                                new TestAvroRefNumber("ref1", TestAvroEnumIdentifier.REF_1),
                                new TestAvroRefNumber("ref2", TestAvroEnumIdentifier.REF_2),
                                new TestAvroRefNumber("ref3", TestAvroEnumIdentifier.REF_3)

                        ),
                        List.of(
                                new TestAvroOrderLines("11", 100.0F),
                                new TestAvroOrderLines("22", 200.0F),
                                new TestAvroOrderLines("33", 300.0F)
                        ),
                        3L
                )
        );

        service.listen("group1", message -> {
            assertRecordEquals(record, message);
            latch.countDown();
        });

        service.listen("group2", message -> {
            assertRecordEquals(record, message);

            latch.countDown();
        });

        service.invoke(record);

        boolean messageConsumed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(messageConsumed);

    }

    void assertRecordEquals(TestAvroRecord expected, TestAvroRecord actual){
        assertEquals(expected.metadata(), actual.metadata());
        assertEquals(expected.order().number(), actual.order().number());
        assertEquals(expected.order().a_number(), actual.order().a_number());
        assertEquals(expected.order().created_at(), actual.order().created_at());
        assertEquals(expected.order().lines(), actual.order().lines());
        assertArrayEquals(expected.order().qr_code(), actual.order().qr_code());
    }
}
