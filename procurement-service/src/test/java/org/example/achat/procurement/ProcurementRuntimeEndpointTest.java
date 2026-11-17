package org.example.achat.procurement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.example.achat.procurement.api.dto.OperatorReferenceResponse;
import org.example.achat.procurement.api.dto.ProductResponse;
import org.example.achat.procurement.api.dto.SupplierReferenceResponse;
import org.example.achat.procurement.infrastructure.client.OperatorClient;
import org.example.achat.procurement.infrastructure.client.ProductClient;
import org.example.achat.procurement.infrastructure.client.SupplierClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "CONFIG_SERVER_IMPORT=optional:configserver:http://localhost:8888",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:procurement_runtime;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.cloud.openfeign.client.config.default.connectTimeout=1000",
        "spring.cloud.openfeign.client.config.default.readTimeout=1000"
})
class ProcurementRuntimeEndpointTest {

    @MockitoBean
    private SupplierClient supplierClient;

    @MockitoBean
    private OperatorClient operatorClient;

    @MockitoBean
    private ProductClient productClient;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void invoiceEndpointsRespondThroughEmbeddedServer() throws Exception {
        when(supplierClient.findSupplier(7L))
                .thenReturn(new SupplierReferenceResponse(7L, "S-7", "Supplier"));
        when(operatorClient.findOperator(3L))
                .thenReturn(new OperatorReferenceResponse(3L, "Doe", "Jane"));
        when(productClient.findProduct(42L))
                .thenReturn(new ProductResponse(42L, "P-42", "Product", 5.0f, null, 2L));

        HttpResponse<String> created = request("POST", "/facture/add-facture",
                "{\"montantRemise\":99,\"montantFacture\":999,\"archivee\":false,"
                        + "\"dateCreationFacture\":\"2026-04-15\",\"fournisseurId\":7,"
                        + "\"detailsFacture\":[{\"qteCommandee\":3,\"pourcentageRemise\":10,"
                        + "\"produit\":{\"idProduit\":42}}]}");
        assertThat(created.statusCode()).isEqualTo(200);
        JsonNode invoice = objectMapper.readTree(created.body());
        long invoiceId = invoice.get("idFacture").asLong();
        assertThat(invoice.get("montantFacture").floatValue()).isEqualTo(13.5f);
        assertThat(invoice.get("montantRemise").floatValue()).isEqualTo(1.5f);
        assertThat(invoice.get("detailsFacture").get(0).get("produit").get("idProduit").asLong())
                .isEqualTo(42L);
        assertThat(invoice.get("detailsFacture").get(0).get("prixTotalDetail").floatValue())
                .isEqualTo(13.5f);

        HttpResponse<String> allInvoices = request("GET", "/facture/retrieve-all-factures", null);
        assertThat(allInvoices.statusCode()).isEqualTo(200);
        assertThat(allInvoices.body()).contains("\"libelleProduit\":\"Product\"");
        assertThat(request("GET", "/facture/retrieve-facture/" + invoiceId, null).statusCode())
                .isEqualTo(200);
        assertThat(request("GET", "/facture/getFactureByFournisseur/7", null).statusCode()).isEqualTo(200);
        assertThat(request("PUT", "/facture/assignOperateurToFacture/3/" + invoiceId, null)
                .statusCode()).isEqualTo(200);
        HttpResponse<String> eligibility = request(
                "GET", "/internal/v1/invoices/" + invoiceId + "/payment-eligibility", null);
        assertThat(eligibility.statusCode()).isEqualTo(200);
        assertThat(eligibility.body()).contains("\"eligible\":true");
        HttpResponse<String> total = request("GET",
                "/internal/v1/invoices/total?startDate=2026-04-01&endDate=2026-04-30", null);
        assertThat(total.statusCode()).isEqualTo(200);
        assertThat(total.body()).contains("\"total\":13.5");
        assertThat(request("PUT", "/facture/cancel-facture/" + invoiceId, null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/facture/retrieve-facture/" + invoiceId, null)
                .body()).contains("\"archivee\":true");
        assertThat(request("GET", "/internal/v1/invoices/" + invoiceId + "/payment-eligibility", null)
                .body()).contains("\"eligible\":false", "\"reasonCode\":\"INVOICE_ARCHIVED\"");
    }

    @Test
    void missingProductPreventsInvoiceCreation() throws Exception {
        when(productClient.findProduct(404L)).thenThrow(feignNotFound());

        HttpResponse<String> response = request("POST", "/facture/add-facture",
                "{\"montantRemise\":0,\"montantFacture\":0,\"archivee\":false,"
                        + "\"detailsFacture\":[{\"qteCommandee\":1,\"pourcentageRemise\":0,"
                        + "\"produit\":{\"idProduit\":404}}]}");

        assertThat(response.statusCode()).isEqualTo(404);
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

    private feign.FeignException feignNotFound() {
        feign.Request request = feign.Request.create(
                feign.Request.HttpMethod.GET,
                "/produits/404",
                java.util.Map.of(),
                null,
                java.nio.charset.StandardCharsets.UTF_8,
                null);
        feign.Response response = feign.Response.builder()
                .status(404)
                .request(request)
                .headers(java.util.Map.of())
                .build();
        return feign.FeignException.errorStatus("ProductClient#findProduct", response);
    }
}
