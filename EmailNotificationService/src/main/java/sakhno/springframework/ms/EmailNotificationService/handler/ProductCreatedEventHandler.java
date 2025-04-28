package sakhno.springframework.ms.EmailNotificationService.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import sakhno.springframework.ms.EmailNotificationService.entety.ProcessedEventEntity;
import sakhno.springframework.ms.EmailNotificationService.exception.NonRetryableException;
import sakhno.springframework.ms.EmailNotificationService.exception.RetryableException;
import sakhno.springframework.ms.EmailNotificationService.event.ProductCreatedEvent;
import sakhno.springframework.ms.EmailNotificationService.repository.ProcessedEventRepository;

@Component
@KafkaListener(topics = "product-created-events-topic")
public class ProductCreatedEventHandler {
    private RestTemplate restTemplate;
    private ProcessedEventRepository processedEventRepository;
    private final static Logger log = LoggerFactory.getLogger(ProductCreatedEventHandler.class);

    @Autowired
    public ProductCreatedEventHandler(RestTemplate restTemplate, ProcessedEventRepository processedEventRepository) {
        this.restTemplate = restTemplate;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Обработчик событий, поступающих через Kafka, для создания и обработки события `ProductCreatedEvent`.
     * @param productCreatedEvent Событие, которое было получено из Kafka.
     * @param messageId Идентификатор сообщения, который используется для проверки на дублирование.
     * @param messageKey Ключ сообщения, который может быть использован для идентификации или маршрутизации.
     */
    @Transactional
    @KafkaHandler
    public void handle(@Payload ProductCreatedEvent productCreatedEvent, @Header("messageId") String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
        log.info("Received event: {}", productCreatedEvent.getTitle());

        String url = "http://localhost:8090/response/200";
        ProcessedEventEntity processedEvent = processedEventRepository.findByMessageId(messageId);

        if(processedEvent != null) {
            log.info("Duplicate message id: {}", messageId);
            return;
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if(response.getStatusCode().value() == HttpStatus.OK.value()) {
                log.info("Received response: {}", response.getBody());
            }
        }catch (ResourceAccessException e) {
            log.error(e.getMessage());
            throw new RetryableException(e);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new NonRetryableException(e);
        }

        try {
            processedEventRepository.save(new ProcessedEventEntity(messageId, messageKey));
        } catch (DataIntegrityViolationException e) {
            log.error(e.getMessage());
            throw new NonRetryableException(e);
        }

    }
}
