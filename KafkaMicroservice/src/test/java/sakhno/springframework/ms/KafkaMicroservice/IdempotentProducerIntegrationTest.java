package sakhno.springframework.ms.KafkaMicroservice;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sakhno.springframework.ms.KafkaMicroservice.event.ProductCreatedEvent;

import java.util.Map;

@SpringBootTest
public class IdempotentProducerIntegrationTest {

    @Autowired
    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    @MockitoBean
    KafkaAdmin kafkaAdmin;

    @Test
    void testProducerConfig_whenIdempotenceEnabled_assertsIdempotentProperties() {
        ProducerFactory<String, ProductCreatedEvent> producerFactory = kafkaTemplate.getProducerFactory();

        Map<String, Object> config = producerFactory.getConfigurationProperties();

        Assertions.assertEquals("true", config.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
        Assertions.assertTrue("all".equalsIgnoreCase((String) config.get(ProducerConfig.ACKS_CONFIG)));

        if(config.containsKey(ProducerConfig.RETRIES_CONFIG)) {
            Assertions.assertTrue(Integer.parseInt(config.get(ProducerConfig.RETRIES_CONFIG).toString()) > 0);
        }

    }
}
