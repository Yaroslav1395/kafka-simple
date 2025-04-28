package sakhno.springframework.ms.EmailNotificationService;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestTemplate;
import sakhno.springframework.ms.EmailNotificationService.entety.ProcessedEventEntity;
import sakhno.springframework.ms.EmailNotificationService.event.ProductCreatedEvent;
import sakhno.springframework.ms.EmailNotificationService.handler.ProductCreatedEventHandler;
import sakhno.springframework.ms.EmailNotificationService.repository.ProcessedEventRepository;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@EmbeddedKafka
@SpringBootTest(properties = "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class ProductCreatedEventHandlerIntegrationTest {
    @MockitoBean
    ProcessedEventRepository processedEventRepository;
    @MockitoBean
    RestTemplate restTemplate;
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoSpyBean
    ProductCreatedEventHandler productCreatedEventHandler;

    @Test
    public void testProductCreatedEventHandler_OnProductCreated_HandlesEvent() throws ExecutionException, InterruptedException {
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(
                UUID.randomUUID().toString(),"Test", BigDecimal.ONE, 1);

        String messageId = UUID.randomUUID().toString();
        String messageKey = productCreatedEvent.getProductId();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                "product-created-events-topic", messageKey, productCreatedEvent);

        record.headers().add("messageId", messageId.getBytes());
        record.headers().add(KafkaHeaders.RECEIVED_KEY, messageKey.getBytes());

        ProcessedEventEntity processedEvent = new ProcessedEventEntity();
        when(processedEventRepository.findByMessageId(anyString())).thenReturn(processedEvent);
        when(processedEventRepository.save(any(ProcessedEventEntity.class))).thenReturn(null);

        String responseBody = "{\"key\":\"value\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                any(HttpMethod.class),
                isNull(),
                eq(String.class)
        )).thenReturn(responseEntity);

        kafkaTemplate.send(record).get();

        ArgumentCaptor<String> messageIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ProductCreatedEvent> processedEventCaptor = ArgumentCaptor.forClass(ProductCreatedEvent.class);

        verify(productCreatedEventHandler, timeout(5000).times(1)).handle(
                processedEventCaptor.capture(), messageIdCaptor.capture(), messageKeyCaptor.capture());

        assertEquals(messageId, messageIdCaptor.getValue());
        assertEquals(messageKey, messageKeyCaptor.getValue());
        assertEquals(productCreatedEvent, processedEventCaptor.getValue());
    }
}
