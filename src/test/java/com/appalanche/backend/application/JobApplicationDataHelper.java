package com.appalanche.backend.application;

import com.appalanche.backend.applications.persistence.dao.JobApplication;
import com.appalanche.backend.applications.persistence.dao.JobApplicationExperience;
import com.appalanche.backend.applications.persistence.dao.JobApplicationStatus;
import com.appalanche.backend.authentication.persistence.Account;

import java.time.Instant;
import java.util.UUID;

public class JobApplicationDataHelper {
    final static UUID USER_ACCOUNT_ID_1 = UUID.randomUUID();
    final static String USER_EMAIL_1 = "test.user@gmail.com";
    final static String USER_PASSWORD_1 = "1definitely2Secure";

    final static UUID USER_ACCOUNT_ID_2 = UUID.randomUUID();
    final static String USER_EMAIL_2 = "not.main@gmail.com";
    final static String USER_PASSWORD_2 = "blarrrgh@123";

    static Account firstAccount() {
        return new Account(USER_ACCOUNT_ID_1, USER_EMAIL_1, USER_PASSWORD_1);
    }

    static Account secondAccount() {
        return new Account(USER_ACCOUNT_ID_2, USER_EMAIL_2, USER_PASSWORD_2);
    }

    static JobApplicationStatus oldStatus() {
        return new JobApplicationStatus("old_status_1", "old status", 0, "000000", "000000");
    }

    static JobApplicationStatus status1() {
        return new JobApplicationStatus("STATUS_1_1", "Code One", 1, "aaaaaa", "aaaaaa");
    }

    static JobApplicationStatus status2() {
        return new JobApplicationStatus("STATUS_2_1", "Code Two", 1, "bbbbbb", "bbbbbb");
    }

    static JobApplicationStatus status3() {
        return new JobApplicationStatus("STATUS_2_2", "Code Two", 2, "cccccc", "cccccc");
    }

    static JobApplicationStatus status4() {
        return new JobApplicationStatus("STATUS_3_1", "Code Three", 1, "dddddd", "dddddd");
    }


    static JobApplicationExperience oldExperience() {
        return new JobApplicationExperience("old_exp_1", "old experience", "old description");
    }

    static JobApplicationExperience experience1() {
        return new JobApplicationExperience("EXP_1", "Experience One", "Description one");
    }

    static JobApplicationExperience experience2() {
        return new JobApplicationExperience("EXP_2", "Experience Two", "Description two");
    }

    static JobApplicationExperience experience3() {
        return new JobApplicationExperience("EXP_3", "Experience Three", "Description three");
    }

    static JobApplication firstUserApplication1(JobApplicationStatus status, JobApplicationExperience experience) {
        return new JobApplication(
                UUID.randomUUID(),
                "R-001",
                USER_ACCOUNT_ID_1,
                "Job 1",
                "Company 1",
                8,
                status,
                experience,
                Instant.now(), Instant.now());
    }

    static JobApplication firstUserApplication2(JobApplicationStatus status, JobApplicationExperience experience) {
        return new JobApplication(
                UUID.randomUUID(),
                "R-002",
                USER_ACCOUNT_ID_1,
                "Job 2",
                "Company 2",
                6,
                status,
                experience,
                Instant.now(), Instant.now());
    }

    static JobApplication firstUserApplication3(JobApplicationStatus status, JobApplicationExperience experience) {
        return new JobApplication(
                UUID.randomUUID(),
                "R-003",
                USER_ACCOUNT_ID_1,
                "Job 3",
                "Company 1",
                5,
                status,
                experience,
                Instant.now(), Instant.now());
    }

    static JobApplication secondUserApplication1(JobApplicationStatus status, JobApplicationExperience experience) {
        return new JobApplication(
                UUID.randomUUID(),
                "R-004",
                USER_ACCOUNT_ID_2,
                "Job 4",
                "Company 3",
                9,
                status,
                experience,
                Instant.now(), Instant.now());
    }

    static JobApplication secondUserApplication2(JobApplicationStatus status, JobApplicationExperience experience) {
        return new JobApplication(
                UUID.randomUUID(),
                "R-005",
                USER_ACCOUNT_ID_2,
                "Job 5",
                "Company 3",
                4,
                status,
                experience,
                Instant.now(), Instant.now());
    }

    static JobApplicationBuilder validUserOneOwnedUniqueApplication(JobApplicationStatus status, JobApplicationExperience experience) {
        return new JobApplicationBuilder().withApplicationId(UUID.randomUUID())
                                          .withRequisitionId("R-500")
                                          .withOwnerId(USER_ACCOUNT_ID_1)
                                          .withTitle("Unique Job Title")
                                          .withCompany("Unique Company")
                                          .withInterest(10)
                                          .withStatus(status)
                                          .withExperience(experience);
    }

    static JobApplicationBuilder userOneOwnedHelper(JobApplicationStatus status, JobApplicationExperience experience,
                                                    String title, Integer interest, Instant responseDate) {
        return validUserOneOwnedUniqueApplication(status, experience).withTitle(title)
                                                                     .withInterest(interest)
                                                                     .withResponseDate(responseDate);
    }

    static class JobApplicationBuilder {
        private UUID applicationId;
        private String requisitionId;
        private UUID ownerId;
        private String title;
        private String company;
        private Integer interest;
        private JobApplicationStatus status;
        private JobApplicationExperience experience;
        private Instant appliedDate;
        private Instant responseDate;

        JobApplicationBuilder() {
        }

        JobApplicationBuilder(JobApplication application) {
            this.applicationId = application.getApplicationId();
            this.requisitionId = application.getRequisitionId();
            this.ownerId = application.getOwnerAccountId();
            this.title = application.getTitle();
            this.company = application.getCompany();
            this.interest = application.getInterest();
            this.status = application.getStatus();
            this.experience = application.getExperience();
            this.appliedDate = application.getAppliedDate();
            this.responseDate = application.getResponseDate();
        }

        JobApplicationBuilder withApplicationId(UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        JobApplicationBuilder withRequisitionId(String requisitionId) {
            this.requisitionId = requisitionId;
            return this;
        }

        JobApplicationBuilder withOwnerId(UUID id) {
            this.ownerId = id;
            return this;
        }

        JobApplicationBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        JobApplicationBuilder withCompany(String company) {
            this.company = company;
            return this;
        }

        JobApplicationBuilder withInterest(Integer interest) {
            this.interest = interest;
            return this;
        }

        JobApplicationBuilder withStatus(JobApplicationStatus status) {
            this.status = status;
            return this;
        }

        JobApplicationBuilder withExperience(JobApplicationExperience experience) {
            this.experience = experience;
            return this;
        }

        JobApplicationBuilder withApplicationDate(Instant applicationDate) {
            this.appliedDate = applicationDate;
            return this;
        }

        JobApplicationBuilder withResponseDate(Instant responseDate) {
            this.responseDate = responseDate;
            return this;
        }

        JobApplication build() {
            return new JobApplication(applicationId, requisitionId, ownerId, title, company, interest, status,
                    experience, appliedDate, responseDate);
        }
    }
}
