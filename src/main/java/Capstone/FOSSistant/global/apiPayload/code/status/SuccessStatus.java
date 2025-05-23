package Capstone.FOSSistant.global.apiPayload.code.status;


import Capstone.FOSSistant.global.apiPayload.code.BaseCode;
import Capstone.FOSSistant.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

    //멤버 관련
    MEMBER_CREATED(HttpStatus.CREATED, "MEMBER_1000", "회원가입이 완료되었습니다."),
    MEMBER_LOGIN_OK(HttpStatus.OK, "MEMBER_1001", "로그인이 완료되었습니다."),

    // 이슈 관련
    ISSUE_TAGGING_OK(HttpStatus.OK, "ISSUE_200_001", "이슈 난이도 태깅 성공"),
    ISSUE_GUIDE_OK(HttpStatus.OK, "ISSUE_200_002", "이슈 해결 가이드 제공 성공");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}

