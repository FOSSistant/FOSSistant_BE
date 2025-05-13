package Capstone.FOSSistant.global.web.dto.util.custom;

import Capstone.FOSSistant.global.Validator.GitHubIssueUrlValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GitHubIssueUrlValidator.class)
@Documented
public @interface ValidGitHubIssueUrl {
    String message() default "유효하지 않은 Github 이슈 URL입니다.;";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
