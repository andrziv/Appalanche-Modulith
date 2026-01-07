package com.appalanche.backend.applications.endpoint;

import com.appalanche.backend.applications.business.JobApplicationService;
import com.appalanche.backend.applications.business.hateoas.JobApplicationModel;
import com.appalanche.backend.applications.business.hateoas.JobApplicationModelAssembler;
import com.appalanche.backend.applications.business.request_response.AddApplicationRequest;
import com.appalanche.backend.applications.business.request_response.AddApplicationResponse;
import com.appalanche.backend.applications.business.request_response.ModifyApplicationRequest;
import com.appalanche.backend.applications.business.request_response.SearchApplicationRequest;
import com.appalanche.backend.applications.persistence.dao.JobApplication;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RequestMapping("/application")
@RestController
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;
    private final PagedResourcesAssembler<JobApplication> pagedResourcesAssembler;
    private final JobApplicationModelAssembler jobApplicationModelAssembler;

    public JobApplicationController(JobApplicationService jobApplicationService,
                                    PagedResourcesAssembler<JobApplication> pagedResourcesAssembler,
                                    JobApplicationModelAssembler jobApplicationModelAssembler) {
        this.jobApplicationService = jobApplicationService;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.jobApplicationModelAssembler = jobApplicationModelAssembler;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<JobApplicationModel>> searchApplications(@Valid @ModelAttribute SearchApplicationRequest request,
                                                                              @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable) {
        var pagedModelReturn = pagedResourcesAssembler.toModel(
                jobApplicationService.searchApplications(request, pageable),
                jobApplicationModelAssembler);
        return ResponseEntity.ok(pagedModelReturn);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobApplicationModel> getJobApplication(@PathVariable Long id) {
        JobApplication application = jobApplicationService.getApplication(id);
        return ResponseEntity.ok(jobApplicationModelAssembler.toModelWithDescription(application));
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
    public ResponseEntity<Void> modifyApplication(@PathVariable Long id, @Valid @RequestBody ModifyApplicationRequest request) {
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
