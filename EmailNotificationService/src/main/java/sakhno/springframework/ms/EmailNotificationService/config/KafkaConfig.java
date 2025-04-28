package sakhno.springframework.ms.EmailNotificationService.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import sakhno.springframework.ms.EmailNotificationService.exception.NonRetryableException;
import sakhno.springframework.ms.EmailNotificationService.exception.RetryableException;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    private final Environment environment;

    @Autowired
    public KafkaConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Метод создает словарь с конфигурацией для kafka consumer
     * @return - словарь с настройками
     */
    @Bean
    ConsumerFactory<String, Object> consumerFactory(){
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.consumer.bootstrap-servers"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES,
                environment.getProperty("spring.kafka.consumer.group-id"));
        config.put(ConsumerConfig.GROUP_ID_CONFIG,
                environment.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages"));
        config.put("spring.json.type.mapping",
                environment.getProperty("spring.kafka.consumer.properties.spring.json.type.mapping"));
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                environment.getProperty("spring.kafka.consumer.auto-offset-reset"));
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Этот бин создает и настраивает фабрику контейнера слушателя Kafka для обработки сообщений.
     * <p>
     * <b>DefaultErrorHandler</b> — используется для обработки ошибок во время обработки сообщений. В данном случае ошибка будет
     * сначала публиковаться в Dead Letter Queue через DeadLetterPublishingRecoverer. Это происходит, если сообщение не
     * может быть обработано в несколько попыток.
     * </p>
     * <p>
     * <b>FixedBackOff(3000, 3)</b>: определяет, что будет происходить в случае ошибки. В данном случае, если ошибка возникла,
     * будет сделано три попытки через интервал в 3000 миллисекунд (3 секунды).
     * </p>
     * <p>
     * <b>DeadLetterPublishingRecoverer(kafkaTemplate)</b>: позволяет отправить неудачные сообщения в специальный "мертвый"
     * топик (Dead Letter Queue) для дальнейшего анализа.
     * </p>
     * <p>
     * <b>addNotRetryableExceptions(NonRetryableException.class)</b> — сообщает обработчику ошибок, что исключения типа
     * NonRetryableException не должны повторно обрабатываться. Эти исключения сразу же приводят к отправке сообщения в
     * Dead Letter Queue.
     * </p>
     * <p>
     * <b>addRetryableExceptions(RetryableException.class)</b> — сообщает обработчику ошибок, что исключения типа
     * RetryableException должны привести к повторной попытке обработки сообщения.
     * </p>
     * <p>
     * <b>ConcurrentKafkaListenerContainerFactory</b> — фабрика, которая используется для создания контейнеров слушателей Kafka.
     * В данном случае фабрика настроена на работу с типами сообщений String для ключа и Object для значения.
     * </p>
     * @param consumerFactory Фабрика, которая создает потребителей Kafka, используется для настройки конфигурации потребителя.
     * @param kafkaTemplate Используется для публикации сообщений в Dead Letter Queue (DLQ) в случае ошибок обработки.
     * @return Настроенная фабрика контейнера слушателя Kafka, которая использует обработчик ошибок.
     */
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory, KafkaTemplate kafkaTemplate) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(3000, 3));
        errorHandler.addNotRetryableExceptions(NonRetryableException.class);
        errorHandler.addRetryableExceptions(RetryableException.class);
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /**
     * Метод создает словарь с конфигурацией для kafka producer
     * @return - словарь с настройками
     */
    @Bean
    ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.consumer.bootstrap-servers"));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Бин KafkaTemplate для отправки сообщений типа Object с ключом типа String.
     * Использует фабрику продюсера для создания и настройки Kafka-продюсеров.
     */
    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}
