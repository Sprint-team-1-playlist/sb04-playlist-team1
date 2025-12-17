# TEAM1 - Playlist

## 목차
1. [링크](#링크)
2. [프로젝트 소개](#프로젝트-소개)
3. [기술 스택](#기술-스택)
4. [프로젝트 실행 가이드](#프로젝트-실행-가이드)
   - [1. 필수 소프트웨어](#1-필수-소프트웨어)
   - [2. 환경 변수 설정](#2-환경-변수-설정)
   - [3. 인프라 서비스 실행 (Redis, Kafka)](#3-인프라-서비스-실행-redis-kafka)
   - [4. 데이터베이스 설정 (PostgreSQL)](#4-데이터베이스-설정-postgresql)
   - [5. 외부 API](#5-외부-api)
   - [6. AWS (운영 환경)](#6-aws-운영-환경)
   - [7. 프로젝트 실행](#7-프로젝트-실행)
5. [파일 구조](#파일-구조)
6. [팀원 구성](#팀원-구성)

---  

## 링크
<a href="https://www.notion.so/2a7a7ba35b118194a38ad50d87f51905">
  <img src="https://github.com/user-attachments/assets/b8d5ff15-4c53-49ea-83d4-97b08af86455" width="30" height="30" valign="middle" />
  모두의플리 팀 노션
</a><br><br>
<a href="https://playlist-team1.me/#/sign-in">
  <img src="https://github.com/user-attachments/assets/3700f539-d6fe-40b7-869b-e5a4c0a01463" width="30" height="30" valign="middle" />
  배포 링크 ( ~ 26.01.18  )
</a><br><br>
<a href="https://github.com/user-attachments/files/24206721/playlist.pdf">
  <img src="https://raw.githubusercontent.com/Sprint-team-1-playlist/sb04-playlist-team1/refs/heads/develop/src/main/resources/static/vite.svg" width="30" height="30" valign="middle" />
  포트폴리오(pdf)
</a><br>

---

## **프로젝트 소개**

- 프로젝트 기간: 2025.11.10 ~ 2025.12.18
- 대규모 트래픽이 예상되는 글로벌 컨텐츠 평점 및 큐레이션 플랫폼
- 영화, 드라마, 스포츠 등 다양한 콘텐츠를 큐레이팅하고 공유하며, 실시간 같이 보기 기능까지 제공하는 소셜 서비스로, 사용자들은 자신만의 플레이리스트를 만들고, 다른 사용자와 소통하며 콘텐츠 경험을 확장할 수  있는 서비스
---

## **기술 스택**

<!--
 - 기본 개발 환경: IntelliJ, Spring Boot(v3.5.5), Java(v17)
- Database: PostgreSQL(v17.5), MongoDB(Atlas), AWS-RDS
- Storage: AWS-S3
- 배포: Docker, GitHub Actions(CI/CD), AWS(AWS-ECR, AWS-ECS, AWS-EC2)
- 추가 스택: Spring Data JPA, Spring Actuator, Spring WebFlux(네이버 API), Jsoup(RSS), Spring Batch, Mockito, micrometer(커스텀 매트릭)  
- 협업 Tool: Git & Github, Discord, Notion
 -->

| Category | Stacks                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| :--- |:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Backend** | <img src="https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white"> <img src="https://img.shields.io/badge/SpringBoot-3.3.5-6DB33F?logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring Data JPA-6DB33F?logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring Batch-6DB33F?logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring WebFlux-6DB33F?logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring Actuator-6DB33F?logo=spring&logoColor=white"> |
| **Security** | <img src="https://img.shields.io/badge/Spring Security-6DB33F?logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/Cloudflare-F38020?logo=cloudflare&logoColor=white">                                                                                                                                                                                                                                                                                                                                                                 |
| **Database** | <img src="https://img.shields.io/badge/PostgreSQL-17.5-4169E1?logo=postgresql&logoColor=white"> <img src="https://img.shields.io/badge/Amazon RDS-527FFF?logo=amazonrds&logoColor=white">                                                                                                                                                                                                                                                                                                                                                                      |
| **Infra & Cache** | <img src="https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white">                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **Messaging** | <img src="https://img.shields.io/badge/Apache Kafka-231F20?logo=apachekafka&logoColor=white"> <img src="https://img.shields.io/badge/Confluent Kafka-0052CC?logo=confluent&logoColor=white">                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **Build & Dependency** | <img src="https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white">                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| **Deployment & CI/CD** | <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/GitHub Actions-2088FF?logo=githubactions&logoColor=white"> <img src="https://img.shields.io/badge/Amazon EC2-FF9900?logo=amazonec2&logoColor=white"> <img src="https://img.shields.io/badge/Amazon ECS-FF9900?logo=amazon-ecs&logoColor=white"> <img src="https://img.shields.io/badge/Amazon ECR-FF9900?logo=amazon-ecr&logoColor=white">                                                                                           |
| **Storage** | <img src="https://img.shields.io/badge/Amazon S3-569A31?logo=amazons3&logoColor=white">                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| **Monitoring** | <img src="https://img.shields.io/badge/Micrometer-1081C2?logo=micrometer&logoColor=white">                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| **Testing** | <img src="https://img.shields.io/badge/JUnit5-25A162?logo=junit5&logoColor=white"> <img src="https://img.shields.io/badge/Mockito-8A2BE2?logo=mockito&logoColor=white"> <img src="https://img.shields.io/badge/JaCoCo-Code%20Coverage-BE312E?logo=java&logoColor=white">                                                                                                                                                                                                                                                                                       |
| **Collaboration** | <img src="https://img.shields.io/badge/Git-F05032?logo=git&logoColor=white"> <img src="https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white"> <img src="https://img.shields.io/badge/Discord-5865F2?logo=discord&logoColor=white"> <img src="https://img.shields.io/badge/Notion-000000?logo=notion&logoColor=white">                                                                                                                                                                                                                        |
| **IDE** | <img src="https://img.shields.io/badge/IntelliJ IDEA-000000?logo=intellijidea&logoColor=white">                                                                                                                                                                                                                                                                                                                                                                                                                                                                |

--- 

## 프로젝트 실행 가이드
### 1. 필수 소프트웨어
- Java 17
- Gradle
- PostgreSQL (v15 이상 권장)
- Docker & Docker Compose (Redis, Kafka 실행용)
- 인터넷 연결 (외부 API 사용)

### 2. 환경 변수 설정
1. 프로젝트 루트에 `.env` 파일 생성
2. 다음을 참고하여 `.env` 파일 채우기
```
# 기타
SPRING_PROFILES_ACTIVE=dev

WEBSOCKET_ALLOWED_ORIGINS=http://localhost:8080

# Docker 관련
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

POSTGRES_USER=
POSTGRES_PASSWORD=

# Redis 관련
REDIS_HOST=playlist-redis
REDIS_PORT=6379

# Admin 관련
ADMIN_USER=admin
ADMIN_EMAIL=admin@playlist.com
ADMIN_PASSWORD=testtest

# jwt 관련
JWT_ACCESS_TOKEN_SECRET=mySuperSecureSecret12313@@!$!@$!@$@!!@1215135636
JWT_REFRESH_TOKEN_SECRET=mySuperSecureSecretKeyF!@!%&sandoaneoOFDFNrasndo

JWT_ACCESS_TOKEN_EXPIRATION_MS=3600000
JWT_REFRESH_TOKEN_EXPIRATION_MS=604800000

# 소셜 로그인에서 이메일 발송을 위한 환경변수
SPRING_MAIL_HOST=
SPRING_MAIL_PORT=
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_PASSWORD=
KAKAO_REST_API_KEY=
KAKAO_CLIENT_SECRET=

# S3 관련
CONTENT_BUCKET=
PROFILE_BUCKET=
LOGS_BUCKET=
AWS_REGION=

# 콘텐츠 외부 API 관련
TMDB_API_KEY=
TMDB_BASE_URL=https://api.themoviedb.org/3
SPORTSDB_API_KEY=123
SPORTSDB_BASE_URL=https://www.thesportsdb.com/api/v1/json

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```
> ⚠️ `.env` 파일은 민감 정보가 포함되어 있으므로 Git에 커밋하지 마세요.


* Redis: 캐시 및 세션 관리
* Kafka: 이벤트/비동기 메시징 처리

### 3. 데이터베이스 설정 (PostgreSQL)

#### 기본

* PostgreSQL 실행
* DB 생성 후 `.env`에 접속 정보 입력


### 4. 외부 API

* 콘텐츠 데이터 수집을 위해 외부 API 사용

  * themoviedb : https://www.themoviedb.org/
  * thesportsdb : https://www.thesportsdb.com/
* `.env`에 API Key 입력 필요

* https://playlist-team1.me/#/contents/adminMovie 로 포스트맨에서 GET 요청시 콘텐츠를 가져올 수 있음

### 5. AWS (운영 환경)

* 운영 환경에서만 사용

  * S3: 파일 저장
  * EC2 / ECS: 배포
* 로컬 실행 시 AWS 설정은 필수 아님
* 프로필 수정 및 콘텐츠 생성은 S3가 운영환경에서만 연결되어 있어서 불가합니다.

### 6. 인프라 서비스 및 프로젝트 실행

```bash
docker compose up -d
```

브라우저 접속:

```
http://localhost:8080
```

- 어드민 계정으로 로그인 (Admin 관련에서 수정가능)
  - 이메일: admin@playlist.com
  - 비밀번호: testest

--- 

## **파일 구조**
```
src/main
├─java
│  └─com
│      └─codeit
│          └─playlist
│              ├─domain
│              │  ├─auth
│              │  │  ├─controller
│              │  │  ├─exception
│              │  │  ├─passwordratelimit
│              │  │  └─service
│              │  │      └─basic
│              │  ├─base
│              │  ├─content
│              │  │  ├─api
│              │  │  │  ├─controller
│              │  │  │  ├─handler
│              │  │  │  ├─mapper
│              │  │  │  ├─response
│              │  │  │  ├─scheduler
│              │  │  │  └─service
│              │  │  ├─batch
│              │  │  ├─controller
│              │  │  ├─dto
│              │  │  │  ├─data
│              │  │  │  ├─request
│              │  │  │  └─response
│              │  │  ├─entity
│              │  │  ├─exception
│              │  │  ├─mapper
│              │  │  ├─repository
│              │  │  └─service
│              │  │      └─basic
│              │  ├─conversation
│              │  │  ├─controller
│              │  │  ├─dto
│              │  │  │  ├─data
│              │  │  │  ├─request
│              │  │  │  └─response
│              │  │  ├─entity
│              │  │  ├─exception
│              │  │  ├─mapper
│              │  │  ├─repository
│              │  │  └─service
│              │  │      └─basic
│              │  ├─file
│              │  │  ├─exception
│              │  │  └─scheduler
│              │  ├─follow
│              │  │  ├─controller
│              │  │  ├─dto
│              │  │  │  ├─data
│              │  │  │  └─request
│              │  │  ├─entity
│              │  │  ├─exception
│              │  │  ├─mapper
│              │  │  ├─repository
│              │  │  └─service
│              │  │      └─basic
│              │  ├─message
│              │  │  ├─controller
│              │  │  ├─dto
│              │  │  │  ├─data
│              │  │  │  ├─request
│              │  │  │  └─response
│              │  │  ├─entity
│              │  │  ├─event
│              │  │  │  ├─listener
│              │  │  │  └─message
│              │  │  ├─exception
│              │  │  ├─mapper
│              │  │  ├─repository
│              │  │  └─service
│              │  │      └─basic
│              │  ├─notification
│              │  │  ├─controller
│              │  │  ├─dto
│              │  │  │  ├─data
│              │  │  │  └─response
│              │  │  ├─entity
│              │  │  ├─exception
│              │  │  ├─mapper
│              │  │  ├─repository
│              │  │  │  └─custom
│              │  │  └─service
│              │  │      └─basic
│              │  ├─playlist
│              │  │  ├─controller
│              │  │  ├─dto
│              │  │  │  ├─data
│              │  │  │  ├─request
│              │  │  │  └─response
│              │  │  ├─entity
│              │  │  ├─exception
│              │  │  ├─mapper
│              │  │  ├─repository
│              │  │  │  └─custom
│              │  │  ├─scheduler
│              │  │  └─service
│              │  │      └─basic
│              │  ├─review
│              │  │  ├─controller
│              │  │  ├─dto
│              │  │  │  ├─data
│              │  │  │  ├─request
│              │  │  │  └─response
│              │  │  ├─entity
│              │  │  ├─exception
│              │  │  ├─mapper
│              │  │  ├─repository
│              │  │  │  └─custom
│              │  │  └─service
│              │  │      └─basic
│              │  ├─security
│              │  │  ├─jwt
│              │  │  └─oauth
│              │  ├─sse
│              │  │  ├─controller
│              │  │  ├─entity
│              │  │  ├─exception
│              │  │  ├─repository
│              │  │  └─service
│              │  ├─user
│              │  │  ├─controller
│              │  │  ├─dto
│              │  │  │  ├─data
│              │  │  │  ├─request
│              │  │  │  └─response
│              │  │  ├─entity
│              │  │  ├─event
│              │  │  ├─exception
│              │  │  ├─mapper
│              │  │  ├─repository
│              │  │  └─service
│              │  │      └─basic
│              │  └─watching
│              │      ├─controller
│              │      ├─dto
│              │      │  ├─data
│              │      │  ├─request
│              │      │  └─response
│              │      ├─event
│              │      │  ├─publisher
│              │      │  └─subscriber
│              │      ├─exception
│              │      ├─repository
│              │      └─service
│              │          └─basic
│              └─global
│                  ├─config
│                  ├─constant
│                  ├─error
│                  ├─init
│                  ├─interceptor
│                  ├─kafka
│                  └─redis
└─resources
    └─static
        └─assets
```

--- 

## **팀원 구성** 

| 이름   | 이메일                       | 담당 기능 | 개인 회고록 |
|--------|---------------------------|-----------|-------------|
| 김찬호 | cheis11@naver.com         | 프로필 관리, Kafka, SSE | [🦖](https://docs.google.com/document/d/1OHjWLT_i-iKscc_SW_PjWkYaNotDdbGBrM0eMv7blaE/edit?usp=sharing) |
| 강은혁 | dmsgur7370@gmail.com      | 사용자 관리 | [🦖]() |
| 신은수 | sin9801@naver.com         | 팀장, 콘텐츠 평가 및 큐레이팅, 알림 | [⚙️]() |
| 안중원 | anjoongwon517@gmail.com   | 실시간 같이 보기, Redis, CI/CD, 배포 | [😎]() |
| 신동진 | index.librorum.prohibitorum27@gmail.com                  | 콘텐츠 데이터 관리 | [⛱️]() |
