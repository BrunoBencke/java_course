package cerne.externallookup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;

import cerne.members.Cpf;

/** Client for the external CPF service with Java 11's HTTP Client. It is never called in the tests. */
@ApplicationScoped
public class CpfLookupClient implements CpfLookup {

    private static final URI BASE_URL = URI.create("https://cpf-lookup.example.coop/v1/cpf/");

    // one client per application: it keeps the connection pool
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private final String authenticationHeader;

    public CpfLookupClient() {
        this("cerne", "change-in-configuration");
    }

    public CpfLookupClient(String username, String password) {
        this.authenticationHeader = authenticationHeader(username, password);
    }

    @Override
    public CpfStatus lookUp(Cpf cpf) {
        var request = HttpRequest.newBuilder(BASE_URL.resolve(cpf.number()))
                .header("Accept", "application/json")
                .header("Authorization", authenticationHeader)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("CPF lookup failed with status " + response.statusCode());
            }
            return CpfStatus.fromJson(response.body());
        } catch (IOException e) {
            throw new UncheckedIOException("CPF lookup failed for " + cpf.formatted(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // never swallow the interruption
            throw new IllegalStateException("CPF lookup interrupted", e);
        }
    }

    static String authenticationHeader(String username, String password) {
        var credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    static String usernameFromHeader(String header) {
        var credentials = new String(Base64.getDecoder().decode(header.substring("Basic ".length())),
                StandardCharsets.UTF_8);
        return credentials.substring(0, credentials.indexOf(':'));
    }
}
