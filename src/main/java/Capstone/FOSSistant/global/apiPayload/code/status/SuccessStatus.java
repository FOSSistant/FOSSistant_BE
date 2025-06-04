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
    MEMBER_CREATED(HttpStatus.CREATED, "MEMBER_100_001", "회원가입이 완료되었습니다."),
    MEMBER_LOGIN_OK(HttpStatus.OK, "MEMBER_100_002", "로그인이 완료되었습니다."),
    ACCESSTOKEN_OK(HttpStatus.OK, "MEMBER_100_003", "액세스 토큰이 재발급 되었습니다."),
    MEMBER_LOGOUT_OK(HttpStatus.OK, "MEMBER_100_004", "로그아웃이 완료되었습니다."),
    MEMBER_WITHDRAW_OK(HttpStatus.OK, "MEMBER_100_005", "회원 탈퇴가 완료되었습니다."),
    MEMBER_LEVEL_OK(HttpStatus.OK, "MEMBER_100_006", "회원의 레벨 설정이 완료되었습니다."),
    MEMBER_PROFILE_OK(HttpStatus.OK,  "MEMBER_100_007", "회원 프로필 조회 성공"),

    //레포 관련
    REPO_CRAWLING_OK(HttpStatus.OK, "REPO_300_001", "트렌딩 레포지터리 크롤링이 완료되었습니다."),



    // 이슈 관련
    ISSUE_TAGGING_OK(HttpStatus.OK, "ISSUE_200_001", "이슈 난이도 태깅 성공"),
    ISSUE_GUIDE_OK(HttpStatus.OK, "ISSUE_200_002", "이슈 해결 가이드 제공 성공"),
    ISSUE_FEEDBACK_OK(HttpStatus.OK, "ISSUE_200_003", "이슈 피드백 저장/수정 성공"),
    ISSUE_FEEDBACK_FOUND(HttpStatus.OK, "ISSUE_200_004", "작성한/작성하지 않은 피드백 조회 성공 ");

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

