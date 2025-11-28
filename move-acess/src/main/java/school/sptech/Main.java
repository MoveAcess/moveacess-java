package school.sptech;

import java.io.InputStream;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- INICIANDO SISTEMA DE IMPORTAÇÃO S3 -> MYSQL ---");

        try {
            // 1. Validar Variáveis de Ambiente
            String bucketName = System.getenv("S3_BUCKET_NAME");
            if (bucketName == null || bucketName.isBlank()) {
                throw new RuntimeException("ERRO: Variável 'S3_BUCKET_NAME' não definida.");
            }

            // 2. Inicializar Dependências
            ConexaoComBanco conexao = new ConexaoComBanco();
            Log logger = new Log(conexao);
            S3Service s3Service = new S3Service();
            ExcelImporter importer = new ExcelImporter(conexao, logger);

            // 3. Buscar arquivo mais recente
            System.out.println("Buscando arquivo .xlsx no bucket: " + bucketName);
            String fileKey = s3Service.getLatestFileKey(bucketName, ".xlsx")
                    .orElseThrow(() -> new RuntimeException("Nenhum arquivo .xlsx encontrado no bucket."));

            System.out.println("Arquivo encontrado: " + fileKey);

            // 4. Baixar (Stream) e Processar
            // O try-with-resources fecha o download do S3 automaticamente ao terminar
            try (InputStream s3Stream = s3Service.getFileAsInputStream(bucketName, fileKey)) {
                System.out.println("Iniciando leitura e gravação no banco...");

                // AQUI ESTÁ A MÁGICA: Passamos o fluxo de dados, não um arquivo
                importer.processarStream(s3Stream);
            }

            System.out.println("--- PROCESSO FINALIZADO COM SUCESSO ---");

        } catch (Exception e) {
            System.err.println("ERRO FATAL NA MAIN:");
            e.printStackTrace();
            System.exit(1); // Sai com erro para o Docker reiniciar se necessário
        }
    }
}