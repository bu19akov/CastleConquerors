package main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EndpointsServiceTest {
    EndpointsService endpointsService = new EndpointsService();


    @Test
    void givenFaultyCredentials_whenTryingToLogIn_thenFalseReturned() {
        boolean isLoginSuccessful = endpointsService.verifyLogin(
                "non-existing-username",
                "password"
        );
        Assertions.assertFalse(isLoginSuccessful);
    }

    @Test
    void givenWorkingCredentials_whenTryingToLogIn_thenFalseReturned() {
        boolean isLoginSuccessful = endpointsService.verifyLogin(
                "test",
                "test"
        );
        Assertions.assertTrue(isLoginSuccessful);
    }
}