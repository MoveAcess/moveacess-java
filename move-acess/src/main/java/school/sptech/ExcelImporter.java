package school.sptech;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
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

    public void importFile(String filePath) throws Exception {
        String[] processos = {
                "CONEXAO_BANCO_DADOS",
                "LEITURA_ARQUIVO_EXCEL",
                "PROCESSAMENTO_DADOS_LOCAL_EMBARQUE",
                "INSERCAO_DADOS_BANCO"
        };

        logger.generateLog(processos);

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis);
             Connection conn = dbConfig.getConnection()) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Pular cabeçalho
            if (rows.hasNext()) rows.next();

            // SQL para inserir nas tabelas
            String sqlLocalEmbarque = "INSERT INTO localEmbarque (nome, municipio, linha_frota, tipo, ano) VALUES (?, ?, ?, ?, ?)";
            String sqlVeiculo = "INSERT INTO veiculo (tipoTransporte, tipoVeiculo, statusAcessibilidade, ano) VALUES (?, ?, ?, ?)";

            try (PreparedStatement psLocalEmbarque = conn.prepareStatement(sqlLocalEmbarque);
                 PreparedStatement psVeiculo = conn.prepareStatement(sqlVeiculo)) {

                int countLocalEmbarque = 0;
                int countVeiculo = 0;
                Set<String> linhasProcessadas = new HashSet<>();

                while (rows.hasNext()) {
                    Row row = rows.next();

                    try {
                        // Ler dados do Excel
                        String estacao = getCellString(row.getCell(0)); // Coluna A: Estação
                        String linha = getCellString(row.getCell(1));   // Coluna B: Linha
                        String equipamentos = getCellString(row.getCell(2)); // Coluna C: Equipamentos
                        String nivelAcessibilidade = getCellString(row.getCell(3)); // Coluna D: Nível de Acessibilidade
                        String anoStr = getCellString(row.getCell(4)); // Coluna E: Ano

                        // Pular linhas vazias
                        if (estacao == null || estacao.isBlank()) {
                            continue;
                        }

                        // Inserir na tabela localEmbarque (ESTAÇÕES)
                        psLocalEmbarque.setString(1, estacao); // nome
                        psLocalEmbarque.setString(2, "São Paulo"); // municipio (assumindo SP)
                        psLocalEmbarque.setString(3, linha); // linha_frota
                        psLocalEmbarque.setString(4, equipamentos); // endereco (equipamentos disponíveis)
                        psLocalEmbarque.setDate(5, parseAno(anoStr)); // ano
                        psLocalEmbarque.addBatch();
                        countLocalEmbarque++;

                        // Processar linhas únicas para a tabela veiculo
                        if (linha != null && !linha.isBlank() && !linhasProcessadas.contains(linha)) {
                            String[] linhasArray = linha.split("\\|");
                            for (String linhaIndividual : linhasArray) {
                                String linhaLimpa = linhaIndividual.trim();
                                if (!linhaLimpa.isEmpty() && !linhasProcessadas.contains(linhaLimpa)) {
                                    // Inserir na tabela veiculo (LINHAS DE TRANSPORTE)
                                    psVeiculo.setString(1, "Trem/Metrô"); // tipoTransporte
                                    psVeiculo.setString(2, "Linha " + linhaLimpa); // tipoVeiculo
                                    psVeiculo.setString(3, nivelAcessibilidade); // statusAcessibilidade
                                    psVeiculo.setDate(4, parseAno(anoStr)); // ano
                                    psVeiculo.addBatch();
                                    countVeiculo++;

                                    linhasProcessadas.add(linhaLimpa);
                                }
                            }
                        }

                        // Executar batch a cada 50 registros
                        if (countLocalEmbarque % 50 == 0) {
                            psLocalEmbarque.executeBatch();
                            psVeiculo.executeBatch();
                            System.out.printf("Processados %d registros...%n", countLocalEmbarque);
                        }

                    } catch (Exception e) {
                        System.err.println("Erro ao processar linha " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }

                // Executar batches restantes
                psLocalEmbarque.executeBatch();
                psVeiculo.executeBatch();

                System.out.println("✅ Importação finalizada com sucesso!");
                System.out.println("Locais de embarque inseridos: " + countLocalEmbarque);
                System.out.println("Veículos (linhas) inseridos: " + countVeiculo);

            }

        } catch (Exception e) {
            System.err.println("❌ Erro durante a importação: " + e.getMessage());
            throw e;
        }
    }

    private java.sql.Date parseAno(String anoStr) {
        try {
            if (anoStr == null || anoStr.isBlank()) {
                return new java.sql.Date(System.currentTimeMillis());
            }

            // Converter string do ano para Date (primeiro dia do ano)
            int ano = (int) Double.parseDouble(anoStr); // Para tratar números do Excel
            LocalDate data = LocalDate.of(ano, 1, 1);
            return java.sql.Date.valueOf(data);

        } catch (Exception e) {
            System.err.println("Erro ao converter ano: " + anoStr + ", usando ano atual");
            return new java.sql.Date(System.currentTimeMillis());
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    // Para números, converter para inteiro se for número redondo
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        yield String.valueOf((int) value);
                    } else {
                        yield String.valueOf(value);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        yield String.valueOf((int) cell.getNumericCellValue());
                    } catch (Exception e2) {
                        yield String.valueOf(cell.getNumericCellValue());
                    }
                }
            }
            default -> null;
        };
    }
}