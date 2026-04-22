#!/bin/bash

set -euo pipefail

#######################################
# 기본 설정
#######################################
POD_NAME="gorani-dev-pod"

NGINX_CONTAINER="dev-nginx"
NGINX_IMAGE="docker.io/library/nginx:latest"

ECODRIVE_DB_CONTAINER="db-ecodrive"
PAY_DB_CONTAINER="db-pay"
POSTGRES_IMAGE="docker.io/library/postgres:15"

PAY_REDIS_CONTAINER="redis-pay"
REDIS_IMAGE="docker.io/library/redis:7"

# 외부 노출 포트
HOST_PORT_NGINX=8090

# 내부 서비스 포트
NGINX_PORT=8090
ECODRIVE_DB_PORT=5432
PAY_DB_PORT=5433
PAY_REDIS_PORT=6379

# DB 정보
ECODRIVE_DB_NAME="ecodrive_dev"
ECODRIVE_DB_USER="postgres"
ECODRIVE_DB_PASSWORD="gorani"

PAY_DB_NAME="pay_dev"
PAY_DB_USER="postgres"
PAY_DB_PASSWORD="gorani"

# Redis 정보
PAY_REDIS_PASSWORD="gorani"

# 호스트 디렉토리
BASE_DIR="/home/gorani/dev"
NGINX_DIR="${BASE_DIR}/nginx"
DATA_DIR="${BASE_DIR}/data"

ECODRIVE_DB_DATA="${DATA_DIR}/ecodrive-postgres"
PAY_DB_DATA="${DATA_DIR}/pay-postgres"
PAY_REDIS_DATA="${DATA_DIR}/pay-redis"

NGINX_CONF="${NGINX_DIR}/nginx.conf"

#######################################
# 함수
#######################################
log() {
  echo ""
  echo "========== $1 =========="
}

container_exists() {
  local name="$1"
  sudo podman container exists "$name"
}

pod_exists() {
  local name="$1"
  sudo podman pod exists "$name"
}

#######################################
# 1. 디렉토리 준비
#######################################
log "디렉토리 생성"
sudo mkdir -p "${NGINX_DIR}"
sudo mkdir -p "${ECODRIVE_DB_DATA}"
sudo mkdir -p "${PAY_DB_DATA}"
sudo mkdir -p "${PAY_REDIS_DATA}"

#######################################
# 2. nginx.conf 존재 여부 확인
#######################################
log "nginx.conf 확인"

if [ ! -f "${NGINX_CONF}" ]; then
  echo "ERROR: ${NGINX_CONF} 파일이 없습니다."
  echo "먼저 nginx.conf를 ${NGINX_CONF} 경로에 생성해주세요."
  exit 1
fi

#######################################
# 3. pod 생성
#######################################
log "pod 생성"

if pod_exists "${POD_NAME}"; then
  echo "Pod '${POD_NAME}' already exists."
else
  sudo podman pod create \
    --name "${POD_NAME}" \
    -p ${HOST_PORT_NGINX}:${NGINX_PORT}
fi

#######################################
# 4. ecodrive DB 생성
#######################################
log "ecodrive DB 컨테이너 생성"

if container_exists "${ECODRIVE_DB_CONTAINER}"; then
  echo "Container '${ECODRIVE_DB_CONTAINER}' already exists."
else
  sudo podman run -d \
    --name "${ECODRIVE_DB_CONTAINER}" \
    --pod "${POD_NAME}" \
    --restart always \
    -e POSTGRES_DB="${ECODRIVE_DB_NAME}" \
    -e POSTGRES_USER="${ECODRIVE_DB_USER}" \
    -e POSTGRES_PASSWORD="${ECODRIVE_DB_PASSWORD}" \
    -e PGPORT="${ECODRIVE_DB_PORT}" \
    -v "${ECODRIVE_DB_DATA}:/var/lib/postgresql/data:Z" \
    "${POSTGRES_IMAGE}"
fi

#######################################
# 5. pay DB 생성
#######################################
log "pay DB 컨테이너 생성"

if container_exists "${PAY_DB_CONTAINER}"; then
  echo "Container '${PAY_DB_CONTAINER}' already exists."
else
  sudo podman run -d \
    --name "${PAY_DB_CONTAINER}" \
    --pod "${POD_NAME}" \
    --restart always \
    -e POSTGRES_DB="${PAY_DB_NAME}" \
    -e POSTGRES_USER="${PAY_DB_USER}" \
    -e POSTGRES_PASSWORD="${PAY_DB_PASSWORD}" \
    -e PGPORT="${PAY_DB_PORT}" \
    -v "${PAY_DB_DATA}:/var/lib/postgresql/data:Z" \
    "${POSTGRES_IMAGE}"
fi

#######################################
# 6. pay Redis 생성
#######################################
log "pay Redis 컨테이너 생성"

if container_exists "${PAY_REDIS_CONTAINER}"; then
  echo "Container '${PAY_REDIS_CONTAINER}' already exists."
else
  sudo podman run -d \
    --name "${PAY_REDIS_CONTAINER}" \
    --pod "${POD_NAME}" \
    --restart always \
    -v "${PAY_REDIS_DATA}:/data:Z" \
    "${REDIS_IMAGE}" \
    redis-server \
    --port "${PAY_REDIS_PORT}" \
    --appendonly yes \
    --requirepass "${PAY_REDIS_PASSWORD}"
fi

#######################################
# 7. nginx 생성
#######################################
log "nginx 컨테이너 생성"

if container_exists "${NGINX_CONTAINER}"; then
  echo "Container '${NGINX_CONTAINER}' already exists."
else
  sudo podman run -d \
    --name "${NGINX_CONTAINER}" \
    --pod "${POD_NAME}" \
    --restart always \
    -v "${NGINX_CONF}:/etc/nginx/nginx.conf:Z,ro" \
    "${NGINX_IMAGE}"
fi

#######################################
# 8. 상태 확인
#######################################
log "pod 상태 확인"
sudo podman pod ps

log "컨테이너 상태 확인"
sudo podman ps -a --pod

log "완료"
echo "dev 인프라 초기화가 완료되었습니다."
echo "nginx:        http://<dev-server-ip>:${HOST_PORT_NGINX}"
echo "db-ecodrive:  localhost:${ECODRIVE_DB_PORT}"
echo "db-pay:       localhost:${PAY_DB_PORT}"
echo "redis-pay:    localhost:${PAY_REDIS_PORT}"