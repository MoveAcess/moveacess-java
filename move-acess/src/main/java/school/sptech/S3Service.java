package school.sptech;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class S3Service {

    private final S3Client s3_client;

    public S3Service() {
        this.s3_client = S3Provider.getClient();
    }

    public InputStream getFileAsInputStream(String bucket_name, String file_key) {
        System.out.printf("Iniciando download do arquivo '%s' do bucket '%s'...%n", file_key, bucket_name);
        try {
            GetObjectRequest get_object_request = GetObjectRequest.builder()
                    .bucket(bucket_name)
                    .key(file_key)
                    .build();
            return s3_client.getObject(get_object_request);
        } catch (S3Exception e) {
            System.err.println("Erro ao buscar arquivo no S3: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Falha ao buscar arquivo no S3.", e);
        }
    }

    public Optional<String> getLatestFileKey(String bucket_name, String suffix) {
        System.out.println("Procurando o arquivo mais recente no bucket: " + bucket_name);
        try {
            ListObjectsV2Request list_req = ListObjectsV2Request.builder()
                    .bucket(bucket_name)
                    .build();

            List<S3Object> objects = s3_client.listObjectsV2(list_req).contents();

            if (objects.isEmpty()) {
                return Optional.empty();
            }

            S3Object latest_object_found = null;
            for (int i = 0; i < objects.size(); i++) {
                S3Object obj = objects.get(i);
                if (obj.key().toLowerCase().endsWith(suffix)) {
                    if (latest_object_found == null || obj.lastModified().isAfter(latest_object_found.lastModified())) {
                        latest_object_found = obj;
                    }
                }
            }

            Optional<S3Object> latest_object = Optional.ofNullable(latest_object_found);

            return latest_object.map(S3Object::key);

        } catch (S3Exception e) {
            System.err.println("Erro ao listar arquivos no S3: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Falha ao listar arquivos no S3.", e);
        }
    }
}