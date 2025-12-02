package com.appalanche.backend.applications.endpoint;

import com.appalanche.backend.applications.business.JobApplicationService;
import com.appalanche.backend.applications.business.request_response.AddApplicationRequest;
import com.appalanche.backend.applications.business.request_response.AddApplicationResponse;
import com.appalanche.backend.applications.business.request_response.ModifyApplicationRequest;
import com.appalanche.backend.applications.business.request_response.SearchApplicationRequest;
import com.appalanche.backend.applications.persistence.dao.JobApplication;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.domain.Sort.Direction.DESC;
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
    public ResponseEntity<Page<JobApplication>> searchApplications(@ModelAttribute SearchApplicationRequest request,
                                                                   @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable) {
        return ResponseEntity.ok(jobApplicationService.searchApplications(request, pageable));
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
