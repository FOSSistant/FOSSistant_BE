package Capstone.FOSSistant.global.web.dto.util.custom;

import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodeExamples {
    ErrorStatus[] value();
}