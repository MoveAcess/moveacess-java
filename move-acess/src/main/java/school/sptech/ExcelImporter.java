package school.sptech;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ExcelImporter {
    private final ConexaoComBanco dbConfig;
    private final Log logger;

    public ExcelImporter(ConexaoComBanco dbConfig, Log logger) {
        this.dbConfig = dbConfig;
        this.logger = logger;
    }

    // Método principal alterado para receber InputStream
    public void processarStream(InputStream inputStream) throws Exception {

        // Logs de monitoramento
        String[] processos = {
                "CONEXAO_BANCO",
                "STREAM_S3_LEITURA",
                "PROCESSAMENTO_LINHAS",
                "INSERT_BATCH"
        };
        logger.generateLog(processos);
        SlackNotifier.enviarMensagem("CONEXAO_BANCO\",\n" +
                "                \"STREAM_S3_LEITURA\",\n" +
                "                \"PROCESSAMENTO_LINHAS\",\n" +
                "                \"INSERT_BATCH");

        // Abre o Excel direto da memória (Stream)
        try (Workbook workbook = new XSSFWorkbook(inputStream);
             Connection conn = dbConfig.getConnection()) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Pular cabeçalho
            if (rows.hasNext()) rows.next();

            // Os SQLs permanecem os mesmos, mas as colunas 'ano' serão preenchidas com INT
            String sqlLocal = "INSERT INTO localEmbarque (nome, municipio, linha_frota, tipo, ano) VALUES (?, ?, ?, ?, ?)";
            String sqlVeiculo = "INSERT INTO veiculo (tipoTransporte, tipoVeiculo, statusAcessibilidade, ano) VALUES (?, ?, ?, ?)";

            try (PreparedStatement psLocal = conn.prepareStatement(sqlLocal);
                 PreparedStatement psVeiculo = conn.prepareStatement(sqlVeiculo)) {

                int countLocal = 0;
                int countVeiculo = 0;
                Set<String> linhasProcessadas = new HashSet<>();

                while (rows.hasNext()) {
                    Row row = rows.next();

                    try {
                        String estacao = getCellString(row.getCell(0));
                        String linha = getCellString(row.getCell(1));
                        String equipamentos = getCellString(row.getCell(2));
                        String nivelAcessibilidade = getCellString(row.getCell(3));
                        String anoStr = getCellString(row.getCell(4));

                        // Chamamos o método parseAno que agora retorna um Integer
                        Integer ano = parseAno(anoStr);

                        if (estacao == null || estacao.isBlank() || ano == null) continue;

                        // 1. Inserir Local Embarque
                        psLocal.setString(1, estacao);
                        psLocal.setString(2, "São Paulo");
                        psLocal.setString(3, linha);
                        psLocal.setString(4, equipamentos);
                        psLocal.setInt(5, ano); // USANDO setInt() para coluna YEAR
                        psLocal.addBatch();
                        countLocal++;

                        // 2. Inserir Veículo (evitando duplicados na mesma execução)
                        if (linha != null && !linha.isBlank() && !linhasProcessadas.contains(linha)) {
                            // Se houver várias linhas separadas por pipe "|"
                            String[] linhasArray = linha.split("\\|");
                            for (String l : linhasArray) {
                                String lClean = l.trim();
                                if (!lClean.isEmpty() && !linhasProcessadas.contains(lClean)) {
                                    psVeiculo.setString(1, "Trem/Metrô");
                                    psVeiculo.setString(2, "Linha " + lClean);
                                    psVeiculo.setString(3, nivelAcessibilidade);
                                    psVeiculo.setInt(4, ano); // USANDO setInt() para coluna YEAR
                                    psVeiculo.addBatch();
                                    countVeiculo++;
                                    linhasProcessadas.add(lClean);
                                }
                            }
                        }

                        // Executa o lote a cada 50 registros para não sobrecarregar a memória
                        if (countLocal % 50 == 0) {
                            psLocal.executeBatch();
                            psVeiculo.executeBatch();
                            System.out.printf("...processados %d registros.%n", countLocal);
                            SlackNotifier.enviarMensagem("...processados %d registros.%n");
                        }

                    } catch (Exception e) {
                        System.err.println("Erro linha " + (row.getRowNum() + 1) + ": " + e.getMessage());
                        // CONTINUA O LOOP para tentar a próxima linha
                    }
                }

                // Executa o restante
                psLocal.executeBatch();
                psVeiculo.executeBatch();

                System.out.println("✅ IMPORTAÇÃO CONCLUÍDA!");
                System.out.println("Locais inseridos: " + countLocal);
                System.out.println("Veículos inseridos: " + countVeiculo);
                SlackNotifier.enviarMensagem("✅ IMPORTAÇÃO CONCLUÍDA!");
                SlackNotifier.enviarMensagem("Locais inseridos: " + countLocal);
                SlackNotifier.enviarMensagem("Veículos inseridos: " + countVeiculo);
            }
        }
    }

    // --- Métodos Auxiliares ---

    private Integer parseAno(String anoStr) {
        try {
            if (anoStr == null || anoStr.isBlank()) {
                // Se for nulo/vazio, retorna o ano atual
                return LocalDate.now().getYear();
            }

            // Tenta obter o ano como inteiro a partir da string
            return (int) Double.parseDouble(anoStr.trim());
        } catch (NumberFormatException e) {
            // Se o parsing falhar (ex: a célula contém texto), retorna o ano atual como fallback
            // Para ser ainda mais rigoroso, poderia retornar 'null' e ignorar a linha.
            System.err.println("WARN: Valor de 'ano' não é um número válido: " + anoStr + ". Usando ano atual como fallback.");
            SlackNotifier.enviarMensagem("WARN: Valor de 'ano' não é um número válido: " + anoStr + ". Usando ano atual como fallback.");
            return LocalDate.now().getYear();
        } catch (Exception e) {
            System.err.println("ERRO INESPERADO no parseAno: " + e.getMessage());
            SlackNotifier.enviarMensagem("ERRO INESPERADO no parseAno: " + e.getMessage());
            return LocalDate.now().getYear();
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ?
                    // Se for data formatada, retorna apenas o ano da data (que ainda será uma string, mas passível de parse)
                    String.valueOf(LocalDate.ofInstant(cell.getDateCellValue().toInstant(), java.time.ZoneId.systemDefault()).getYear()) :
                    String.valueOf((int) cell.getNumericCellValue()); // Força inteiro se for número
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> null;
        };
    }
}