package school.sptech;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SlackNotifier {

    private static final String WEBHOOK_URL = System.getenv("SLACK_URL");

    public static void enviarMensagem(String mensagem) {
        try {

            String jsonPayload = "{\"text\": \"" + mensagem + "\"}";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEBHOOK_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Erro ao enviar para o Slack: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("Não foi possível conectar ao Slack: " + e.getMessage());
        }
    }
}