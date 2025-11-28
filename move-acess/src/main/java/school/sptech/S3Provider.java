package school.sptech;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Provider {

    private static final String awsRegion = System.getenv("AWS_REGION");

    public static S3Client getClient() {

        if (awsRegion == null || awsRegion.isBlank()) {
            throw new IllegalStateException("A variável de ambiente 'AWS_REGION' não está definida.");
        }

        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}