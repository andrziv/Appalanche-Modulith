package com.appalanche.backend.applications.business;

import com.appalanche.backend.applications.business.request_response.AddApplicationRequest;
import com.appalanche.backend.applications.business.request_response.ModifyApplicationRequest;
import com.appalanche.backend.applications.business.request_response.SearchApplicationRequest;
import com.appalanche.backend.applications.persistence.ApplicationRepository;
import com.appalanche.backend.applications.persistence.ApplicationSpecificationFactory;
import com.appalanche.backend.applications.persistence.JobApplicationExperienceRepository;
import com.appalanche.backend.applications.persistence.JobApplicationStatusRepository;
import com.appalanche.backend.applications.persistence.dao.JobApplication;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final JobApplicationStatusRepository statusRepository;
    private final JobApplicationExperienceRepository experienceRepository;

    public JobApplicationService(ApplicationRepository applicationRepository, JobApplicationStatusRepository statusRepository,
                                 JobApplicationExperienceRepository experienceRepository) {
        this.applicationRepository = applicationRepository;
        this.statusRepository = statusRepository;
        this.experienceRepository = experienceRepository;
    }

    public Optional<JobApplication> getApplication(long id) {
        return applicationRepository.findByIdAndOwnerEmail(id, getCurrentUserEmail());
    }

    public Page<JobApplication> searchApplications(SearchApplicationRequest filter, Pageable pageable) {
        Specification<JobApplication> spec = ApplicationSpecificationFactory.generateSpecificationList(filter, getCurrentUserEmail());
        return applicationRepository.findAll(spec, pageable);
    }

    @Transactional
    public JobApplication addApplication(AddApplicationRequest request) {
        logger.info("Received {} to AddApplication service method.", request);

        var status = statusRepository.findByCode(request.statusCode())
                                     .orElseThrow(() -> {
                                         var errorString = String.format("Either '%s' is an improper status code, " +
                                                 "or the code was not found in the database.", request.statusCode());
                                         return new EntityNotFoundException(errorString);
                                     });

        var experience = experienceRepository.findByCode(request.experienceLevelCode())
                                             .orElseThrow(() -> {
                                                 var errorString = String.format("Either '%s' is an improper experience level code, " +
                                                         "or the code was not found in the database.", request.experienceLevelCode());
                                                 return new EntityNotFoundException(errorString);
                                             });

        var appliedDate = request.appliedDate();
        if (appliedDate == null) {
            appliedDate = new Date(System.currentTimeMillis());
        }

        var responseDate = request.responseDate();
        if (responseDate == null) {
            responseDate = appliedDate;
        } else if (responseDate.before(appliedDate)) {
            appliedDate = responseDate;
            logger.warn("Response date detected to be before the Applied date on Application creation. Setting Applied date to Response date.");
        }

        JobApplication application =
                new JobApplication(request.requisitionId(),
                        getCurrentUserEmail(),
                        request.title(),
                        request.company(),
                        request.interest(),
                        status,
                        experience,
                        appliedDate,
                        responseDate);

        return applicationRepository.save(application);
    }

    @Transactional
    public void modifyApplication(Long applicationId, ModifyApplicationRequest request) {
        logger.info("Received {} to ModifyApplication service method.", request);

        String currentUser = getCurrentUserEmail();

        var application = applicationRepository.findByIdAndOwnerEmail(applicationId, currentUser)
                                               .orElseThrow(() -> new EntityNotFoundException("Job application not found."));

        if (request.statusCode() != null) {
            var newStatus = statusRepository.findByCode(request.statusCode())
                                            .orElseThrow(() -> {
                                                var errorString = String.format("Either '%s' is an improper status code, or the code was not found in the database.", request.statusCode());
                                                return new EntityNotFoundException(errorString);
                                            });

            application.setStatus(newStatus);
        }

        if (request.experienceLevelCode() != null) {
            var newExperienceLevel = experienceRepository.findByCode(request.experienceLevelCode())
                                                         .orElseThrow(() -> {
                                                             var errorString = String.format("Either '%s' is an improper experience level code, " +
                                                                     "or the code was not found in the database.", request.experienceLevelCode());
                                                             return new EntityNotFoundException(errorString);
                                                         });

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

        if (request.appliedDate() != null) {
            application.setAppliedDate(request.appliedDate());
        }

        if (request.responseDate() != null) {
            application.setResponseDate(request.responseDate());
        }

        // TODO: owner email changing logic perhaps?

        applicationRepository.save(application);
    }

    @Transactional
    public void removeApplication(Long applicationId) {
        logger.info("Received DeletionRequest[id='{}'] at RemoveApplication service method.", applicationId);

        String currentUser = getCurrentUserEmail();

        JobApplication application = applicationRepository.findByIdAndOwnerEmail(applicationId, currentUser)
                                                          .orElseThrow(() -> new EntityNotFoundException("Job application not found."));

        applicationRepository.delete(application);
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
