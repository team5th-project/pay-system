<h1 align="center">🛒 Payment System</h1>

<p align="center">
  결제 시스템 프로젝트<br>
</p>

---

<p align="center">

<img src="https://img.shields.io/badge/Java-17-red">
<img src="https://img.shields.io/badge/SpringBoot-4.x-green">
<img src="https://img.shields.io/badge/JPA-Hibernate-orange">
<img src="https://img.shields.io/badge/MySQL-8-blue">
<img src="https://img.shields.io/badge/Gradle-8-02303A">
<img src="https://img.shields.io/badge/GitHub-Repository-black">

</p>

---

## 📌 프로젝트 소개

이 프로젝트는 **커머스 결제 시스템** 으로  
이커머스의 핵심 기능인 결제 시스템을 구현하는 방법을 학습했습니다.  
사용자는 결제 시 포인트를 사용해 할인된 금액으로 상품을 구매할 수 있으며, 주문 확정 전까지 환불등의 서비스를 이용할 수 있습니다.

요구사항
- 다중 상품을 한 번에 주문할 수 있는 기능 필요
- **PortOne + KG 이니시스 기반**의 일반 카드 결제 기능 필요
- 사용자가 주문 시 **포인트**를 결제 금액의 일부 또는 전부로 사용할 수 있는 기능 필요
- 결제 성공 시 주문 생성, 재고 차감, 포인트 사용/적립이, 실패 시 모든 작업이 롤백(Rollback)되는 **트랜잭션**의 기능 필요
- 데이터 증가에 따른 **페이징** 기능 필요
- Spring Security를 연동하여, 인증된 사용자만이 자신의 포인트를 조회하고 사용할 수 있는 **접근제어** 기능 필요

---

## 👥 팀소개

| 이름  | 역할               | 담당                       |
|-----|------------------|--------------------------|
| 권지원 | 공통, 상품, 인증인가 API | JWT 기반 인증 인가 구현          |
| 김예은 | 포인트 API, 배포      | 포인트 적립, 사용, 환불등 관련 기능 구현 |
| 박소영 | 결제 API           | 결제 생성, 승인, 확정, 검증 기능 구현  |
| 신현민 | 결제 API           | 환불 요청, 목록 조회, 웹훅 기능 구현   |
| 정민교 | 주문 API, 배포       | 주문 생성, 확정, 조회 기능 구현      |

<br>

### [📎프로젝트 노션 바로가기](https://www.notion.so/teamsparta/31e2dc3ef51480ee8956ddfe4af39dc1)

<br>

---

## ⏲️ 개발기간
- 2026.03.16(월) ~ 2026.03.27(금)

---

## 🔧 Technologies & Tools (BE)

사용 Tool


---

## 🧠 적용 기술
#### ◻ Spring Security
> 인가/인증 처리를 필터 체인 기반으로 구성하여 API 접근 권한을 통제하고, 인증되지 않은 요청을 차단하는 보안 구조를 적용했습니다.

#### ◻ JWT
> JWT 기반 인증 방식을 사용하여 구현했습니다, 로그아웃 시 blackList 를 사용하여 이전 token 으로 로그인 할 수 없도록 수정했습니다. 

#### ◻ 공통 응답 처리 (CommonResponse, CommonException, GlobalExceptionHandler, CommonResponseHandler)
> 프론트에서의 원활한 처리를 위해서 API 응답 구조를 통일하고, 공통 응답 내부에 성공 반환값과 에러 반환값을 정리하여 사용했습니다. 

#### ◻ Validation
> 요청 데이터에 대해 길이 제한, 필수값 검증 등 입력 검증 로직을 적용하여 잘못된 요청을 사전에 차단하고 서비스 안정성을 높였습니다.

#### ◻ 공통 응답 DTO (ApiResponse)
> API 응답 구조를 ApiResponse<T> 형태로 통일하여 성공/실패 응답 형식을 일관되게 유지하고, 프론트엔드가 상태·메시지·데이터를 예측 가능하게 처리할 수 있도록 했습니다.

#### ◻ Postman
> 프론트엔드 화면 기반으로 기능을 검증하며 개발을 진행하고, 일부 API는 Postman을 활용하여 단위 테스트 및 요청/응답 구조를 확인했습니다.

---

## 🚀 주요 기능

