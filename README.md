# Order Service IaC/CD

Spring Boot 주문 서비스에 Terraform, AWS ECS, GitHub Actions 배포 흐름을 붙인 백엔드/인프라 포트폴리오입니다. 주문 생성 시 상품 재고를 비관락으로 차감하고, 로컬에서는 운영자가 상품과 주문 흐름을 확인할 수 있는 웹 콘솔을 함께 제공합니다.

![Order console](portfolio/order-console-main.png)

![Order flow](portfolio/order-console-flow.png)

## 핵심 포인트

- Kotlin/Spring Boot 기반 주문, 상품, 인증 API
- JWT 인증과 관리자 권한 기반 상품 등록
- 주문 생성 시 상품 row를 `PESSIMISTIC_WRITE`로 잠그고 재고 차감
- Flyway migration으로 DB 스키마 버전 관리
- Docker Compose 기반 로컬 재현 환경
- Terraform plan/apply와 GitHub Actions 배포 파이프라인 분리

## 로컬 실행

```bash
cd order-service
docker compose up --build -d
```

| Target | URL |
| --- | --- |
| 주문 운영 콘솔 | http://localhost:18082 |
| Health Check | http://localhost:18082/actuator/health |
| PostgreSQL | localhost:15435 |

로컬 콘솔 계정은 `admin@orders.local / password123`입니다. 상품 목록 조회, 상품 등록, 주문 생성, 주문 상세 확인 흐름을 한 화면에서 볼 수 있습니다. 화면은 API 문서 대신 운영자가 보는 콘솔처럼 구성해, 재고 차감 결과와 배포 파이프라인 흐름을 함께 확인할 수 있게 했습니다.

## API 흐름

```bash
curl -X POST http://localhost:18082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@orders.local","password":"password123"}'
```

```bash
curl http://localhost:18082/products
curl -X POST http://localhost:18082/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

## 구조

```text
order-service/src/main/kotlin/.../auth       JWT login and security
order-service/src/main/kotlin/.../product    Product catalog and stock lock query
order-service/src/main/kotlin/.../order      Order creation and stock deduction
order-service/src/main/resources/static      Browser console
infra/terraform                              ECS, ECR, networking, task definition
.github/workflows                            Build, image push, Terraform plan/apply
portfolio                                    Portfolio screenshots and notes
```

## GitHub Actions

- 인증 방식: 워크플로우는 OIDC 대신 GitHub Secrets에 저장된 AWS 액세스 키(`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`)를 사용하도록
  구성되어 있습니다.
- Terraform Plan (`.github/workflows/terraform-plan.yml`)
    - 트리거: Pull Request(또는 수동 `workflow_dispatch`)에서 실행
    - 동작: `terraform plan -out=tfplan`을 실행한 뒤, 생성된 `tfplan` 파일을 아티팩트로 업로드합니다.
    - 안전장치: `tfplan` 파일 존재 여부를 검증하는 스텝을 포함합니다.
- Terraform Apply (`.github/workflows/terraform-apply.yml`)
    - 트리거: 수동 실행(`workflow_dispatch`) — 운영 환경에 대해 수동 승인/검토 후 적용하도록 구성
    - 입력: `plan_run_id`(선택) — Plan을 실행한 run id를 지정하면 해당 run의 아티팩트를 사용합니다. 비우면 최근 성공한 Plan run을 자동 검색해 사용(권장: 직접 run id를
      지정하여 명확히 사용하세요)
    - 동작: 지정된 Plan run에서 업로드한 `tfplan` 아티팩트를 내려받아 `terraform apply`를 수행합니다. Apply 전에 아티팩트 존재를 확인하는 스텝을 추가해 오류 원인을 명확히
      합니다.
- Docker 빌드/푸시 (`.github/workflows/docker-build-and-push.yml`)
    - 트리거: main(또는 config에 따라) 브랜치 푸시
    - 인증: AWS 액세스 키를 사용해 ECR에 로그인하고 이미지를 푸시합니다.
- Build & Test (`.github/workflows/build-and-test.yml`)
    - PR/푸시 시 Gradle 빌드와 테스트를 수행합니다. `working-directory`가 `order-service`로 정확히 지정되어 있으므로 `./gradlew`를 올바르게 실행합니다.

## 필수 GitHub Secrets

- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- AWS_REGION
- TF_STATE_BUCKET  (Terraform 원격 상태 S3 버킷)

## 워크플로우 실행 방법

- Terraform Plan: PR을 만들거나 Actions → Terraform Plan → Run workflow로 실행
    - 실행 후 Actions 페이지에서 해당 run을 열어 Artifacts에 `tfplan`이 있는지 확인하세요.
- Terraform Apply(수동): Actions → Terraform Apply → Run workflow
    - `plan_run_id`에 Plan 실행의 run id를 넣으면 그 run의 tfplan을 사용합니다. 비우면 최근 성공한 Plan run을 찾아 사용합니다.
    - `plan_run_id` 얻는 법: Plan 실행 페이지 URL 끝의 숫자(예: .../actions/runs/12345678)
- gh CLI 예시 (옵션):
    - Plan list: `gh run list --workflow="Terraform Plan" --limit 10`
    - Apply 수동 실행: `gh workflow run terraform-apply.yml -f environment=production -f plan_run_id=<RUN_ID>`
