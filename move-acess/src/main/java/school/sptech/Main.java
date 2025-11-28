package school.sptech;

import java.io.InputStream;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- INICIANDO SISTEMA DE IMPORTAÇÃO S3 -> MYSQL ---");
        SlackNotifier.enviarMensagem("INICIANDO SISTEMA DE IMPORTAÇÃO S3 -> MYSQL");

        try {
            String bucketName = System.getenv("S3_BUCKET_NAME");
            if (bucketName == null || bucketName.isBlank()) {
                throw new RuntimeException("ERRO: Variável 'S3_BUCKET_NAME' não definida.");
            }

            ConexaoComBanco conexao = new ConexaoComBanco();
            Log logger = new Log(conexao);
            S3Service s3Service = new S3Service();
            ExcelImporter importer = new ExcelImporter(conexao, logger);

            System.out.println("Buscando arquivo .xlsx no bucket: " + bucketName);
            String fileKey = s3Service.getLatestFileKey(bucketName, ".xlsx")
                    .orElseThrow(() -> new RuntimeException("Nenhum arquivo .xlsx encontrado no bucket."));

            System.out.println("Arquivo encontrado: " + fileKey);

            try (InputStream s3Stream = s3Service.getFileAsInputStream(bucketName, fileKey)) {
                System.out.println("Iniciando leitura e gravação no banco...");
                SlackNotifier.enviarMensagem("[PROCESSANDO] Iniciando leitura e gravação no banco");

                importer.processarStream(s3Stream);
            }

            System.out.println("--- PROCESSO FINALIZADO COM SUCESSO ---");
            SlackNotifier.enviarMensagem("[SUCESSO] PROCESSO FINALIZADO COM SUCESSO");

        } catch (Exception e) {
            System.err.println("ERRO FATAL NA MAIN:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}