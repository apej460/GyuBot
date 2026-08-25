package com.gyubot.document.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/*
 * Spring Boot는 더 이상 Kafka HealthIndicator를 자동으로 붙여주지 않는다
 * (매 헬스체크마다 describeCluster()를 호출하는 비용 때문에 기본에서 빠짐).
 * document-service의 핵심 기능이 Kafka 발행이라 다른 서비스들처럼 실제 연결 상태를
 * /actuator/health에서 바로 확인할 수 있게 직접 추가한다.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            client.describeCluster().nodes().get(3, TimeUnit.SECONDS);
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
