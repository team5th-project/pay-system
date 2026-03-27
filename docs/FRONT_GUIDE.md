# 💳 CommerceHub - 결제/구독 UI 템플릿

> 부트캠프용 프론트엔드 UI 템플릿 - Spring Boot + Thymeleaf + PortOne

## 📋 프로젝트 개요

이 프로젝트는 **수강생들이 백엔드 API를 구현하기 위한 프론트엔드 UI 템플릿**입니다.

### 핵심 특징

- ✅ **UI 템플릿만 제공** - 비즈니스 로직 API는 포함하지 않음
- ✅ **API 계약 기반 개발** - `client-api-config.yml`에서 API 계약 정의
- ✅ **PortOne SDK 통합** - 결제창/빌링키 발급 자동화
- ✅ **2가지 독립적인 결제 플로우**
    - **기본 결제**: 일반 카드 결제 (주문 페이지)
    - **포인트 결제**: 포인트 사용 결제 (포인트 페이지)

### 왜 이 프로젝트를 사용하나요?

1. **API 구현에 집중**: 프론트엔드 UI는 이미 완성되어 있어 백엔드 API 구현에만 집중
2. **실전 결제 플로우 학습**: PortOne SDK와 연동된 실제 결제 프로세스 경험
3. **유연한 API 계약**: YML 파일만 수정하면 자신만의 API 설계 가능
4. **독립적인 테스트**: 3가지 결제 플로우를 각각 독립적으로 테스트

## 🎯 결제 플로우 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                     2가지 독립적인 결제 플로우                        │
└─────────────────────────────────────────────────────────────────┘

1️⃣ 기본 결제 (일반 카드 결제)
   상점 → 주문 생성 → 주문 페이지 → 결제 시작 → PortOne 결제창 → 결제 확정

2️⃣ 포인트 결제 (포인트 사용)
   포인트 페이지 → 주문 조회 → 주문 선택 → 포인트 입력 → 결제 → 자동 확정

```

### 왜 분리되었나?

각 결제 플로우는 서로 다른 비즈니스 요구사항을 가지고 있습니다:

| 플로우 | 페이지 | 포인트 사용 | 확정 방식 | 주요 사용처 |
|--------|--------|------------|----------|------------|
| **기본 결제** | 주문 | ❌ | 수동 | 일반 쇼핑몰 결제 |
| **포인트 결제** | 포인트 | ✅ | 자동 | 포인트 할인 결제 |

## 🚀 빠른 시작

### 1. 환경변수 파일 준비

이 프로젝트는 루트 경로의 `.env` 파일을 시작 시 자동으로 로드합니다.

```bash
# 예시 파일 복사
cp .env.example .env
```

`.env`에는 `application.yml`에서 사용하는 외부 설정 값을 넣습니다.

```env
PORTONE_API_SECRET=
PORTONE_STORE_ID=
PORTONE_CHANNEL_KG=
PORTONE_CHANNEL_TOSS=
JWT_SECRET=
JWT_VALIDITY=86400
```

**설명:**
- `PORTONE_API_SECRET`: PortOne REST API Secret
- `PORTONE_STORE_ID`: PortOne Store ID
- `PORTONE_CHANNEL_KG`: 일반 결제용 KG Inicis 채널 키
- `PORTONE_CHANNEL_TOSS`: 구독/빌링키용 Toss 채널 키
- `JWT_SECRET`: JWT 서명용 시크릿 키
- `JWT_VALIDITY`: JWT 만료 시간(초), 기본값 `86400`

**주의사항:**
- `.env.example`은 예시 템플릿이며, 실제 비밀값은 `.env`에만 입력하세요.
- `.env`는 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.
- `.env` 파일이 없어도 애플리케이션은 실행되지만, `application.yml`의 기본값이 사용됩니다.

### 2. 프로젝트 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 IDE에서 실행
# PaymentDemoApplication.java 메인 클래스 실행
```

### 3. 브라우저 접속

```
http://localhost:8080
```

### 4. 기본 계정 (Spring Security)

현재 데모 버전에서는 별도 로그인 없이 사용 가능합니다.

## 📁 프로젝트 구조

