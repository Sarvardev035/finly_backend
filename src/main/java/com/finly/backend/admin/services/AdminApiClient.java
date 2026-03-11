package com.finly.backend.admin.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finly.backend.admin.security.VaadinSecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminApiClient {

    private final WebClient.Builder webClientBuilder;
    private final VaadinSecurityContext securityContext;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "http://localhost:8080";

    private WebClient getClient() {
        String token = securityContext.getOrCreateToken();
        String actAsUserId = securityContext.getActAsUserId();
        WebClient.Builder builder = webClientBuilder.baseUrl(BASE_URL);
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (actAsUserId != null && !actAsUserId.isBlank()) {
            builder.defaultHeader("X-User-Id", actAsUserId);
        }
        return builder.build();
    }

    public <T> Mono<T> get(String path, Class<T> responseType) {
        return getClient().get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .bodyToMono(JsonNode.class)
                .map(node -> convert(node.path("data"), responseType));
    }

    public <T> Mono<T[]> getArray(String path, Class<T[]> arrayType) {
        return getClient().get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .bodyToMono(JsonNode.class)
                .map(node -> convert(node.path("data"), arrayType));
    }

    public <T> Mono<List<T>> getList(String path, ParameterizedTypeReference<List<T>> typeReference) {
        return getClient().get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .bodyToMono(JsonNode.class)
                .map(node -> convertList(node.path("data"), typeReference));
    }

    public <T, R> Mono<R> post(String path, T body, Class<R> responseType) {
        return getClient().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .bodyToMono(JsonNode.class)
                .map(node -> {
                    if (node.has("data")) {
                        return convert(node.path("data"), responseType);
                    }
                    return convert(node, responseType);
                });
    }

    public <T, R> Mono<R> put(String path, T body, Class<R> responseType) {
        return getClient().put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .bodyToMono(JsonNode.class)
                .map(node -> {
                    if (node.has("data")) {
                        return convert(node.path("data"), responseType);
                    }
                    return convert(node, responseType);
                });
    }

    public Mono<Void> delete(String path) {
        return getClient().delete()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .toBodilessEntity()
                .then();
    }

    private <T> T convert(JsonNode node, Class<T> responseType) {
        return objectMapper.convertValue(node, responseType);
    }

    private <T> List<T> convertList(JsonNode node, ParameterizedTypeReference<List<T>> typeReference) {
        return objectMapper.convertValue(node,
                objectMapper.getTypeFactory().constructType(typeReference.getType()));
    }

    private Mono<? extends Throwable> toApiException(ClientResponse response) {
        return response.bodyToMono(JsonNode.class)
                .defaultIfEmpty(objectMapper.createObjectNode())
                .map(node -> {
                    String message = node.path("message").asText();
                    if (message == null || message.isBlank()) {
                        message = "Request failed with status " + response.statusCode().value();
                    }
                    return new RuntimeException(message);
                });
    }
}
