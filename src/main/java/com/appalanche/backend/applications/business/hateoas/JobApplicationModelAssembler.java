package com.appalanche.backend.applications.business.hateoas;

import com.appalanche.backend.applications.endpoint.JobApplicationController;
import com.appalanche.backend.applications.persistence.dao.JobApplication;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class JobApplicationModelAssembler extends RepresentationModelAssemblerSupport<JobApplication, JobApplicationModel> {
    public JobApplicationModelAssembler() {
        super(JobApplicationController.class, JobApplicationModel.class);
    }

    @NonNull
    @Override
    public JobApplicationModel toModel(JobApplication entity) {
        return new JobApplicationModel().withId(entity.getId())
                                        .withApplicationId(entity.getApplicationId())
                                        .withRequisitionId(entity.getRequisitionId())
                                        .withTitle(entity.getTitle())
                                        .withCompany(entity.getCompany())
                                        .withInterest(entity.getInterest())
                                        .withStatus(entity.getStatus())
                                        .withExperience(entity.getExperience())
                                        .withJobPostingLink(entity.getJobPostingLink())
                                        .withAppliedDate(entity.getAppliedDate())
                                        .withResponseDate(entity.getResponseDate());
    }

    @NonNull
    public JobApplicationModel toModelWithDescription(JobApplication entity) {
        return toModel(entity).withDescription(entity.getDescription());
    }
}