```
src/
└── main/
    ├── java/com/bootcamp/paymentdemo/
    │   ├── PaymentDemoApplication.java
    │   ├── common/
    │   │   ├── config/
    │   │   │   ├── DataInitializer.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── RestClientConfig.java
    │   │   │   ├── DotenvInitializer.java
    │   │   │   ├── AppProperties.java
    │   │   │   ├── ClientApiProperties.java
    │   │   │   ├── DemoApiProperties.java
    │   │   │   └── PortOneProperties.java
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── ErrorCode.java
    │   │   │   ├── PortOneException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── ErrorResponse.java
    │   │   │   └── ServiceException.java
    │   │   │
    │   │   ├── global/
    │   │   │   ├── PageResponse.java
    │   │   │   ├── CommonResponseHandler.java
    │   │   │   ├── CommonResponse.java
    │   │   └── BaseEntity.java
    │   ├── controller/    # 페이지/설정/인증 등 공통 컨트롤러
    │   ├── dto/           # 공통 DTO
    │   ├── order/         # 주문 도메인
    │   ├── payment/       # 결제 도메인
    │   ├── point/         # 포인트 도메인
    │   ├── product/       # 상품 도메인
    │   ├── refund/        # 환불 도메인
    │   ├── security/      # JWT, 인증/인가 처리
    │   ├── user/          # 회원 도메인
    │   └── webhook/       # 결제 웹훅 처리
    │
    └── resources/
        ├── static/                # 정적 리소스(css, js 등)
        ├── templates/             # 템플릿 파일
        ├── client-api-config.yml  # 클라이언트 API 설정
        ├── application.yml        # 공통 설정
        ├── application-prod.yml   # 운영 환경 설정
        ├── application-local.yml  # 로컬 환경 설정
        └── data-xxx.sql           # 초기 데이터 SQL           # 구독 관리
```

## ⚙️ 설정 가이드

### 1. 환경변수 기반 설정

`src/main/resources/application.yml`은 다음 값을 환경변수에서 읽습니다:

```yaml
portone:
  api:
    secret: ${PORTONE_API_SECRET:your-api-secret}
  store:
    id: ${PORTONE_STORE_ID:your-store-id}
  channel:
    kg-inicis: ${PORTONE_CHANNEL_KG:your-kg-inicis-channel-key}
    toss: ${PORTONE_CHANNEL_TOSS:your-toss-channel-key}

jwt:
  secret: ${JWT_SECRET:commercehub-secret-key-for-demo-please-change-this-in-production-environment}
  token-validity-in-seconds: ${JWT_VALIDITY:86400}
```

로컬 개발 시에는 프로젝트 루트의 `.env` 파일을 사용하면 됩니다.

---

### 2. PortOne 설정

`src/main/resources/application.yml` 파일에서 PortOne 정보를 설정합니다.
로컬에서는 application-local.yml 파일, 배포 환경에서는 application-prod.yml 파일에서 설정합니다.

```yaml
portone:
  api:
    base-url: https://api.portone.io
    secret: your-api-secret                      # PortOne API Secret
  store:
    id: your-store-id                            # PortOne Store ID
  channel:
    kg-inicis: your-kg-inicis-channel-key        # 일반결제 채널 (카드결제)
    toss: your-toss-channel-key                  # 정기결제 채널 (빌링키)
```

**주의사항:**
- `kg-inicis`: 일반 결제(주문, 포인트)에 사용
- 환경변수로 관리 권장: `${PORTONE_STORE_ID}`, `${PORTONE_API_SECRET}`

---

### 3. API 계약 설정 (가장 중요!)

`src/main/resources/client-api-config.yml` 파일에서 API 계약을 정의합니다.

#### 기본 구조

```yaml
api:
  base-url: http://localhost:8080              # 백엔드 API 서버 주소

  endpoints:
    # API 이름 (키)
    create-order:
      url: /api/orders                          # ⬅️ 엔드포인트 경로
      method: POST                              # HTTP 메서드
      description: 주문 생성
      request:
        fields:
          - name: items                         # 요청 필드명
            type: array                         # 데이터 타입
            required: true                      # 필수 여부
            description: 주문 아이템 배열
      response:
        body:
          fields:
            - name: orderId                     # 응답 필드명
              type: string
              required: true
              description: 생성된 주문 ID
```

#### URL 필드 추가 방법

