package Capstone.FOSSistant.global.Validator;

import Capstone.FOSSistant.global.web.dto.util.custom.ValidGitHubIssueUrl;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GitHubIssueUrlValidator implements ConstraintValidator<ValidGitHubIssueUrl, String> {

    private static final String REGEX = "^https://github\\.com/[^/]+/[^/]+/issues/\\d+$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false; // NotBlank 조건
        }
        return value.matches(REGEX);
    }
}