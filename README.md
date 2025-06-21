# FOSSistant 🚀

FOSSistant는 **오픈소스 프로젝트 기여를 돕는 AI 기반 어시스턴트**입니다.  
GitHub 이슈 분석, 해결 방안 제시, 기여 가이드 제공을 통해 오픈소스 생태계 참여를 지원합니다.

## 📋 주요 기능

### 🔍 이슈 분석 & 요약
- GitHub 이슈의 자동 분석 및 요약
- LLM 기반 이슈 내용 이해 및 중요 포인트 하이라이팅
- 프로젝트 컨텍스트를 고려한 맞춤형 해결 방안 제시

### 📚 기여 가이드
- 프로젝트별 Contributing 가이드 연결
- README.md, 이슈 템플릿 등 관련 문서 링크 제공
- 디렉토리 구조 분석을 통한 코드 기여 가이드

### 🔐 사용자 인증
- GitHub OAuth 2.0 연동
- JWT 기반 토큰 인증
- 사용자별 맞춤 서비스 제공

### 📊 모니터링
- Spring Boot Actuator 기반 헬스체크
- Redis 캐싱을 통한 성능 최적화

## 🛠 기술 스택

### Backend
- **Java 17** - 언어
- **Spring Boot 3.4.5** - 프레임워크
- **Spring Security** - 보안
- **Spring Data JPA** - 데이터 접근

### Database & Cache
- **MySQL** - 주 데이터베이스 (Azure MySQL)
- **Redis** - 캐싱 & 세션 관리

### AI & External APIs
- **Google Gemini API** - LLM 기반 이슈 분석
- **GitHub GrpahQLAPI** - 저장소 및 이슈 데이터 연동

### Infrastructure
- **Docker** - 컨테이너화
- **Docker Compose** - 로컬 개발 환경

### Documentation
- **SpringDoc OpenAPI** - API 문서 자동 생성


## 📁 프로젝트 구조

```
src/main/java/Capstone/FOSSistant/
├── FosSistantApplication.java          # 메인 애플리케이션
└── global/                             # 전역 설정 및 공통 기능
    ├── config/                         # 스프링 설정
    ├── security/                       # 보안 설정
    ├── domain/                         # 엔티티 모델
    ├── repository/                     # 데이터 접근 계층
    ├── service/                        # 비즈니스 로직
    ├── web/                           # 웹 계층 (컨트롤러)
    ├── apiPayload/                    # API 요청/응답 DTO
    ├── converter/                     # 데이터 변환
    ├── Validator/                     # 입력값 검증
    └── aop/                          # 관점 지향 프로그래밍
```

## 🔧 주요 설정

### JVM 옵션
```bash
-Xmx512m -Xms256m  # 메모리 제한 (Docker 환경)
```

### Redis 설정
```yaml
# 최대 메모리: 256MB
# 정책: allkeys-lru (가장 오래된 키 삭제)
```

### 헬스체크
- **엔드포인트**: `/actuator/health`
- **간격**: 30초
- **타임아웃**: 10초
- **재시도**: 3회

## 📄 라이선스

이 프로젝트는 [라이선스 파일](LICENSE)에 명시된 조건에 따라 배포됩니다.

**FOSSistant**로 오픈소스 기여를 더 쉽고 즐겁게 만들어보세요! 🎉 
