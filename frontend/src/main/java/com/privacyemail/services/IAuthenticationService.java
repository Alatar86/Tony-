package com.privacyemail.services;

import com.privacyemail.models.ApiResult;
import com.privacyemail.models.StatusResponse;
import javafx.concurrent.Task;

/**
 * Interface abstraction for AuthenticationService to aid testing and dependency inversion.
 */
public interface IAuthenticationService {
    Task<ApiResult<Boolean>> initiateLogin();
    Task<ApiResult<StatusResponse>> verifyAuthenticationStatus();
    Task<ApiResult<Boolean>> checkAuthStatus();
}
