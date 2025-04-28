package sakhno.springframework.ms.KafkaMicroservice.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import sakhno.springframework.ms.KafkaMicroservice.dto.CreateProductDto;
import sakhno.springframework.ms.KafkaMicroservice.event.ProductCreatedEvent;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ProductServiceImpl implements ProductService {
    private KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final static Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Данный метод отправляет событие в кафку в синхронном режиме
     * @param createProductDto - продукт
     * @return - идентификатор
     * @throws ExecutionException - если асинхронная операция завершилась с ошибкой, она будет зафиксирована. Например,
     * проблемы с подключением к Kafka или ошибка сериализации.
     * @throws InterruptedException - возникает, если текущий поток был прерван в процессе ожидания асинхронной операции.
     * Это может происходить, если, например, внешний код решит прервать выполнение текущего потока.
     */
    @Override
    public String createProduct(CreateProductDto createProductDto) throws ExecutionException, InterruptedException {
        //TODO: сохранять в базу
        String productId = UUID.randomUUID().toString();

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(
                productId, createProductDto.getTitle(), createProductDto.getPrice(), createProductDto.getQuantity());

        ProducerRecord<String, ProductCreatedEvent> record = new ProducerRecord<>("product-created-events-topic",
                productId, productCreatedEvent);
        record.headers().add("messageId", UUID.randomUUID().toString().getBytes());
        SendResult<String, ProductCreatedEvent> result = kafkaTemplate.send(record).get();

        log.info("Topic: {}", result.getRecordMetadata().topic());
        log.info("Partition: {}", result.getRecordMetadata().partition());
        log.info("Offset: {}", result.getRecordMetadata().offset());

        log.info("Return: {}", productId);
        return productId;
    }
}