각 API에는 `url` 필드가 **필수**입니다. URL이 정의되지 않으면 프론트엔드가 API를 호출할 수 없습니다.

**예시: 결제 시작 API 추가**

```yaml
create-payment:
  url: /api/payments          # ⬅️ 이 부분을 반드시 추가!
  method: POST
  description: 결제 시작
  request:
    fields:
      - name: orderId
        type: string
        required: true
      - name: totalAmount
        type: number
        required: true
      - name: pointsToUse
        type: number
        required: false        # 선택 필드
```

#### Path Parameter 사용

URL에 `{paramName}` 형태로 path parameter를 정의할 수 있습니다:

```yaml
confirm-payment:
  url: /api/payments/{paymentId}/confirm    # ⬅️ {paymentId}가 동적으로 치환됨
  method: POST
  description: 결제 확정
```

JavaScript에서는 다음과 같이 사용합니다:

```javascript
const url = await buildApiUrl('confirm-payment', { paymentId: 'pay_123' });
// 결과: http://localhost:8080/api/payments/pay_123/confirm
```

---

### 4. 동적 API 목록 표시

각 페이지는 필요한 API 목록을 `client-api-config.yml`에서 동적으로 읽어와 화면에 표시합니다.

**예시: 주문 페이지가 필요한 API**

```yaml
# 주문 페이지 (orders.html)는 다음 API들이 필요합니다:
create-payment:       # 결제 시작
  url: /api/payments
confirm-payment:      # 결제 확정
  url: /api/payments/{paymentId}/confirm
cancel-payment:       # 결제 취소
  url: /api/payments/{paymentId}/cancel
```

만약 `url` 필드가 비어있거나 없으면:
- ✅ 정의됨: 화면에 API 정보 표시
- ❌ 미정의: 경고 메시지 표시

---

## 📖 사용 가이드

### 1️⃣ 결제 플로우 (포인트 사용 선택)

**목적**: 쇼핑몰 결제 프로세스

#### Step 1: 상품 선택 및 주문 생성

1. **상점** 페이지로 이동
2. 상품 수량 선택 (수량 조절 버튼 사용)
3. 사용할 포인트 선택(포인트 사용량 조절 버튼 사용 및 포인트로 전액 결제 가능)
    - ✅ **API 호출**: `get-my-point`
    - **응답**: `{ currentPoint, graede }`
4. "주문 생성" 버튼 클릭
    - ✅ **API 호출**: `create-order`
    - **요청**: `{ items: [{ productId, quantity }], usedPoint }`
    - **응답**: `{ orderId, totalAmount }`
5. 생성된 주문 ID 확인

#### Step 2: 결제 및 확정

1. **주문 목록** 페이지로 이동
    - ✅ **API 호출**: `list-orders`
2. **사용자 정보 조회** (결제창에 필요)
    - ✅ **API 호출**: `get-current-user`
    - **응답**: `{ customerUid, email, name }`
    - `customerUid`는 PortOne 결제창의 `customer.customerId`로 사용됨
4. "결제 시작" 버튼 클릭
    - ✅ **API 호출**: `create-payment`
    - **요청**: `{ orderId, totalAmount, pointToUse }`
    - **응답**: `{ paymentId }`
    - ✅ **SDK 호출**: `PortOne.requestPayment()` (결제창 열림)
        - `customer.customerId`: customerUid (3번에서 조회)
        - `customer.email`: email (3번에서 조회)
        - `customer.fullName`: name (3번에서 조회)
5. 결제창에서 카드 정보 입력 및 결제 완료
6. "결제 확정" 버튼 클릭
    - ✅ **API 호출**: `confirm-payment`
    - **요청**: `{ paymentId }`
    - **응답**: `{ success, status }`

**필요한 API:**
- `list-products` (상품 목록 조회)
- `get-my-point` (내 포인트 조회)
- `create-order` (주문 생성)
- `list-orders` (주문 목록 조회)
- `get-current-user` (사용자 정보 조회 - 결제창에 필요)
- `create-payment` (결제 시작)
- `confirm-payment` (결제 확정)
- `cancel-payment` (결제 취소)

---

## 💡 핵심 개념

### 1. API 계약 기반 개발 (Contract-First)

`client-api-config.yml`이 **단일 진실 공급원(Single Source of Truth)**입니다:

