package com.gyubot.document.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/*
 * 원본 파일을 S3(로컬 개발은 MinIO)에 저장한다. 버킷 생성/존재 확인을 앱 시작 시 한 번 해두는 건
 * user-service의 FileStorageService가 로컬 디렉터리를 미리 만들어두는 것과 같은 발상이다.
 */
@Component
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3StorageService(S3Client s3Client, @Value("${app.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    public String upload(MultipartFile file, String key) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ResponseInputStream<GetObjectResponse> download(String key) {
        return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public static String newKey(Long companyId, String originalFilename) {
        String extension = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) extension = originalFilename.substring(dot);
        }
        return "documents/" + companyId + "/" + UUID.randomUUID() + extension;
    }
}
