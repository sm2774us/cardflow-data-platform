package com.cardflow.platform.lake;

import com.cardflow.platform.config.AppProperties;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class LakeConfig {
    @Bean
    @ConditionalOnProperty(name = "cardflow.lake.type", havingValue = "local", matchIfMissing = true)
    LakeSink localSink(AppProperties p) { return new LocalLakeSink(p.lake().localDir()); }

    @Bean
    @ConditionalOnProperty(name = "cardflow.lake.type", havingValue = "s3")
    LakeSink s3Sink(AppProperties p) {
        var b = S3Client.builder().region(Region.of(p.lake().region()));
        String endpoint = p.lake().s3Endpoint();
        if (endpoint != null && !endpoint.isBlank()) {   // LocalStack / MinIO
            b.endpointOverride(URI.create(endpoint)).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return new S3LakeSink(b.build(), p.lake().bucket());
    }
}
