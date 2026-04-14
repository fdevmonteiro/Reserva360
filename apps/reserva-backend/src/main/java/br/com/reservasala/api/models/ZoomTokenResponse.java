package br.com.reservasala.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZoomTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;


  @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private long expiresIn; // em segundos

    @JsonProperty("scope")
    private String scope;

    
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public String getScope() { return scope; }

  
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public void setScope(String scope) { this.scope = scope; }

    
    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}