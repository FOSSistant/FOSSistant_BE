package Capstone.FOSSistant.global.security.oauth;

public interface GitHubOAuthClient {
    String getAccessTokenFromCode(String code);
    GitHubUserInfo getUserInfo(String accessToken);

    record GitHubUserInfo(Long githubId, String email, String nickname, String profileImage) {}
}
