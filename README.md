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

### 1. 주문 (Order)

#### 1.1 주문 생성
> 주문 생성 API 제공  
> 주문 상품, 수량, 금액 정보 저장  
> 초기 상태: `CREATED`  

#### 1.2 주문 상태 관리
> 상태 흐름   
  `CREATED → PAID → CONFIRMED → CANCELLED`  
> 상태 변경 시 비즈니스 로직 트리거  

#### 1.3 주문 자동 확정 (Scheduler)
> 일정 기간(7일) 경과 시 자동으로 `CONFIRMED` 처리  
> 사용자 액션 의존 제거  
> 포인트 적립 트리거  

---

### 2. 결제 (Payment)

#### 2.1 결제 시도 기록
> 결제 요청 시 Payment Record 생성  
> 주문과 결제 매핑 (`orderId` 기준)  

#### 2.2 외부 결제 연동
> PortOne 결제창 연동  
> `imp_uid`, `order_uid` 기반 결제 식별  

#### 2.3 결제 검증
> 웹훅 또는 API를 통해 결제 결과 수신  
> PortOne API 호출로 실제 결제 데이터 검증  
> 승인 금액 검증 (위변조 방지)  

#### 2.4 멱등성 처리
> 동일 결제 중복 처리 방지  
> 실패 시 결제 취소 처리 (보상 트랜잭션)  

---

### 3. 웹훅 (Webhook)

#### 3.1 웹훅 수신 처리
> 외부 요청 허용 (`permitAll`)  
> 사용자 인증 없이 처리  

#### 3.2 보안 검증
> Header Signature 기반 요청 검증  
> PortOne API 재검증 (이중 검증 구조)  

#### 3.3 주문 매핑
> `imp_uid` → 결제 검증  
> `order_uid` → 내부 주문 식별  

---

### 4. 포인트 (Point)

#### 4.1 포인트 사용  
> 주문 시 포인트 차감  
> 결제 금액에서 차감 반영  

#### 4.2 포인트 적립  
> 주문 확정 시 포인트 적립  
> 실제 결제 금액 기준 적립  

#### 4.3 포인트 정책
> 사용 시점과 적립 시점 분리  
> 부정 적립 방지  

#### 4.4 포인트 이력 관리
> 포인트 거래 내역 저장 (사용 / 적립 / 복구)  

---

### 5. 주문 완료 처리

#### 5.1 결제 완료 후 처리
> 주문 상태 `PAID → CONFIRMED`  
> 재고 차감  
> 포인트 적립  
> 멤버십 등급 업데이트  

#### 5.2 트랜잭션 관리
> 주문 / 결제 / 포인트 / 재고 단일 트랜잭션 처리  
> 실패 시 롤백  

---

### 6. 예외 및 보상 처리

#### 6.1 결제 실패 처리
> 결제 실패 시 주문 상태 유지 또는 취소  

#### 6.2 재고 부족 처리
> 결제 이후 재고 부족 시 결제 취소  
> 외부 API 통해 환불 요청  

#### 6.3 포인트 복구
> 결제 실패 / 취소 시 사용 포인트 복구  

#### 6.4 보상 트랜잭션
> 부분 실패 시 데이터 정합성 유지  

---

### 7. 환불 (Refund)

#### 7.1 환불 요청
> 환불 요청 API 제공  
> 주문 상태 및 환불 가능 여부 검증  

#### 7.2 환불 처리
> PortOne 결제 취소 API 호출  
> 환불 상태 관리  

#### 7.3 환불 후 처리
> 포인트 복구  
> 환불 이력 저장  
> 멤버십 등급 재조정  

---

### 8. 인증 및 보안 (Auth & Security)

#### 8.1 JWT 인증
> Access Token 기반 인증 처리  
> Refresh Token 재발급 구조  
  
#### 8.2 로그아웃 처리
> Access Token 블랙리스트 등록  
> Refresh Token 삭제  

#### 8.3 웹훅 보안
> 사용자 인증 대신 요청 검증 수행  
> Signature 기반 위변조 방지  

___

### 9. 동시성 및 데이터 정합성

#### 9.1 재고 처리
> 동시성 제어 (락 또는 트랜잭션 활용)  

#### 9.2 포인트 처리
> 사용자 포인트 조회 시 Lock 적용  

#### 9.3 데이터 일관성
> 결제 / 주문 / 포인트 상태 정합성 유지  


---

## 🖥 Development Environment

- **Spring Data JPA (Hibernate)**  
  → 반복적인 CRUD 코드를 줄이고, 객체 중심의 도메인 설계를 통해 생산성과 유지보수성을 높이기 위해 사용했습니다.

- **Spring Security + JWT**  
  → 서버 상태를 유지하지 않는 Stateless 인증 방식을 적용하여 확장성과 보안성을 고려한 인증 구조를 구현했습니다.

- **H2 / MySQL 분리**  
  → 로컬 개발 환경에서는 H2를 사용해 빠른 테스트가 가능하도록 하고, 운영 환경에서는 MySQL을 사용해 안정적인 데이터 관리를 할 수 있도록 구성했습니다.

- **Validation**  
  → 요청 데이터에 대한 사전 검증을 통해 잘못된 입력을 방지하고, API 안정성을 높이기 위해 적용했습니다.

- **dotenv**  
  → 환경 변수 및 민감 정보(DB, API Key 등)를 코드와 분리하여 보안성과 설정 관리 편의성을 확보했습니다.

- **JWT**  
  → 토큰 기반 인증을 구현하여 사용자 인증 정보를 안전하게 전달하고 관리할 수 있도록 했습니다.

- **PortOne API**  
  → 실제 결제 흐름을 구현하기 위해 외부 결제 API를 연동하고, 결제 승인 및 검증 로직을 처리했습니다.

- **Docker**  
  → 배포의 안정성을 보장하기 위해 docker를 사용했습니다. 

---

## 🖼 API 명세서

API 명세서

보다 자세한 API 명세서는
[📎프로젝트 노션](https://www.notion.so/teamsparta/2-2ff2dc3ef514805aa074fd80c0ad353d) 에서 확인할 수 있습니다.

---

## 🗄 ER Diagram

<img width="2190" height="1322" alt="oz_maigical_shop_payment_service_erd" src="https://github.com/user-attachments/assets/b0284af5-9107-4f33-9d66-e8c7cee68ee3" />


---

## Flow Chart

<img width="8192" height="715" alt="Car Evaluation Decision Flow-2026-03-19-023632" src="https://github.com/user-attachments/assets/5cf72aa7-6015-4558-ae73-73d8d7c19289" />
<img width="8191" height="793" alt="Car Evaluation Decision Flow-2026-03-19-021934" src="https://github.com/user-attachments/assets/f156dfa0-982c-494c-93bb-de2a847b53c3" />
<img width="8192" height="776" alt="Car Evaluation Decision Flow-2026-03-19-011616" src="https://github.com/user-attachments/assets/f6129e07-f509-4a8b-9f4d-b18065330e54" />


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
