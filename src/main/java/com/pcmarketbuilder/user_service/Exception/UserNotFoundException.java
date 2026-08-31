package com.pcmarketbuilder.user_service.Exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("No se encontró el usuario con username: " + username);
    }

    public static UserNotFoundException byAzureOid(String azureOid) {
        return new UserNotFoundException("No se encontró el usuario con azure_oid: " + azureOid);
    }
}
