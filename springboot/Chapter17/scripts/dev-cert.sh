#!/usr/bin/env bash
# 生成自签名证书（≈ 书中用 mkcert/openssl 生成的 ssl/educa.crt），供本机测试 compose.prod.yaml 用。
# 用法：scripts/dev-cert.sh [域名]，默认 educaproject.com；证书同时覆盖 *.域名（课程子域名）。
set -euo pipefail
domain="${1:-educaproject.com}"
dir="$(dirname "$0")/../deploy/certs"
mkdir -p "$dir"
openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout "$dir/educa.key" -out "$dir/educa.crt" \
  -subj "/CN=$domain" \
  -addext "subjectAltName=DNS:$domain,DNS:*.$domain"
echo "Created $dir/educa.crt and $dir/educa.key for $domain and *.$domain"
echo "Add to /etc/hosts:  127.0.0.1 $domain www.$domain django.$domain"
