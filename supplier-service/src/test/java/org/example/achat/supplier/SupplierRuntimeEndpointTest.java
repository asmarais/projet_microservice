package org.example.achat.supplier;

import org.example.achat.supplier.api.dto.ActivitySectorRequest;
import org.example.achat.supplier.api.dto.ActivitySectorResponse;
import org.example.achat.supplier.api.dto.SupplierRequest;
import org.example.achat.supplier.api.dto.SupplierResponse;
import org.example.achat.supplier.domain.SupplierCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
        "spring.datasource.url=jdbc:h2:mem:supplier_runtime;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class SupplierRuntimeEndpointTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @org.springframework.beans.factory.annotation.Autowired
    private Environment environment;

    @org.springframework.beans.factory.annotation.Autowired
    private ObjectMapper objectMapper;

    @Test
    void supplierAndSectorEndpointsRespondThroughEmbeddedServer() throws Exception {
        SupplierRequest supplierRequest = new SupplierRequest(
                "S-RUNTIME", "Runtime supplier", SupplierCategory.ORDINAIRE, null);
        HttpResponse<String> createdSupplier = request("POST", "/fournisseurs",
                objectMapper.writeValueAsString(supplierRequest));
        assertThat(createdSupplier.statusCode()).isEqualTo(201);
        Long supplierId = objectMapper.readValue(createdSupplier.body(), SupplierResponse.class).idFournisseur();

        ActivitySectorRequest sectorRequest = new ActivitySectorRequest("A-RUNTIME", "Runtime sector");
        HttpResponse<String> createdSector = request("POST", "/secteurs-activite",
                objectMapper.writeValueAsString(sectorRequest));
        assertThat(createdSector.statusCode()).isEqualTo(201);
        Long sectorId = objectMapper.readValue(createdSector.body(), ActivitySectorResponse.class)
                .idSecteurActivite();

        assertThat(request("GET", "/fournisseurs", null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/secteurs-activite/" + sectorId, null).statusCode()).isEqualTo(200);

        HttpResponse<String> assigned = request("PUT",
                "/fournisseurs/" + supplierId + "/secteurs-activite/" + sectorId, null);
        assertThat(assigned.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readValue(assigned.body(), SupplierResponse.class)
                .secteurActiviteIds()).contains(sectorId);

        assertThat(request("GET", "/fournisseurs/" + supplierId, null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/secteurs-activite", null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/internal/v1/suppliers/" + supplierId, null).statusCode()).isEqualTo(200);

        assertThat(request("PUT", "/fournisseurs/" + supplierId,
                "{\"code\":\"S-UPDATED\",\"libelle\":\"Updated supplier\","
                        + "\"categorieFournisseur\":\"CONVENTIONNE\"}").statusCode()).isEqualTo(200);
        assertThat(request("PUT", "/secteurs-activite/" + sectorId,
                "{\"codeSecteurActivite\":\"A-UPDATED\",\"libelleSecteurActivite\":\"Updated sector\"}")
                .statusCode()).isEqualTo(200);
        assertThat(request("DELETE", "/fournisseurs/" + supplierId, null).statusCode()).isEqualTo(204);
        assertThat(request("DELETE", "/secteurs-activite/" + sectorId, null).statusCode()).isEqualTo(204);
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