### Admin
- 관리자 회원가입/로그인/로그아웃
- 관리자 목록/상세 조회
- 관리자 정보 수정
- 관리자 역할/상태 변경
- 관리자 삭제 (Soft Delete)
- 관리자 승인/거부 처리
- 내 프로필 조회/수정
- 비밀번호 변경

### Customer
- 고객 목록/상세 조회
- 고객 정보 수정
- 고객 상태 변경
- 고객 삭제 (Soft Delete)

### Dashboard
- 관리자 / 고객 / 상품 / 주문 / 리뷰 카운트
- 총 매출 / 상태별 주문 수 집계
- 리뷰 평점 분포 차트
- 고객 상태 분포
- 카테고리 분포
- 최근 주문 목록 조회

### Order
- 주문 생성
- 주문 목록/상세 조회
- 주문 상태 변경
- 주문 취소

### Product
- 상품 등록
- 상품 목록/상세 조회
- 상품 정보 수정
- 상품 재고/상태 변경
- 상품 삭제 (Soft Delete)

### Review
- 리뷰 목록/상세 조회
- 리뷰 삭제
- 상품별 리뷰 조회

---

## 🖥 Development Environment

- **Spring Data JPA (Hibernate)**  
  → 반복적인 CRUD 코드를 줄이고, 객체 중심의 도메인 설계를 통해 생산성과 유지보수성을 높이기 위해 사용했습니다.

- **Spring Security + JWT**  
  → 서버 상태를 유지하지 않는 Stateless 인증 방식을 적용하여 확장성과 보안성을 고려한 인증 구조를 구현했습니다.

- **H2 / MySQL 분리**  
  → 로컬 개발 환경에서는 H2를 사용해 빠른 테스트가 가능하도록 하고, 운영 환경에서는 MySQL을 사용해 안정적인 데이터 관리를 할 수 있도록 구성했습니다.

- **Thymeleaf**  
  → 서버 사이드 렌더링 기반으로 프론트엔드와의 빠른 연동 및 동적 페이지 구성을 위해 사용했습니다.

- **Validation**  
  → 요청 데이터에 대한 사전 검증을 통해 잘못된 입력을 방지하고, API 안정성을 높이기 위해 적용했습니다.

- **dotenv**  
  → 환경 변수 및 민감 정보(DB, API Key 등)를 코드와 분리하여 보안성과 설정 관리 편의성을 확보했습니다.

- **JWT (jjwt)**  
  → 토큰 기반 인증을 구현하여 사용자 인증 정보를 안전하게 전달하고 관리할 수 있도록 했습니다.

- **PortOne API**  
  → 실제 결제 흐름을 구현하기 위해 외부 결제 API를 연동하고, 결제 승인 및 검증 로직을 처리했습니다.

---

## 🖼 API 명세서

API 명세서

보다 자세한 API 명세서는
[📎프로젝트 노션](https://www.notion.so/teamsparta/2-2ff2dc3ef514805aa074fd80c0ad353d) 에서 확인할 수 있습니다.

---

## 🗄 ERD Diagram

ERD

---

## 📁 Project Structure

도메인 중심 구조로 패키지 분리했습니다.
```text
├── src/main/java/com/bootcamp/paymentdemo
│
├── common/                     # 공통 모듈
│   ├── config/                 # 설정 관련 클래스
│   ├── exception/              # 예외 처리
│   └── global/                 # 공통 응답, BaseEntity 등
│
├── payment/                    # 결제 도메인
│   └── service/                # PaymentService, 검증/취소 로직 등
│
├── point/                      # 포인트 도메인
│   ├── service/                # 포인트 적립/차감 로직
│   └── scheduler/              # 포인트 관련 스케줄링 처리
├── refund/                     # 환불 도메인
│   └── service/                # 환불 처리 및 PortOne 연동
├── order/                      # 주문 도메인
├── product/                    # 상품 도메인
├── user/                       # 사용자 도메인
│
├── security/                   # 인증/인가 처리
│   ├── token/                  # JWT 관련 처리
│   ├── JwtAuthenticationFilter
│   └── JwtTokenProvider
│
├── resources/
│   ├── static/
│   ├── templates/
│   ├── application.yml
│   ├── application-local.yml
│   └── application-prod.yml
│
└── PaymentDemoApplication      # 프로젝트 메인 실행 클래스
```

---

## 🚨 Trouble Shooting

트러블 슈팅 정리

---
