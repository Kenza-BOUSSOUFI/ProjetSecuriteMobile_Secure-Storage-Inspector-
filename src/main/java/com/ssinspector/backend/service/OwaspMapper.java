package com.ssinspector.backend.service;

import com.ssinspector.backend.model.OwaspCategory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for mapping internal detection types to OWASP Mobile Top 10 categories.
 */
@Service
public class OwaspMapper {

    private static final Map<String, OwaspCategory> MAPPINGS = new HashMap<>();

    static {
        // M1: Improper Platform Usage
        MAPPINGS.put("EXPORTED_COMPONENT_EXPOSED", OwaspCategory.M1);
        MAPPINGS.put("DANGEROUS_PERMISSION_REQUESTED", OwaspCategory.M1);
        MAPPINGS.put("EXPORTED_COMPONENT", OwaspCategory.M1);
        MAPPINGS.put("DANGEROUS_PERMISSION", OwaspCategory.M1);

        // M2: Insecure Data Storage
        MAPPINGS.put("ALLOW_BACKUP_ENABLED", OwaspCategory.M2);
        MAPPINGS.put("API_KEY", OwaspCategory.M2);
        MAPPINGS.put("JWT_TOKEN", OwaspCategory.M2);
        MAPPINGS.put("HEURISTIC_SECRET", OwaspCategory.M2);
        MAPPINGS.put("INSECURE_LOGGING", OwaspCategory.M2);
        MAPPINGS.put("SHARED_PREFS_STORAGE", OwaspCategory.M2);
        MAPPINGS.put("HEURISTIC_KV", OwaspCategory.M2);
        MAPPINGS.put("SUSPICIOUS_STRING", OwaspCategory.M2);
        
        // M5: Insufficient Cryptography
        MAPPINGS.put("WEAK_CRYPTO", OwaspCategory.M5);
    }

    /**
     * Retrieves the OWASP category for a given internal issue type.
     * 
     * @param type Internal issue type (e.g., JWT_TOKEN)
     * @return OwaspCategory or null if no mapping exists
     */
    public OwaspCategory getCategory(String type) {
        return MAPPINGS.get(type);
    }
}
