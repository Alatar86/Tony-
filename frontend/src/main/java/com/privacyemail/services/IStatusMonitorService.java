package com.privacyemail.services;

import com.privacyemail.models.ApiResult;
import com.privacyemail.models.StatusResponse;
import javafx.concurrent.Task;

public interface IStatusMonitorService {
    Task<ApiResult<StatusResponse>> checkBackendStatus();
    Task<ApiResult<StatusResponse>> getBackendStatus();
}
