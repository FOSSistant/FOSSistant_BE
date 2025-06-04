package Capstone.FOSSistant.global.apiPayload.code.status;


import Capstone.FOSSistant.global.apiPayload.code.BaseErrorCode;
import Capstone.FOSSistant.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;



@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // Auth 관련
    // 100: Auth
    AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "100_001", "토큰이 만료되었습니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "100_002", "토큰이 유효하지 않습니다."),
    AUTH_GITHUB_FAIL(HttpStatus.UNAUTHORIZED, "100_003", "깃헙 로그인 실패."),

    // 200: Member
    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST,    "200_001", "사용자가 없습니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT,  "200_002", "이미 존재하는 유저입니다."),
    MEMBER_EMAIL_INVALID(HttpStatus.BAD_REQUEST,"200_003", "이메일 형식이 올바르지 않습니다."),
    MEMBER_LEVEL_BLANK(HttpStatus.BAD_REQUEST,  "200_004", "레벨은 필수입니다."),


    //서버 에러
    ISSUE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_500_001", "이슈를 찾지 못함"),
    REDIS_CONNECTION_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "INFRA_500_001", "Redis 연결 실패"),
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INFRA_500_002", "DB 처리 중 에러 발생"),
    NOT_CONTAIN_TOKEN(HttpStatus.INTERNAL_SERVER_ERROR, "INFRA_500_003", "Redis에 토큰이 없음."),
    AI_API_FAIL(HttpStatus.BAD_GATEWAY, "EXT_502_001", "AI API 호출 실패"),
    GITHUB_API_FAIL(HttpStatus.BAD_GATEWAY, "EXT_502_002", "Github API 호출 실패"),
    GITHUB_NO_REPOSITORY_FOUND(HttpStatus.BAD_GATEWAY, "EXT_502_002", "DB에 저장된 레포지터리 없음"),

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    ;



    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}


