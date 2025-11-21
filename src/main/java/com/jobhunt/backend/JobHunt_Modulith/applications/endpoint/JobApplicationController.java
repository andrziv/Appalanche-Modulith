package com.jobhunt.backend.JobHunt_Modulith.applications.endpoint;

import com.jobhunt.backend.JobHunt_Modulith.applications.business.JobApplicationService;
import com.jobhunt.backend.JobHunt_Modulith.applications.business.request_response.AddApplicationRequest;
import com.jobhunt.backend.JobHunt_Modulith.applications.business.request_response.ModifyApplicationRequest;
import com.jobhunt.backend.JobHunt_Modulith.applications.persistence.JobApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/application")
@RestController
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobApplication>> getJobApplications() {
        List<JobApplication> applications = jobApplicationService.getApplications();
        return ResponseEntity.ok(applications);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> addApplication(@RequestBody AddApplicationRequest request) {
        jobApplicationService.addApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> modifyApplication(@PathVariable Long id, @RequestBody ModifyApplicationRequest request) {
        jobApplicationService.modifyApplication(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeApplication(@PathVariable Long id) {
        jobApplicationService.removeApplication(id);
        return ResponseEntity.noContent().build();
    }
}
