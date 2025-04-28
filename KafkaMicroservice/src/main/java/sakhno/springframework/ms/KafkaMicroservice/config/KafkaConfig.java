package sakhno.springframework.ms.KafkaMicroservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import sakhno.springframework.ms.KafkaMicroservice.event.ProductCreatedEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerializer;

    @Value("${spring.kafka.producer.value-serializer}")
    private String valueSerializer;

    @Value("${spring.kafka.producer.acks}")
    private String acks;

    @Value("${spring.kafka.producer.properties.delivery.timeout.ms}")
    private String deliveryTimeout;

    @Value("${spring.kafka.producer.properties.linger.ms}")
    private String linger;

    @Value("${spring.kafka.producer.properties.request.timeout.ms}")
    private String requestTimeout;

    @Value("${spring.kafka.producer.properties.enable.idempotence}")
    private String idempotence;

    @Value("${spring.kafka.producer.properties.max.in.flight.request.per.connection}")
    private String maxInFlightRequests;

    @Value("${spring.kafka.producer.properties.spring.json.type.mapping}")
    private String typeMapping;

    /**
     * Метод создает словарь с конфигурацией для kafka producer
     * @return - словарь с настройками
     */
    Map<String, Object> producerConfigs() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        config.put(ProducerConfig.ACKS_CONFIG, acks);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeout);
        config.put(ProducerConfig.LINGER_MS_CONFIG, linger);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotence);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequests);
        config.put("spring.json.type.mapping", typeMapping);
        return config;
    }

    /**
     * Метод создает экземпляр с фабрикой по созданию kafka producer. Для создания необходим словарь с настройками
     * продюсера
     * @return - фабрика по созданию kafka producer
     */
    @Bean
    ProducerFactory<String, ProductCreatedEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * Бин KafkaTemplate для отправки сообщений типа ProductCreatedEvent с ключом типа String.
     * Использует фабрику продюсера для создания и настройки Kafka-продюсеров.
     */
    @Bean
    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Бин для создания Kafka-топика с именем "product-created-events-topic".
     * Топик имеет 3 партиции, 3 реплики и настраиваемое количество реплик, которые должны быть синхронизированы для
     * обеспечения отказоустойчивости.
     */
    @Bean
    NewTopic createTopic() {
        return TopicBuilder
                .name("product-created-events-topic")
                .partitions(3)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2"))
                .build();
    }
}
