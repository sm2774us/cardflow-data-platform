package com.cardflow.platform.lake;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

public class S3LakeSink implements LakeSink {
    private final S3Client s3;
    private final String bucket;

    public S3LakeSink(S3Client s3, String bucket) { this.s3 = s3; this.bucket = bucket; }

    @Override
    public String write(String key, byte[] data) {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/x-ndjson")
                .contentEncoding("gzip").serverSideEncryption(ServerSideEncryption.AES256).build(), RequestBody.fromBytes(data));
        return "s3://" + bucket + "/" + key;
    }
}
