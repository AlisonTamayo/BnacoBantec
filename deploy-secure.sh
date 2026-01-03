#!/bin/bash
# Script de Automatización de Seguridad BANTEC (Totalmente Automatizado)
# Uso: ./deploy-secure.sh [domain] [duckdns_token]

echo "🚀 Iniciando despliegue seguro automatizado BANTEC..."

# 1. Preparar permisos y variables
chmod +x *.sh
DOMAIN="${1:-bantec-bank.duckdns.org}"
TOKEN="$2"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# 2. Actualizar IP en DuckDNS
if [ -n "$TOKEN" ]; then
    echo "🌐 [0/4] Actualizando DuckDNS..."
    ./update_duckdns.sh "${DOMAIN%%.*}" "$TOKEN"
fi

# 3. Configuración dinámica de Nginx
if [ -f "./fix_nginx_conf.sh" ]; then
    echo "🔧 [1/4] Configurando Nginx para el dominio $DOMAIN..."
    ./fix_nginx_conf.sh nginx/nginx.conf "$DOMAIN"
fi

# 4. Verificar/Generar certificados mTLS para ms-transaccion
# Buscamos en la ruta que usa docker-compose (./ms-transaccion/certs/)
if [ ! -f "./ms-transaccion/certs/bantec-keystore.p12" ]; then
    echo "🔐 [2/4] Generando certificados mTLS por primera vez..."
    ./generate-mtls-certs.sh
else
    echo "✅ [2/4] Certificados mTLS ya existen. Omitiendo generación."
fi

# 5. Verificar/Generar certificados SSL con Let's Encrypt
if [ ! -d "./nginx/certs/live/$DOMAIN" ]; then
    echo "🛡️ [3/4] Iniciando proceso de SSL Let's Encrypt para $DOMAIN..."
    ./init-letsencrypt.sh --auto
else
    # Verificamos si el certificado es real o Dummy (autofirmado)
    if openssl x509 -in "./nginx/certs/live/$DOMAIN/fullchain.pem" -noout -issuer | grep -q "localhost"; then
        echo "⚠️  Certificado DUMMY detectado. Reemplazando por Let's Encrypt..."
        ./init-letsencrypt.sh --auto
    else
        echo "✅ [3/4] Certificado SSL Real ya instalado."
    fi
fi

# 6. Levantar o Reiniciar servicios con construcción total
echo "🏗️ [4/4] Desplegando servicios con Docker Compose..."
# Forzamos construcción para aplicar cambios en el código y variables de entorno
docker-compose -f docker-compose.prod.yml up -d --build --remove-orphans

# 7. Verificación de Salud de Nginx
echo "🔍 Verificando estado de Nginx..."
sleep 5
NGINX_STATUS=$(docker inspect -f '{{.State.Running}}' nginx-proxy-bantec 2>/dev/null)
if [ "$NGINX_STATUS" == "true" ]; then
    echo "✅ Nginx está corriendo correctamente."
else
    echo "❌ ERROR: Nginx no pudo arrancar. Revisando logs..."
    docker logs nginx-proxy-bantec | tail -n 20
fi

echo "---------------------------------------------------"
echo "✅ DESPLIEGUE SEGURO COMPLETADO CON ÉXITO"
echo "🌐 URL Banca Web: https://$DOMAIN"
echo "🏧 URL Cajero:    https://$DOMAIN:8443"
echo "---------------------------------------------------"
