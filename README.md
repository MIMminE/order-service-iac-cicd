# order-service-iac-cicd

이 저장소는 Terraform + AWS ECS + GitHub Actions를 이용한 코드 기반 인프라(CICD) 실습용 예제입니다.

주요 구성 요소:

- `order-service`: Spring Boot 기반 주문/상품 서비스
- `infra/terraform`: AWS와 연동되는 Terraform 구성
- `.github/workflows`: CI/CD 워크플로우 (빌드/테스트, Docker 빌드/푸시, Terraform plan/apply)

## 1) 로컬 개발

- 로컬 DB(Postgres)는 `docker compose up --build`로 띄우고, `SPRING_DATASOURCE_*` 값을 맞춥니다.
- Gradle로 빌드 및 테스트: `order-service` 디렉터리에서 `./gradlew test` 실행
- 애플리케이션 실행: `./gradlew bootRun` 또는 빌드한 JAR 실행

## 2) GitHub Action

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

## 3) 필수 GitHub Secrets(리포지토리 → Settings → Secrets and variables → Actions)

- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- AWS_REGION
- TF_STATE_BUCKET  (Terraform 원격 상태 S3 버킷)

## 4) 워크플로우 실행 방법

- Terraform Plan: PR을 만들거나 Actions → Terraform Plan → Run workflow로 실행
    - 실행 후 Actions 페이지에서 해당 run을 열어 Artifacts에 `tfplan`이 있는지 확인하세요.
- Terraform Apply(수동): Actions → Terraform Apply → Run workflow
    - `plan_run_id`에 Plan 실행의 run id를 넣으면 그 run의 tfplan을 사용합니다. 비우면 최근 성공한 Plan run을 찾아 사용합니다.
    - `plan_run_id` 얻는 법: Plan 실행 페이지 URL 끝의 숫자(예: .../actions/runs/12345678)
- gh CLI 예시 (옵션):
    - Plan list: `gh run list --workflow="Terraform Plan" --limit 10`
    - Apply 수동 실행: `gh workflow run terraform-apply.yml -f environment=production -f plan_run_id=<RUN_ID>`
