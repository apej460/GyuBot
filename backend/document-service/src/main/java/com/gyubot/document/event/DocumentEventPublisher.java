package com.gyubot.document.event;

import com.gyubot.document.domain.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String uploadedTopic;
    private final String deletedTopic;

    public DocumentEventPublisher(
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${app.kafka.document-uploaded-topic}") String uploadedTopic,
            @Value("${app.kafka.document-deleted-topic}") String deletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.uploadedTopic = uploadedTopic;
        this.deletedTopic = deletedTopic;
    }

    public void publishUploaded(Document document) {
        kafkaTemplate.send(uploadedTopic, String.valueOf(document.id()), toEvent(document));
    }

    public void publishDeleted(Document document) {
        kafkaTemplate.send(deletedTopic, String.valueOf(document.id()), toEvent(document));
    }

    private DocumentEvent toEvent(Document document) {
        return new DocumentEvent(
                document.id(), document.companyId(), document.s3Key(), document.originalFilename(), document.contentType());
    }
}
