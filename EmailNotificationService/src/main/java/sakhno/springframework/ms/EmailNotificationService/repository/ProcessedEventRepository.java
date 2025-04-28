package sakhno.springframework.ms.EmailNotificationService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sakhno.springframework.ms.EmailNotificationService.entety.ProcessedEventEntity;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {

    ProcessedEventEntity findByMessageId(String messageId);
}
