package com.jobhunt.backend.JobHunt_Modulith.application;

import com.jobhunt.backend.JobHunt_Modulith.applications.persistence.JobApplication;
import com.jobhunt.backend.JobHunt_Modulith.applications.persistence.JobApplicationStatus;
import com.jobhunt.backend.JobHunt_Modulith.authentication.persistence.Account;

import java.util.Date;

public class JobApplicationDataHelper {
    final static String USER_FIRST_NAME_1 = "Test";
    final static String USER_LAST_NAME_1 = "User";
    final static String USER_EMAIL_1 = "test.user@gmail.com";
    final static String USER_PASSWORD_1 = "1definitely2Secure";

    final static String USER_FIRST_NAME_2 = "Other";
    final static String USER_LAST_NAME_2 = "User";
    final static String USER_EMAIL_2 = "not.main@gmail.com";
    final static String USER_PASSWORD_2 = "blarrrgh@123";

    static Account firstAccount() {
        return new Account(USER_FIRST_NAME_1, USER_LAST_NAME_1, USER_EMAIL_1, USER_PASSWORD_1);
    }

    static Account secondAccount() {
        return new Account(USER_FIRST_NAME_2, USER_LAST_NAME_2, USER_EMAIL_2, USER_PASSWORD_2);
    }

    static JobApplicationStatus status1() {
        return new JobApplicationStatus("CODE_1_1", "Code One", 1, "aaaaaa", "aaaaaa");
    }

    static JobApplicationStatus status2() {
        return new JobApplicationStatus("CODE_2_1", "Code Two", 1, "bbbbbb", "bbbbbb");
    }

    static JobApplicationStatus status3() {
        return new JobApplicationStatus("CODE_2_2", "Code Two", 2, "cccccc", "cccccc");
    }

    static JobApplicationStatus status4() {
        return new JobApplicationStatus("CODE_3_1", "Code Three", 1, "dddddd", "dddddd");
    }

    static JobApplication firstUserApplication1(JobApplicationStatus status) {
        return new JobApplication(
                "R-001",
                USER_EMAIL_1,
                "Job 1",
                "Company 1",
                8,
                status,
                new Date(), new Date());
    }

    static JobApplication firstUserApplication2(JobApplicationStatus status) {
        return new JobApplication(
                "R-002",
                USER_EMAIL_1,
                "Job 2",
                "Company 2",
                6,
                status,
                new Date(), new Date());
    }

    static JobApplication firstUserApplication3(JobApplicationStatus status) {
        return new JobApplication(
                "R-003",
                USER_EMAIL_1,
                "Job 3",
                "Company 1",
                5,
                status,
                new Date(), new Date());
    }

    static JobApplication secondUserApplication1(JobApplicationStatus status) {
        return new JobApplication(
                "R-004",
                USER_EMAIL_2,
                "Job 4",
                "Company 3",
                9,
                status,
                new Date(), new Date());
    }

    static JobApplication secondUserApplication2(JobApplicationStatus status) {
        return new JobApplication(
                "R-005",
                USER_EMAIL_2,
                "Job 5",
                "Company 3",
                4,
                status,
                new Date(), new Date());
    }
}
