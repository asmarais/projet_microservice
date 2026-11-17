package org.example.achat.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "CONFIG_SERVER_IMPORT=optional:configserver:http://localhost:8888",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:user_runtime;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class UserRuntimeEndpointTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void operatorProfileCrudRunsOverEmbeddedServerAndDoesNotReturnCredential() throws Exception {
        HttpResponse<String> created = request("POST", "/operateurs",
                "{\"nom\":\"Doe\",\"prenom\":\"Jane\",\"password\":\"secret-password\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).doesNotContain("secret-password", "passwordHash");
        JsonNode response = objectMapper.readTree(created.body());
        long operatorId = response.get("idOperateur").asLong();

        assertThat(request("GET", "/operateurs", null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/operateurs/" + operatorId, null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/internal/v1/operators/" + operatorId, null).statusCode()).isEqualTo(200);
        assertThat(request("PUT", "/operateurs/" + operatorId,
                "{\"nom\":\"Doe\",\"prenom\":\"Janet\",\"password\":\"replacement\"}")
                .statusCode()).isEqualTo(200);
        assertThat(request("DELETE", "/operateurs/" + operatorId, null).statusCode()).isEqualTo(204);
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        int port = Integer.parseInt(environment.getProperty("local.server.port"));
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
