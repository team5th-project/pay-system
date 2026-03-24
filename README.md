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
> 세션 대신 JWT 기반 인증 방식을 사용하여 구현했습니다.

#### ◻ Soft Delete
> 데이터를 실제 삭제하지 않고 deleted 플래그를 활용해 논리 삭제 처리함으로써 데이터 복구 가능성과 이력 추적성을 확보했습니다.

#### ◻ PasswordEncoder
> 비밀번호를 단방향 해시 방식으로 암호화하여 DB에 평문이 저장되지 않도록 하고, 로그인 시 안전한 비교가 가능하도록 구현했습니다.

#### ◻ 전역 예외 처리 (CommonError, CommonException, GlobalExceptionHandler)
> 비즈니스 예외와 시스템 예외를 분리하고, 전역 예외 처리기를 통해 일관된 에러 응답 형식을 제공하여 클라이언트가 오류 상황을 명확히 인지할 수 있도록 했습니다.

#### ◻ JPA / JPQL
> 객체 중심의 데이터 접근을 위해 JPA를 사용하고, 복잡한 조회는 JPQL을 활용하여 엔티티 기반 쿼리를 작성함으로써 유지보수성과 가독성을 높였습니다.

#### ◻ QueryDSL
> 정렬/검색 조건이 동적으로 변하는 조회 API에 QueryDSL을 적용하여 타입 안정성을 확보하고, 복잡한 조건 조합을 코드 기반으로 안전하게 구성했습니다.

#### ◻ Builder + record
> DTO 생성 시 Builder 패턴을 사용해 가독성과 유지보수성을 높이고, 불변 데이터 전달 객체에는 record를 활용하여 코드량을 줄이고 안정성을 확보했습니다.

#### ◻ Validation
> 요청 데이터에 대해 길이 제한, 필수값 검증 등 입력 검증 로직을 적용하여 잘못된 요청을 사전에 차단하고 서비스 안정성을 높였습니다.

#### ◻ 페이징 조회
> 대량 데이터 조회 시 Page 기반 페이징 처리를 적용하여 응답 속도를 개선하고, 클라이언트가 필요한 데이터만 효율적으로 조회할 수 있도록 구현했습니다.

#### ◻ BaseEntity
> 엔티티 공통 필드(createdAt, modifiedAt 등)를 BaseEntity로 분리하여 중복 코드를 제거하고, 모든 도메인에서 동일한 감사(Auditing) 정책을 적용할 수 있도록 설계했습니다.

#### ◻ 공통 응답 DTO (ApiResponse)
> API 응답 구조를 ApiResponse<T> 형태로 통일하여 성공/실패 응답 형식을 일관되게 유지하고, 프론트엔드가 상태·메시지·데이터를 예측 가능하게 처리할 수 있도록 했습니다.

#### ◻ Postman
> API 테스트 도구로 Postman을 활용하여 엔드포인트 검증, 인증 흐름 테스트, 요청/응답 구조 확인 등을 수행했습니다.

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

| 항목 | 버전  |
|---|-----|
| Java | 17  |
| Spring Boot | 4.x |
| Gradle | 8.x |
| MySQL | 8.x |
| JPA | Hibernate |
| IDE | IntelliJ |

---

## 🖼 API 명세서

<p align="center">
  <img src="docs/images/API_명세서.png" width="80%">
</p>

보다 자세한 API 명세서는
[📎프로젝트 노션](https://www.notion.so/teamsparta/2-2ff2dc3ef514805aa074fd80c0ad353d) 에서 확인할 수 있습니다.

---

## 🗄 ERD Diagram

<p align="center">
  <img src="docs/images/ERD_diagram.png" width="80%">
</p>

---

## 📈 프로젝트 파일 구조

```text
src/main/java/com/commerce/manageit/
├── domain/                    # 핵심 비즈니스 로직 (도메인별 분리)
│   ├── admin/                 # 관리자(Admin) 관련 도메인
│   │   ├── controller/        # API 엔드포인트
│   │   ├── dto/               # Request / Response 객체
│   │   ├── entity/            # JPA 엔티티 (Domain Model)
│   │   ├── enums/             # 상태 코드 및 role enum
│   │   ├── repository/        # DB 접근 계층
│   │   └── service/           # 비즈니스 로직
│   │
│   ├── customer/              # 고객(Customer) 관련 도메인
│   ├── dashboard/             # 대시보드(Dashboard) 관련 도메인
│   ├── order/                 # 주문(Order) 관련 도메인
│   ├── product/               # 상품(Product) 관련 도메인
│   └── review/                # 리뷰(Review) 관련 도메인
│   
├── global/                    # 프로젝트 전역 공통 설정
│   ├── common/                # 공통 추상 클래스 (BaseEntity, ApiResponse)
│   ├── error/                 # 예외 처리 (ExceptionHandler, ErrorCode)
│   └── security/              # Framework 설정 (Security, JWT 등)
└── ECommerceBackofficeApplication.java   # 프로젝트 메인 실행 클래스
```

---

## 🚨 Trouble Shooting

👉 [Dashboard Query Optimization - 인덱스 설계를 통한 대시보드 성능 개선](docs/TroubleShooting/Dashboard_Query_Optimization.md) <br>
👉 [Session_Security_Develop - 관리자 인증/인가 방식 진화(3단계)](docs/TroubleShooting/Session_Security_Develop.md) <br>
👉 [Handling Null References in Soft Delete Relationships - Soft Delete 연관관계에서 발생한 Null 참조 처리](docs/TroubleShooting/Handling_Null_References_In_Soft_Delete_Relationships.md) <br>
👉 [API Consistency - Custom AccessDeniedHandler를 통한 예외 응답 규격 통일](docs/TroubleShooting/Security_Exception.md) <br>
👉 [rating 데이터 자료형 (int -> Integer)변경](docs/TroubleShooting/rating_int_Integer.md) <br>
👉 [Customer Search API - QueryDSL & Paging 설계 학습 정리](docs/TroubleShooting/Customer_QueryDSL.md)<br>

---