package community.flock.wirespec.examples.maven.avro;

import com.eventloopsoftware.TestAvroRecord;
import com.eventloopsoftware.TestAvroRecordChannel;
import org.apache.avro.generic.GenericData;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class AvroExampleService implements TestAvroRecordChannel {

    private static final String TOPIC = "test-avro-record";

    final ProducerFactory<Object, Object> kafkaProducerFactory;

    final ConsumerFactory<Object, Object> kafkaConsumerFactory;

    public AvroExampleService(
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            ProducerFactory<Object, Object> kafkaProducerFactory) {
        this.kafkaConsumerFactory = kafkaConsumerFactory;
        this.kafkaProducerFactory = kafkaProducerFactory;
    }

    @Override
    public void invoke(TestAvroRecord message) {
        var template = new KafkaTemplate<>(kafkaProducerFactory);
        var avro = TestAvroRecord.Avro.to(message);
        template.send(TOPIC, avro);
    }

    public void listen(String groupId, TestAvroRecordChannel listener) {
        var containerProps = new ContainerProperties(TOPIC);
        containerProps.setGroupId(groupId);
        var container = new KafkaMessageListenerContainer<>(kafkaConsumerFactory, containerProps);
        container.setupMessageListener((MessageListener<String, GenericData.Record>) data -> {
            var message = TestAvroRecord.Avro.from(data.value());
            listener.invoke(message);
        });
        container.start();
    }
}
