package com.sda.mapper.api;

import com.sda.mapper.api.dto.SystemStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes a deterministic smoke endpoint for application reachability checks.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    /**
     * Returns the stable application status contract.
     *
     * @return current application status
     */
    @GetMapping("/status")
    public SystemStatusResponse status() {
        return new SystemStatusResponse("UP", "semantic-sda-mapper");
    }
}
