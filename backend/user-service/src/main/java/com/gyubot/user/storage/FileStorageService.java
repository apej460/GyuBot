package com.gyubot.user.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/*
 * 가입 신청 첨부파일(명함·재직증명서)을 로컬 디스크에 저장한다.
 * document-service가 S3 저장을 담당하게 되면 이 클래스를 그쪽 호출로 교체하면 된다.
 */
@Component
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String dir) {
        this.root = Path.of(dir, "signup");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public StoredFile store(MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + extension;
        Path target = root.resolve(storedName);
        try (var input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new StoredFile(file.getOriginalFilename(), target.toString());
    }

    public Resource load(String path) {
        return new FileSystemResource(path);
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) return "";
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : "";
    }

    public record StoredFile(String originalFilename, String storedPath) {
    }
}
