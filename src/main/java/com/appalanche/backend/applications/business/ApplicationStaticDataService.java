package com.appalanche.backend.applications.business;

import com.appalanche.backend.applications.business.request_response.StatusMetadata;
import com.appalanche.backend.applications.config.JobApplicationStatusProperties;
import com.appalanche.backend.applications.persistence.JobApplicationExperienceRepository;
import com.appalanche.backend.applications.persistence.JobApplicationStatusRepository;
import com.appalanche.backend.applications.persistence.dao.JobApplicationExperience;
import com.appalanche.backend.applications.persistence.dao.JobApplicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ApplicationStaticDataService {
    private final JobApplicationStatusRepository statusRepository;
    private final JobApplicationExperienceRepository experienceRepository;
    private final JobApplicationStatusProperties statusProperties;

    public ApplicationStaticDataService(JobApplicationStatusRepository statusRepository,
                                        JobApplicationExperienceRepository experienceRepository,
                                        JobApplicationStatusProperties statusProperties) {
        this.statusRepository = statusRepository;
        this.experienceRepository = experienceRepository;
        this.statusProperties = statusProperties;
    }

    public List<JobApplicationStatus> getAllStatuses() {
        List<JobApplicationStatus> list = new ArrayList<>();
        statusRepository.findAll().forEach(list::add);
        return list;
    }

    public List<JobApplicationExperience> getAllExperiences() {
        List<JobApplicationExperience> list = new ArrayList<>();
        experienceRepository.findAll().forEach(list::add);
        return list;
    }

    public List<StatusMetadata> getStatusMetadata() {
        List<StatusMetadata> metadata = new ArrayList<>();
        for (JobApplicationStatusProperties.StatusConfig config : statusProperties.getStatuses()) {
            metadata.add(new StatusMetadata(config.getCode().replace("_{0}", ""), config.getMaxRounds()));
        }

        return metadata;
    }
}
