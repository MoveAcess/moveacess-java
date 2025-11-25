package school.sptech;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class Log {
    private final ConexaoComBanco dbConfig;
    private int quantidadeErros = 0;

    public Log(ConexaoComBanco dbConfig) {
        this.dbConfig = dbConfig;
    }

    public void generateLog(String[] processes) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime horaInicio = LocalDateTime.now();

        System.out.printf("[%s] Iniciando processo de importação...%n", horaInicio.format(dateFormat));

        boolean sucessoGeral = true;

        try {
            for (String process : processes) {
                try {
                    int delay = ThreadLocalRandom.current().nextInt(500, 2000);
                    Thread.sleep(delay);

                    LocalDateTime fimProcesso = LocalDateTime.now();
                    System.out.printf("[%s] Processo '%s' concluído.%n", fimProcesso.format(dateFormat), process);

                } catch (InterruptedException e) {
                    System.err.printf("[%s] Interrupção no processo '%s'%n",
                            LocalDateTime.now().format(dateFormat), process);
                    quantidadeErros++;
                    sucessoGeral = false;
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.printf("[%s] Erro no processo '%s': %s%n",
                            LocalDateTime.now().format(dateFormat), process, e.getMessage());
                    quantidadeErros++;
                    sucessoGeral = false;
                }
            }
        } finally {
            LocalDateTime horaFim = LocalDateTime.now();

            try {
                registrarLog(horaInicio, horaFim, quantidadeErros);
            } catch (Exception e) {
                System.err.println("Erro ao registrar log final: " + e.getMessage());
            }

            String mensagemFinal = sucessoGeral ?
                    "Importação concluída com sucesso!" :
                    String.format("Importação concluída com %d erro(s)", quantidadeErros);

            System.out.printf("[%s] %s%n", horaFim.format(dateFormat), mensagemFinal);
        }
    }

    private void registrarLog(LocalDateTime horaInicio, LocalDateTime horaFim, int qtdErros) {
        String sql = "INSERT INTO registro_logs (horaInicioProcesso, horaEncerramentoProcesso, qtdErros) VALUES (?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, java.sql.Timestamp.valueOf(horaInicio));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(horaFim));
            ps.setInt(3, qtdErros);

            ps.executeUpdate();
            System.out.println("📝 Log registrado na tabela registro_logs");

        } catch (Exception e) {
            System.err.println("❌ Erro ao registrar log: " + e.getMessage());
            throw new RuntimeException("Falha ao registrar log", e);
        }
    }

    public int getQuantidadeErros() {
        return quantidadeErros;
    }
}