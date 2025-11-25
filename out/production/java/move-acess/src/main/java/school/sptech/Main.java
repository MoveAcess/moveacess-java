package school.sptech;

import java.net.URL;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String path;

        if (args != null && args.length > 0) {
            path = args[0];
        } else {
            URL resource = Main.class.getClassLoader().getResource("base.xlsx");
            if (resource == null) {
                System.err.println("Arquivo base.xlsx não encontrado em resources/");
                return;
            }
            path = Paths.get(resource.getPath()).toString();
        }

        try {
            ConexaoComBanco cfg = new ConexaoComBanco();
            Log logger = new Log(cfg);
            ExcelImporter importer = new ExcelImporter(cfg, logger);

            System.out.println("🚀 Iniciando importação do arquivo: " + path);
            importer.importFile(path);

        } catch (Exception e) {
            System.err.println("💥 Erro na execução do programa: " + e.getMessage());
            e.printStackTrace();
        }
    }
}