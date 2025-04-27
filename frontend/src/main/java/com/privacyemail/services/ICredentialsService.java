package com.privacyemail.services;

public interface ICredentialsService {
    void saveApiKey(String serviceName, String apiKey);
    String loadApiKey(String serviceName);
    void removeApiKey(String serviceName);
}
