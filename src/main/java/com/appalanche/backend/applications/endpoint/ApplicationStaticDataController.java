package com.appalanche.backend.applications.endpoint;

import com.appalanche.backend.applications.business.ApplicationStaticDataService;
import com.appalanche.backend.applications.persistence.dao.JobApplicationExperience;
import com.appalanche.backend.applications.persistence.dao.JobApplicationStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.springframework.http.CacheControl.maxAge;

@RequestMapping("/application/static")
@RestController
public class ApplicationStaticDataController {
    private final ApplicationStaticDataService staticDataService;

    public ApplicationStaticDataController(ApplicationStaticDataService staticDataService) {
        this.staticDataService = staticDataService;
    }

    @GetMapping("/statuses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobApplicationStatus>> getStatuses() {
        return ResponseEntity.ok()
                             .cacheControl(maxAge(24, HOURS))
                             .body(staticDataService.getAllStatuses());
    }

    @GetMapping("/experiences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobApplicationExperience>> getExperiences() {
        return ResponseEntity.ok()
                             .cacheControl(maxAge(24, HOURS))
                             .body(staticDataService.getAllExperiences());
    }
}
