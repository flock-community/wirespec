package community.flock.wirespec.examples.maven.avro;

import com.eventloopsoftware.kafka.channel.TestAvroRecord;
import org.apache.avro.generic.GenericData;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class AvroExampleService implements TestAvroRecord {

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
    public void invoke(com.eventloopsoftware.kafka.model.TestAvroRecord message) {
        var template = new KafkaTemplate<>(kafkaProducerFactory);
        var avro = com.eventloopsoftware.kafka.avro.TestAvroRecordAvro.to(message);
        template.send(TOPIC, avro);
    }

    public void listen(String groupId, TestAvroRecord listener) {
        var containerProps = new ContainerProperties(TOPIC);
        containerProps.setGroupId(groupId);
        var container = new KafkaMessageListenerContainer<>(kafkaConsumerFactory, containerProps);
        container.setupMessageListener((MessageListener<String, GenericData.Record>) data -> {
            var message = com.eventloopsoftware.kafka.avro.TestAvroRecordAvro.from(data.value());
            listener.invoke(message);
        });
        container.start();
    }
}
