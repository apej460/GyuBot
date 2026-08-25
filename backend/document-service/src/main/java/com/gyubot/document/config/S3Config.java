package com.gyubot.document.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.time.Duration;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${app.s3.region}") String region,
            @Value("${app.s3.endpoint:}") String endpoint,
            @Value("${app.s3.access-key}") String accessKey,
            @Value("${app.s3.secret-key}") String secretKey,
            @Value("${app.s3.path-style-access:false}") boolean pathStyleAccess) {

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                // MinIO를 상대할 땐 진짜 AWS로 버킷 리전을 재탐색하는 기능이 의미가 없어 꺼둔다.
                .crossRegionAccessEnabled(false)
                .disableMultiRegionAccessPoints(true)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build())
                // MinIO 등 S3 호환 서버가 느리거나 응답하지 않을 때 무한 대기 대신 빨리 실패시킨다.
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5))
                        .build());

        if (endpoint != null && !endpoint.isBlank()) {
            // 로컬 개발용 MinIO 엔드포인트. 실제 AWS를 쓸 때는 이 값을 비워서 기본 AWS 엔드포인트를 쓴다.
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
