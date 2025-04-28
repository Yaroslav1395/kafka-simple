## Kafka Producer Configuration

В проекте используется Kafka для отправки сообщений. Ниже приведены основные настройки, указанные в `application.properties`

### 🔧 Конфигурация продюсера (`spring.kafka.producer`)

| Параметр                                          | Значение                                                                                    | Описание                                                                                                            |
|---------------------------------------------------|---------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `bootstrap-servers`                               | `localhost:9092,localhost:9094`                                                             | Адреса Kafka-брокеров, к которым подключается продюсер. Поддерживается отказоустойчивость.                          |
| `key-serializer`                                  | `org.apache.kafka.common.serialization.StringSerializer`                                    | Сериализатор для ключей сообщений. Используется строковый сериализатор.                                             |
| `value-serializer`                                | `org.springframework.kafka.support.serializer.JsonSerializer`                               | Сериализатор для значений сообщений. Используется сериализация в JSON.                                              |
| `acks`                                            | `all`                                                                                       | Продюсер ждет подтверждения от всех реплик перед завершением отправки. Обеспечивает надежную доставку.              |
| `properties.delivery.timeout.ms`                  | `20000`                                                                                     | Максимальное время (в мс), в течение которого Kafka должна доставить сообщение.                                     |
| `properties.linger.ms`                            | `0`                                                                                         | Время ожидания перед отправкой пакета сообщений. `0` означает мгновенную отправку.                                  |
| `properties.request.timeout.ms`                   | `10000`                                                                                     | Таймаут запроса к Kafka в мс. Если за это время не получен ответ — возникает ошибка.                                |
| `properties.enable.idempotence`                   | `true`                                                                                      | Включение идемпотентности (предотвращение дублирующей отправки). Обеспечивает exactly-once семантику.               |
| `properties.max.in.flight.request.per.connection` | `5`                                                                                         | Максимальное количество сообщений, отправленных, но ещё не подтверждённых.                                          |
| `properties.spring.json.type.mapping`             | `productCreatedEvent:sakhno.springframework.ms.KafkaMicroservice.event.ProductCreatedEvent` | Связывает тип события (`productCreatedEvent`) с Java-классом, который будет использоваться при десериализации JSON. |

### 💡 Примечания
- Некоторые параметры, такие как `retries` и `retry.backoff.ms`, закомментированы, но их можно активировать для более устойчивой доставки сообщений в случае временных сбоев.
- Идемпотентность (`enable.idempotence`) рекомендуется включать при `acks=all` для предотвращения дублирующих сообщений.
- `JsonSerializer` позволяет сериализовать сложные объекты и поддерживает типизацию через `type.mapping`.
- `spring.kafka.producer.properties.delivery.timeout.ms` означает, что Kafka producer будет пытаться доставить сообщение максимум 20 секунд, включая все возможные попытки ретраев, и если за это время доставка не произойдёт — будет выброшено исключение (timeout). Это общий лимит времени на доставку одного сообщения. Он включает в себя: Время ожидания ответа от Kafka (настраивается через request.timeout.ms), Задержки между ретраями (retry.backoff.ms), Количество попыток (retries) 
---

## Пример конфигурации в `application.properties`:

```properties
spring.kafka.producer.bootstrap-servers=localhost:9092,localhost:9094
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
#spring.kafka.producer.retries=10
#spring.kafka.producer.properties.retry.backoff.ms=1000
spring.kafka.producer.properties.delivery.timeout.ms=20000
spring.kafka.producer.properties.linger.ms=0
spring.kafka.producer.properties.request.timeout.ms=10000
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.producer.properties.max.in.flight.request.per.connection=5
spring.kafka.producer.properties.spring.json.type.mapping=productCreatedEvent:sakhno.springframework.ms.KafkaMicroservice.event.ProductCreatedEvent
```


## Kafka Consumer Configuration

В проекте используется Kafka для получения сообщений. Ниже приведены основные настройки, указанные в `application.properties`

### 🔧 Конфигурация потребителя (`spring.kafka.consumer`)

| Параметр                                      | Значение                                                                                           | Описание                                                                                                                                  |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `bootstrap-servers`                           | `localhost:9092,localhost:9094`                                                                    | Адреса Kafka-брокеров, к которым подключается потребитель. Поддерживается отказоустойчивость.                                             |
| `group-id`                                    | `product-created-events`                                                                           | Идентификатор группы потребителей. Все потребители с одинаковым `group-id` будут делить нагрузку по разделению партиций.                  |
| `properties.spring.json.trusted.packages`     | `sakhno.springframework.ms.EmailNotificationService.event`                                         | Указывает список доверенных пакетов для десериализации сообщений. Это необходимо для защиты от нежелательной десериализации классов.      |
| `properties.spring.json.type.mapping`         | `productCreatedEvent:sakhno.springframework.ms.EmailNotificationService.event.ProductCreatedEvent` | Связывает тип события (`productCreatedEvent`) с Java-классом, который будет использоваться при десериализации JSON.                       |
| `auto-offset-reset`                           | `latest`                                                                                           | Устанавливает, что делать, если смещение не найдено: `latest` означает чтение сообщений, начиная с последнего доступного смещения.        |

### 💡 Примечания
- `auto-offset-reset=latest` означает, что если смещение для данного потребителя не найдено, он начнёт чтение сообщений с самого последнего доступного смещения. Можно использовать `earliest` для чтения с самого начала.
- Параметр `properties.spring.json.trusted.packages` обеспечивает безопасность, ограничивая десериализацию только теми классами, которые находятся в указанных пакетах.
- Для десериализации сложных объектов через JSON используется `JsonDeserializer`, который работает с типами Java-классов, указанными в `spring.kafka.consumer.properties.spring.json.type.mapping`.
- Параметры десериализации через `spring.kafka.consumer.properties` могут быть настроены в зависимости от структуры сообщений и типов данных, которые используются в проекте.

## Пример конфигурации в `application.properties`:

```properties
spring.kafka.consumer.bootstrap-servers=localhost:9092,localhost:9094
spring.kafka.consumer.group-id=product-created-events
spring.kafka.consumer.properties.spring.json.trusted.packages=sakhno.springframework.ms.EmailNotificationService.event
spring.kafka.consumer.properties.spring.json.type.mapping=productCreatedEvent:sakhno.springframework.ms.EmailNotificationService.event.ProductCreatedEvent
spring.kafka.consumer.auto-offset-reset=latest
```



