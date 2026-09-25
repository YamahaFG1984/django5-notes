#!/usr/bin/env bash
# 生成开发用的自签名证书（≈ 书中用 django-extensions + pyOpenSSL 生成 cert.crt）。
# 浏览器会提示“不安全”，开发时手动信任即可；想要无警告，可以改用 mkcert 生成受信任的本地证书。
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p certs
keytool -genkeypair -alias dev -keyalg RSA -keysize 2048 -validity 365 \
  -storetype PKCS12 -keystore certs/dev.p12 -storepass "${SSL_KEYSTORE_PASSWORD:-changeit}" \
  -dname "CN=mysite.com" -ext "SAN=dns:mysite.com,dns:localhost,ip:127.0.0.1"
echo "已生成 certs/dev.p12；在 /etc/hosts 里加一行 127.0.0.1 mysite.com，然后访问 https://mysite.com:8443/"
