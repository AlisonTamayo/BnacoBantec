#!/bin/bash
# Script de Automatización de Seguridad BANTEC (Totalmente Automatizado)
# Este script es idempotente: si los certificados ya existen, no los recrea a menos que sea necesario.

echo "🚀 Iniciando despliegue seguro automatizado..."

# 1. Preparar permisos
chmod +x generate-mtls-certs.sh init-letsencrypt.sh

# 2. Verificar/Generar certificados mTLS para ms-transaccion
# Si el keystore ya existe, no lo regeneramos para no romper la confianza con el Switch
if [ ! -f "./ms-transaccion/certs/bantec-keystore.p12" ]; then
    echo "🔐 [1/3] Generando certificados mTLS por primera vez..."
    ./generate-mtls-certs.sh
else
    echo "✅ [1/3] Certificados mTLS ya existen. Omitiendo generación."
fi

# 3. Verificar/Generar certificados SSL con Let's Encrypt
# Verificamos si existe la carpeta del dominio en live
if [ ! -d "./nginx/certs/live/bantec-bank.duckdns.org" ]; then
    echo "🌐 [2/3] Iniciando proceso de SSL Let's Encrypt para bantec-bank.duckdns.org..."
    # Ejecutamos el script de inicialización
    # Nota: He modificado el init-letsencrypt.sh para que pueda ser llamado sin interacción
    ./init-letsencrypt.sh --auto
else
    echo "✅ [2/3] Certificados SSL ya existen para bantec-bank.duckdns.org."
fi

# 4. Levantar o Reiniciar servicios
echo "🏗️ [3/3] Desplegando servicios con Docker Compose..."
# Forzamos el levantamiento de los servicios, asegurando que nginx tome los certificados
docker-compose -f docker-compose.prod.yml up -d --remove-orphans

echo "---------------------------------------------------"
echo "✅ DESPLIEGUE SEGURO COMPLETADO"
echo "🌐 URL Banca Web: https://bantec-bank.duckdns.org"
echo "🏧 URL Cajero:    https://bantec-bank.duckdns.org:8443"
echo "---------------------------------------------------"
