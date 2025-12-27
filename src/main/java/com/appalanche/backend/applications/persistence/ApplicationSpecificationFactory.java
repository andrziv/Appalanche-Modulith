package com.appalanche.backend.applications.persistence;

import com.appalanche.backend.applications.business.request_response.SearchApplicationRequest;
import com.appalanche.backend.applications.persistence.dao.JobApplication;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.util.StringUtils.hasText;

public class ApplicationSpecificationFactory {
    public static Specification<JobApplication> generateSpecificationList(SearchApplicationRequest searchRequest,
                                                                          UUID ownerAccountId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("ownerAccountId"), ownerAccountId));

            if (hasText(searchRequest.search())) {
                String pattern = "%" + searchRequest.search().toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate companyMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("company")), pattern);
                Predicate requisitionIdMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("requisitionId")), pattern);

                predicates.add(criteriaBuilder.or(titleMatch, companyMatch, requisitionIdMatch));
            }

            if (searchRequest.statusCodes() != null && !searchRequest.statusCodes().isEmpty()) {
                predicates.add(root.get("status").get("code").in(searchRequest.statusCodes()));
            }

            if (searchRequest.experienceLevelCodes() != null && !searchRequest.experienceLevelCodes().isEmpty()) {
                predicates.add(root.get("experience").get("code").in(searchRequest.experienceLevelCodes()));
            }

            if (searchRequest.interestCriteria() != null && !searchRequest.interestCriteria().isEmpty()) {
                List<Predicate> interestPredicates = new ArrayList<>();
                for (String criteria : searchRequest.interestCriteria()) {
                    interestPredicates.add(parseInterest(criteria, root, criteriaBuilder));
                }

                predicates.add(criteriaBuilder.or(interestPredicates.toArray(new Predicate[0])));
            }

            if (searchRequest.appliedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("appliedDate"),
                        startOfDay(searchRequest.appliedAfter())));
            }
            if (searchRequest.appliedBefore() != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("appliedDate"),
                        startOfNextDay(searchRequest.appliedBefore())));
            }
            if (searchRequest.responseAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("responseDate"),
                        startOfDay(searchRequest.responseAfter())));
            }
            if (searchRequest.responseBefore() != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("responseDate"),
                        startOfNextDay(searchRequest.responseBefore())));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate parseInterest(String criteria, Root<JobApplication> root, CriteriaBuilder criteriaBuilder) {
        String[] chunks = criteria.split(":");
        String operator = chunks[0];
        int firstNum = Integer.parseInt(chunks[1]);

        return switch (operator) {
            case "gt" -> criteriaBuilder.greaterThan(root.get("interest"), firstNum);
            case "gte" -> criteriaBuilder.greaterThanOrEqualTo(root.get("interest"), firstNum);
            case "lt" -> criteriaBuilder.lessThan(root.get("interest"), firstNum);
            case "lte" -> criteriaBuilder.lessThanOrEqualTo(root.get("interest"), firstNum);
            case "eq" -> criteriaBuilder.equal(root.get("interest"), firstNum);
            case "between" -> {
                int secondNum = Integer.parseInt(chunks[2]);
                yield criteriaBuilder.between(root.get("interest"), firstNum, secondNum);
            }
            default -> throw new IllegalArgumentException("Unknown interest operator: " + operator);
        };
    }

    private static Instant startOfDay(LocalDate date) {
        var dayStart = date.atStartOfDay();
        return dayStart.toInstant(ZoneOffset.UTC);
    }

    private static Instant startOfNextDay(LocalDate date) {
        var nextDayStart = date.plusDays(1).atStartOfDay();
        return nextDayStart.toInstant(ZoneOffset.UTC);
    }
}
