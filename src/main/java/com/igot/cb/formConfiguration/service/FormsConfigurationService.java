package com.igot.cb.formConfiguration.service;

import com.igot.cb.util.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface FormsConfigurationService {

    default ApiResponse readFormConfig(Map<String, Object> request, String token, String org, List<String> userRoles, boolean isAdmin) {
        return readFormConfig(request, token, org, userRoles, isAdmin, null);
    }

    /**
     * @param shaKey lms profileToken (x-authenticated-user-profileToken), or null for plain /formsConfig/read.
     *               When non-null it must verify and be issued for this caller: its rootorgid /
     *               ministryOrStateType / designation are then used and no org read / user read call is
     *               made. Expired: 422; otherwise invalid: 401.
     */
    ApiResponse readFormConfig(Map<String, Object> request, String token, String org, List<String> userRoles, boolean isAdmin, String shaKey);

    ApiResponse createFormConfigV2(Map<String, Object> request, String token);

    ApiResponse readFormConfigById(Long formId, String token, boolean isAdmin);

    ApiResponse updateFormConfigV2(Map<String, Object> request, String token);

    ApiResponse listFormConfigs(String token);
}
