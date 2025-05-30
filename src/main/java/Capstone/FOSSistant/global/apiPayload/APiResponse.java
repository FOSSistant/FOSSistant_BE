package Capstone.FOSSistant.global.apiPayload;

import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class APiResponse<T> {
    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    // 성공한 경우 응답 생성
    public static <T> APiResponse<T> onSuccess(SuccessStatus status, T result){
        return new APiResponse<>(true, status.getCode(), status.getMessage(), result);
    }

//    public static <T> ApiResponse<T> of(BaseCode code, T result){
//        return new ApiResponse<>(true, code.getReasonHttpStatus().getCode() , code.getReasonHttpStatus().getMessage(), result);
//    }

    // 실패한 경우 응답 생성
    public static <T> APiResponse<T> onFailure(String code, String message, T data){
        return new APiResponse<>(false, code, message, data);
    }
}

