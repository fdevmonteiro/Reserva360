package br.com.reservasala.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

// Esta classe representa a resposta que a Microsoft nos dá ao pedir um token de acesso
public class GraphTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    // Getters e Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}