```yaml
# 프론트엔드는 이 파일만 보고 API를 호출합니다
api:
  base-url: http://localhost:8080
  endpoints:
    create-order:
      url: /api/orders          # ⬅️ 이 경로로 호출
      method: POST              # ⬅️ 이 메서드 사용
      request:
        fields:
          - name: items         # ⬅️ 이 필드명으로 전송
            type: array
```

**장점:**
- API 설계를 먼저 정의하고 구현
- 프론트엔드와 백엔드의 계약이 명확함
- URL, 필드명, 타입을 YML에서 관리
- API가 없으면 화면에 자동으로 경고 표시

---

### 2. 도메인 분리 (Domain Separation)

각 도메인은 독립적으로 동작합니다:

```
Order Domain      → 주문 생성 및 조회
Payment Domain    → 결제 처리 및 확정
Point Domain      → 포인트 적립 및 사용
Subscription Domain → 구독 및 정기결제
```

**예시: 주문 생성은 결제와 분리**

```yaml
# ✅ 주문 생성 (결제 정보 없음)
create-order:
  request:
    fields:
      - name: items
        type: array
        # pointsToUse 없음!

# ✅ 결제 시작 (포인트는 여기서)
create-payment:
  request:
    fields:
      - name: orderId
        type: string
      - name: pointsToUse      # 포인트는 결제 시점에만 사용
        type: number
        required: false
```
---

## 🔧 개발자 가이드

### JavaScript API 호출

#### 1. 설정 로드

```javascript
// 페이지 로드 시 자동 실행됨
const config = await getConfig();
console.log(config.portone.storeId);
console.log(config.api.baseUrl);
```

#### 2. API URL 생성

```javascript
// 일반 엔드포인트
const url = await buildApiUrl('create-order');
// 결과: http://localhost:8080/api/orders

// Path parameter 치환
const url = await buildApiUrl('confirm-payment', { paymentId: 'pay_123' });
// 결과: http://localhost:8080/api/payments/pay_123/confirm
```

#### 3. API 호출

```javascript
const result = await makeApiRequest('create-order', {
    method: 'POST',
    body: {
        items: [
            { productId: 'prod_001', quantity: 2 }
        ]
    }
});

// 응답 검증
if (!result.success) {
    throw new Error(result.error);
}

console.log('Order ID:', result.data.orderId);
```

#### 4. PortOne SDK 호출

```javascript
// 기본 결제 (포인트 없음)
const paymentResult = await openPortOnePayment({
    paymentId: 'pay_123',
    orderName: '상품명',
    totalAmount: 50000,
    currency: 'KRW',
    payMethod: 'CARD',
    customer: {
        customerId: 'cust_001',
        fullName: '홍길동',
        email: 'test@example.com',
        phoneNumber: '01012345678'
    }
});

// 포인트 결제 (포인트 포함)
const paymentResult = await openPortOnePaymentWithPoints({
    paymentId: 'pay_123',
    orderName: '상품명',
    totalAmount: 50000,
    pointsToUse: 5000,          // 포인트 5000원 사용
    currency: 'KRW',
    payMethod: 'CARD',
    customer: { ... }
});

// 빌링키 발급
const billingResult = await issuePortOneBillingKey({
    issueId: 'issue_001',
    issueName: '정기결제 등록',
    customer: { ... }
});
```

---

## ❓ FAQ

### Q1. `client-api-config.yml`에 URL이 없으면 어떻게 되나요?

**A:** 프론트엔드가 API를 호출할 수 없습니다. 각 페이지 상단에 다음과 같은 경고가 표시됩니다:

```
⚠️ 주의: 다음 백엔드 API가 필요합니다
❌ POST /api/payments (미정의)
```

**해결 방법:** `client-api-config.yml`에 `url` 필드를 추가하세요:

```yaml
create-payment:
  url: /api/payments      # ⬅️ 이 필드를 추가!
  method: POST
  # ...
```
---

### Q2. 상점에서 주문 생성 후 바로 결제할 수 없나요?

**A:** 의도적으로 분리했습니다:

- **상점/주문/포인트 페이지**: 주문만 생성 (장바구니 역할)
- **주문목록조회/결제 페이지**: 결제 처리 (결제 모듈 역할)

