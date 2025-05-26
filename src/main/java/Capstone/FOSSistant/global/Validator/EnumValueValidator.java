package Capstone.FOSSistant.global.Validator;

import Capstone.FOSSistant.global.web.dto.util.custom.EnumValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValueValidator
        implements ConstraintValidator<EnumValue, Object> {
    private Set<String> accepted;

    @Override
    public void initialize(EnumValue ann) {
        this.accepted = Arrays.stream(ann.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext ctx) {
        if (value == null) return true;          // @NotNull 과 조합
        return accepted.contains(value.toString());
    }
}