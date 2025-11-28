package com.jobhunt.backend.JobHunt_Modulith.applications.endpoint;

import com.jobhunt.backend.JobHunt_Modulith.applications.business.JobApplicationService;
import com.jobhunt.backend.JobHunt_Modulith.applications.business.request_response.AddApplicationRequest;
import com.jobhunt.backend.JobHunt_Modulith.applications.business.request_response.AddApplicationResponse;
import com.jobhunt.backend.JobHunt_Modulith.applications.business.request_response.ModifyApplicationRequest;
import com.jobhunt.backend.JobHunt_Modulith.applications.persistence.JobApplication;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

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

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobApplication>> getJobApplication(@PathVariable Long id) {
        Optional<JobApplication> application = jobApplicationService.getApplication(id);
        return application.map(jobApplication -> ResponseEntity.ok(List.of(jobApplication)))
                          .orElseGet(() -> ResponseEntity.ok(List.of()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddApplicationResponse> addApplication(@Valid @RequestBody AddApplicationRequest request) {
        var result = jobApplicationService.addApplication(request);

        AddApplicationResponse response = new AddApplicationResponse(result.getId());
        URI location = fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(location).body(response);
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