**이렇게 분리하면:**
1. 주문과 결제 도메인을 명확히 구분
2. 각 기능을 독립적으로 테스트
3. 향후 확장 시 유연하게 대응 가능

---

### Q3. API가 구현되지 않았는데 어떻게 테스트하나요?

**A:** 각 페이지는 필요한 API가 `client-api-config.yml`에 정의되어 있는지 확인하고:

- ✅ 정의됨: API 정보 표시, 호출 가능
- ❌ 미정의: 경고 표시, 호출 불가

**단계별 개발 흐름:**

1. `client-api-config.yml`에 API 계약 정의 (url, 필드 등)
2. 프론트엔드에서 해당 API 호출 테스트 (에러 확인)
3. 백엔드 API 구현
4. 프론트엔드와 통합 테스트

---

### Q4. `get-current-user`는 언제 호출하나요?

**A:** PortOne 결제창 또는 빌링키 발급 전에 **반드시** 호출해야 합니다.

**이유:** PortOne SDK는 고객 정보(`customer`)가 필요합니다:

```javascript
// PortOne 결제창에 필요한 정보
{
  customer: {
    customerId: "cust_001",      // ⬅️ get-current-user의 customerUid
    email: "test@example.com",   // ⬅️ get-current-user의 email
    fullName: "홍길동",           // ⬅️ get-current-user의 name
    phoneNumber: "01012345678"
  }
}
```

**호출 시점:**

| 플로우 | 호출 시점 | 사용 목적 |
|--------|----------|----------|
| 기본 결제 | 결제 시작 버튼 클릭 전 | 결제창 customer 정보 |

**주의사항:**
- `customerUid`는 서버에서 관리하는 고유 식별자입니다
- 프론트엔드에서 임의로 생성하지 마세요
- 페이지 로드 시 한 번 조회하고 캐싱해서 사용할 수 있습니다

---

### Q5. CORS 에러가 발생합니다.

**A:** 백엔드 서버에서 CORS를 허용해야 합니다:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:8080")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

### Q6. JWT 토큰은 어떻게 관리하나요?

- HttpOnly 쿠키 사용 (XSS 방지)
- Secure 플래그 설정 (HTTPS)
- Refresh Token 구현
- 토큰 만료 시 재발급

---

### Q7. `base-url`을 변경하려면?

**A:** `client-api-config.yml`의 `api.base-url`을 수정하세요:

```yaml
api:
  base-url: http://localhost:9090    # ⬅️ 백엔드 서버 주소 변경
```

모든 API 호출이 자동으로 이 주소를 사용합니다.

---

## 🐛 문제 해결

### 설정이 로드되지 않는 경우

1. 브라우저 개발자 도구 → Network 탭 확인
2. `/api/public/config` 엔드포인트 응답 확인
3. 콘솔에서 `window.APP_RUNTIME.config` 확인

### PortOne SDK 에러

1. PortOne SDK 스크립트 로드 확인 (브라우저 콘솔)
2. Store ID / Channel Key 정확성 확인
3. 브라우저 콘솔에서 에러 메시지 확인

### API 호출 실패

1. `client-api-config.yml`의 `base-url` 확인
2. `url` 필드가 정의되어 있는지 확인
3. CORS 설정 확인 (백엔드 서버)
4. 브라우저 개발자 도구 → Network 탭에서 요청/응답 확인

### 포인트 결제 시 금액이 이상합니다

1. 서버에서 포인트 차감 로직 확인:
   ```
   finalAmount = totalAmount - pointsToUse
   ```
2. `create-order` API가 `usedPoint` 를 올바르게 처리하는지 확인
3. `create-payment` API가 `pointsToUse`를 올바르게 처리하는지 확인
4. PortOne SDK에 전달되는 금액이 `finalAmount`인지 확인

---

## 📚 참고 자료

### PortOne 문서
- [PortOne 개발자 문서](https://developers.portone.io/)
- [PortOne SDK v2](https://developers.portone.io/docs/ko/sdk/browser-sdk)

### Spring Boot
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Thymeleaf 공식 문서](https://www.thymeleaf.org/documentation.html)

---

## 📝 라이선스

이 프로젝트는 교육 목적으로 제공됩니다.

---

## 🤝 기여

버그 리포트나 개선 제안은 이슈로 등록해주세요.
