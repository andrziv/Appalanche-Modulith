package com.appalanche.backend.applications.business;

import com.appalanche.backend.applications.business.request_response.AddApplicationRequest;
import com.appalanche.backend.applications.business.request_response.ModifyApplicationRequest;
import com.appalanche.backend.applications.business.request_response.SearchApplicationRequest;
import com.appalanche.backend.applications.persistence.ApplicationRepository;
import com.appalanche.backend.applications.persistence.ApplicationSpecificationFactory;
import com.appalanche.backend.applications.persistence.dao.JobApplication;
import jakarta.persistence.EntityNotFoundException;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final ApplicationStaticDataService staticDataService;
    private final PolicyFactory sanitizer;

    public JobApplicationService(ApplicationRepository applicationRepository,
                                 ApplicationStaticDataService staticDataService, PolicyFactory sanitizer) {
        this.applicationRepository = applicationRepository;
        this.staticDataService = staticDataService;
        this.sanitizer = sanitizer;
    }

    public JobApplication getApplication(UUID id) {
        return applicationRepository.findByApplicationIdAndOwnerAccountId(id, getCurrentAccountId())
                                    .orElseThrow(() -> new EntityNotFoundException("Job application not found."));
    }

    public Page<JobApplication> searchApplications(SearchApplicationRequest filter, Pageable pageable) {
        var newFilter = processFilter(filter);
        Specification<JobApplication> spec = ApplicationSpecificationFactory.generateSpecificationList(newFilter, getCurrentAccountId());
        return applicationRepository.findAll(spec, pageable);
    }

    @Transactional
    public JobApplication addApplication(AddApplicationRequest request) {
        logger.info("Received {} to AddApplication service method.", request);

        var status = staticDataService.statusByCode(request.statusCode());
        var experience = staticDataService.experienceByCode(request.experienceLevelCode());

        var appliedDate = request.appliedDate();
        if (appliedDate == null) {
            appliedDate = Instant.now();
        }

        var responseDate = request.responseDate();
        if (responseDate == null) {
            responseDate = appliedDate;
        } else if (responseDate.isBefore(appliedDate)) {
            appliedDate = responseDate;
            logger.warn("Response date detected to be before the Applied date on Application creation. Setting Applied date to Response date.");
        }

        var description = request.description();
        if (description != null) {
            description = sanitizer.sanitize(description);
        }

        JobApplication application =
                new JobApplication(UUID.randomUUID(),
                        request.requisitionId(),
                        getCurrentAccountId(),
                        request.title(),
                        request.company(),
                        request.interest(),
                        status,
                        experience,
                        request.jobPostingLink(),
                        description,
                        appliedDate,
                        responseDate);

        return applicationRepository.save(application);
    }

    @Transactional
    public void modifyApplication(UUID applicationId, ModifyApplicationRequest request) {
        logger.info("Received {} to ModifyApplication service method.", request);

        UUID currentAccountId = getCurrentAccountId();

        var application = applicationRepository.findByApplicationIdAndOwnerAccountId(applicationId, currentAccountId)
                                               .orElseThrow(() -> new EntityNotFoundException("Job application not found."));

        if (request.statusCode() != null) {
            var newStatus = staticDataService.statusByCode(request.statusCode());
            application.setStatus(newStatus);
        }

        if (request.experienceLevelCode() != null) {
            var newExperienceLevel = staticDataService.experienceByCode(request.experienceLevelCode());
            application.setExperience(newExperienceLevel);
        }

        if (request.requisitionId() != null) {
            application.setRequisitionId(request.requisitionId());
        }

        if (request.title() != null) {
            application.setTitle(request.title());
        }

        if (request.company() != null) {
            application.setCompany(request.company());
        }

        if (request.interest() != null) {
            application.setInterest(request.interest());
        }

        if (request.jobPostingLink() != null) {
            application.setJobPostingLink(request.jobPostingLink());
        }

        if (request.description() != null) {
            String sanitizedDescription = null;
            if (!request.description().isBlank()) {
                sanitizedDescription = sanitizer.sanitize(request.description());
            }

            application.setDescription(sanitizedDescription);
        }

        if (request.appliedDate() != null) {
            application.setAppliedDate(request.appliedDate());
        }

        if (request.responseDate() != null) {
            application.setResponseDate(request.responseDate());
        }

        applicationRepository.save(application);
    }

    @Transactional
    public void removeApplication(UUID applicationId) {
        logger.info("Received DeletionRequest[id='{}'] at RemoveApplication service method.", applicationId);

        UUID currentAccountId = getCurrentAccountId();

        JobApplication application = applicationRepository.findByApplicationIdAndOwnerAccountId(applicationId, currentAccountId)
                                                          .orElseThrow(() -> new EntityNotFoundException("Job application not found."));

        applicationRepository.delete(application);
    }

    private UUID getCurrentAccountId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private SearchApplicationRequest processFilter(SearchApplicationRequest request) {
        var expandedStatuses = expandStatuses(request.statusCodes());

        return new SearchApplicationRequest(
                request.search(),
                expandedStatuses,
                request.experienceLevelCodes(),
                request.interestCriteria(),
                request.appliedAfter(),
                request.appliedBefore(),
                request.responseAfter(),
                request.responseBefore(),
                request.timezone());
    }

    private List<String> expandStatuses(List<String> status) {
        if (status == null) {
            return null;
        }

        var expandedStatuses = new ArrayList<String>();
        for (String statusFragment : status) {
            var expandedCodes = staticDataService.expandStatusCodeFragment(statusFragment);
            var searchCodes = expandedCodes.isEmpty() ? List.of(statusFragment) : expandedCodes;
            expandedStatuses.addAll(searchCodes);
        }

        return expandedStatuses;
    }
}
