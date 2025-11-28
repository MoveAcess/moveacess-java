package school.sptech;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoComBanco {

    private final String url;
    private final String user;
    private final String password;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            SlackNotifier.enviarMensagem("Driver MySQL não encontrado");
            throw new RuntimeException("Driver MySQL não encontrado", e);
        }
    }

    public ConexaoComBanco() {
        // 1. Pega as variáveis definidas no docker-compose.yml
        String dbHost = System.getenv("DB_HOST");       // Deve ser "mysql" no Docker
        String dbName = System.getenv("MYSQL_DATABASE");
        this.user = System.getenv("MYSQL_USER");
        this.password = System.getenv("MYSQL_PASSWORD");

        // 2. Validação simples para evitar erro de conexão nulo
        if (this.user == null || this.password == null) {
            SlackNotifier.enviarMensagem("Variáveis de ambiente de banco (MYSQL_USER, MYSQL_PASSWORD) não definidas!");
            throw new RuntimeException("Variáveis de ambiente de banco (MYSQL_USER, MYSQL_PASSWORD) não definidas!");
        }

        // 3. Define valores padrão caso não esteja rodando no Docker (ex: rodando local na IDE)
        // Isso permite que você teste na sua máquina sem configurar variáveis de ambiente
        if (dbHost == null) dbHost = "localhost";
        if (dbName == null) dbName = "moveacess";

        // 4. Monta a URL de conexão dinâmica
        // Exemplo: jdbc:mysql://mysql:3306/moveacess
        this.url = String.format("jdbc:mysql://%s:3306/%s?useSSL=false&allowPublicKeyRetrieval=true", dbHost, dbName);

        System.out.println("Configuração de Banco: Host=" + dbHost + ", Database=" + dbName + ", User=" + user);
        SlackNotifier.enviarMensagem("Configuração de Banco: Host=" + dbHost + ", Database=" + dbName + ", User=" + user);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}