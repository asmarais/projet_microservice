package org.example.achat.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.example.achat.payment.api.dto.InvoiceEligibilityResponse;
import org.example.achat.payment.api.dto.InvoiceTotalResponse;
import org.example.achat.payment.infrastructure.client.InvoiceClient;
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
        "spring.datasource.url=jdbc:h2:mem:payment_runtime;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class PaymentRuntimeEndpointTest {

    @MockitoBean
    private InvoiceClient invoiceClient;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void settlementEndpointsRespondOverEmbeddedServer() throws Exception {
        when(invoiceClient.getPaymentEligibility(9L))
                .thenReturn(new InvoiceEligibilityResponse(9L, true, null));
        when(invoiceClient.getInvoiceTotal(
                java.time.LocalDate.parse("2026-06-01"),
                java.time.LocalDate.parse("2026-06-30")))
                .thenReturn(new InvoiceTotalResponse(25.0f));

        HttpResponse<String> created = request("POST", "/reglements",
                "{\"montantPaye\":12.5,\"montantRestant\":2.5,\"payee\":false,"
                        + "\"dateReglement\":\"2026-06-05\",\"factureId\":9}");
        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode settlement = objectMapper.readTree(created.body());
        long settlementId = settlement.get("idReglement").asLong();

        assertThat(request("GET", "/reglements", null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/reglements/" + settlementId, null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/reglements/facture/9", null).statusCode()).isEqualTo(200);
        assertThat(request("PUT", "/reglements/" + settlementId,
                "{\"montantPaye\":13.5,\"montantRestant\":1.5,\"payee\":true,"
                        + "\"dateReglement\":\"2026-06-06\",\"factureId\":9}").statusCode()).isEqualTo(200);
        assertThat(request("GET", "/reglements/revenue?startDate=2026-06-01&endDate=2026-06-30", null)
                .statusCode()).isEqualTo(200);
        assertThat(request("GET",
                "/internal/v1/settlements/revenue?startDate=2026-06-01&endDate=2026-06-30", null)
                .statusCode()).isEqualTo(200);
        HttpResponse<String> recoveryRate = request(
                "GET", "/facture/pourcentageRecouvrement/2026-06-01/2026-06-30", null);
        assertThat(recoveryRate.statusCode()).isEqualTo(200);
        assertThat(Float.parseFloat(recoveryRate.body())).isBetween(53.9f, 54.1f);
        assertThat(request("DELETE", "/reglements/" + settlementId, null).statusCode()).isEqualTo(204);
    }

    @Test
    void rejectsArchivedInvoiceAndUnavailableProcurement() throws Exception {
        when(invoiceClient.getPaymentEligibility(10L))
                .thenReturn(new InvoiceEligibilityResponse(10L, false, "INVOICE_ARCHIVED"));
        HttpResponse<String> archived = request("POST", "/reglements",
                "{\"montantPaye\":1,\"montantRestant\":0,\"payee\":true,"
                        + "\"dateReglement\":\"2026-06-05\",\"factureId\":10}");
        assertThat(archived.statusCode()).isEqualTo(409);

        when(invoiceClient.getPaymentEligibility(12L)).thenThrow(feign.FeignException.errorStatus(
                "InvoiceClient#getPaymentEligibility",
                feign.Response.builder()
                        .status(404)
                        .request(feign.Request.create(
                                feign.Request.HttpMethod.GET,
                                "/internal/v1/invoices/12/payment-eligibility",
                                java.util.Map.of(),
                                null,
                                java.nio.charset.StandardCharsets.UTF_8,
                                null))
                        .headers(java.util.Map.of())
                        .build()));
        HttpResponse<String> missing = request("POST", "/reglements",
                "{\"montantPaye\":1,\"montantRestant\":0,\"payee\":true,"
                        + "\"dateReglement\":\"2026-06-05\",\"factureId\":12}");
        assertThat(missing.statusCode()).isEqualTo(404);

        when(invoiceClient.getPaymentEligibility(11L)).thenThrow(feign.FeignException.errorStatus(
                "InvoiceClient#getPaymentEligibility",
                feign.Response.builder()
                        .status(503)
                        .request(feign.Request.create(
                                feign.Request.HttpMethod.GET,
                                "/internal/v1/invoices/11/payment-eligibility",
                                java.util.Map.of(),
                                null,
                                java.nio.charset.StandardCharsets.UTF_8,
                                null))
                        .headers(java.util.Map.of())
                        .build()));
        HttpResponse<String> unavailable = request("POST", "/reglements",
                "{\"montantPaye\":1,\"montantRestant\":0,\"payee\":true,"
                        + "\"dateReglement\":\"2026-06-05\",\"factureId\":11}");
        assertThat(unavailable.statusCode()).isEqualTo(503);
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
