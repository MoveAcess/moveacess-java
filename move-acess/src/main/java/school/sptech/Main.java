package school.sptech;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {
    public static void main(String[] args) {
        String path;

        if (args != null && args.length > 0) {
            path = args[0];
            System.out.println("📁 Usando arquivo fornecido: " + path);
        } else {
            try {
                // Para arquivos dentro do JAR, precisamos copiar para um arquivo temporário
                URL resource = Main.class.getClassLoader().getResource("base.xlsx");
                if (resource == null) {
                    System.err.println("❌ Arquivo base.xlsx não encontrado em resources/");
                    return;
                }

                System.out.println("🔍 Arquivo encontrado no classpath: " + resource);

                // Se for um arquivo dentro do JAR (começa com jar:), copia para temp
                if (resource.toString().startsWith("jar:")) {
                    System.out.println("📦 Arquivo está dentro do JAR, criando cópia temporária...");
                    InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("base.xlsx");
                    if (inputStream == null) {
                        System.err.println("❌ Não foi possível carregar base.xlsx do classpath");
                        return;
                    }

                    File tempFile = File.createTempFile("base", ".xlsx");
                    tempFile.deleteOnExit(); // Apagar quando o programa terminar

                    Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    path = tempFile.getAbsolutePath();
                    System.out.println("✅ Arquivo temporário criado: " + path);
                } else {
                    // Se for um arquivo normal no filesystem
                    path = new File(resource.toURI()).getAbsolutePath();
                    System.out.println("📁 Arquivo local: " + path);
                }

            } catch (Exception e) {
                System.err.println("❌ Erro ao localizar arquivo: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        try {
            System.out.println("🔌 Iniciando conexão com banco...");
            ConexaoComBanco cfg = new ConexaoComBanco();
            Log logger = new Log(cfg);
            ExcelImporter importer = new ExcelImporter(cfg, logger);

            System.out.println("🚀 Iniciando importação do arquivo: " + path);
            importer.importFile(path);
            System.out.println("🎉 Importação concluída com sucesso!");

        } catch (Exception e) {
            System.err.println("💥 Erro na execução do programa: " + e.getMessage());
            e.printStackTrace();
        }
    }
}