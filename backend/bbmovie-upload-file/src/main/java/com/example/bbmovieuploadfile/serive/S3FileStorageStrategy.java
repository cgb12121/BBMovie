package com.example.bbmovieuploadfile.serive;

import com.example.bbmovieuploadfile.dto.FileUploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Component("s3")
public class S3FileStorageStrategy implements FileStorageStrategy {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3FileStorageStrategy(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public Mono<FileUploadResult> store(FilePart filePart, String safeName) {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), safeName);

        return filePart.transferTo(tempFile)
                .then(Mono.fromCallable(() -> {
                    PutObjectRequest putRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(safeName)
                            .acl(ObjectCannedACL.PUBLIC_READ)
                            .contentType(Objects.requireNonNull(filePart.headers().getContentType()).toString())
                            .build();

                    s3Client.putObject(putRequest, RequestBody.fromFile(tempFile.toFile()));

                    String url = String.format("https://%s.s3.amazonaws.com/%s", bucketName, safeName);
                    return new FileUploadResult(url, safeName);
                }));
    }

    @Override
    public String getStorageType() {
        return "s3";
    }
}
