# 🏦 BANTEC - Sistema Bancario Distribuido

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)
![License](https://img.shields.io/badge/License-MIT-yellow)

**Sistema bancario completo con microservicios, transferencias interbancarias y arquitectura cloud-ready**

[Características](#-características) • [Arquitectura](#-arquitectura) • [Despliegue](#-despliegue-rápido) • [Documentación](#-documentación)

</div>

---

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Arquitectura](#-arquitectura)
- [Tecnologías](#-tecnologías)
- [Requisitos Previos](#-requisitos-previos)
- [Despliegue Rápido](#-despliegue-rápido)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Acceso a las Aplicaciones](#-acceso-a-las-aplicaciones)
- [Documentación](#-documentación)
- [Seguridad](#-seguridad)
- [Contribuir](#-contribuir)
- [Licencia](#-licencia)

---

## ✨ Características

### 🎯 Funcionalidades Principales

- **Gestión de Clientes**: Registro y administración de personas y empresas
- **Cuentas de Ahorro**: Creación y gestión de cuentas bancarias
- **Transacciones Locales**: Depósitos, retiros y transferencias internas
- **Transferencias Interbancarias**: Integración con Switch DIGICONECU
- **Banca Web**: Interfaz moderna para clientes
- **Cajero Automático (ATM)**: Interfaz especializada para cajeros
- **API REST**: Documentación completa con Swagger

### 🔒 Seguridad

- **mTLS (Mutual TLS)**: Autenticación mutua con certificados
- **HTTPS/SSL**: Comunicación cifrada con Let's Encrypt
- **Validación de Datos**: Hibernate Validator en todos los endpoints
- **Transacciones ACID**: Garantía de integridad de datos
- **Idempotencia**: Prevención de transacciones duplicadas

### 🚀 Arquitectura Cloud-Ready

- **Microservicios**: Arquitectura distribuida escalable
- **Docker Compose**: Orquestación de contenedores
- **API Gateway**: Punto de entrada único con Spring Cloud Gateway
- **Health Checks**: Monitoreo de salud de servicios
- **Logs Centralizados**: Trazabilidad completa de operaciones

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                      NGINX (SSL Terminator)                  │
│              443 (Web) | 8443 (Cajero) | 80 (HTTP→HTTPS)    │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                     API GATEWAY (8080)                       │
│              Spring Cloud Gateway + Swagger UI               │
└─────┬──────────────┬──────────────┬─────────────────────────┘
      │              │              │
┌─────▼─────┐  ┌────▼─────┐  ┌────▼──────────┐
│  Micro    │  │  Micro   │  │      MS       │
│ Clientes  │  │ Cuentas  │  │ Transacciones │◄──┐
│  (8080)   │  │  (8081)  │  │    (8080)     │   │
└─────┬─────┘  └────┬─────┘  └────┬──────────┘   │
      │             │              │              │
┌─────▼─────┐  ┌───▼──────┐  ┌───▼──────────┐   │
│ PostgreSQL│  │PostgreSQL│  │  PostgreSQL  │   │
│ Clientes  │  │ Cuentas  │  │Transacciones │   │
└───────────┘  └──────────┘  └──────────────┘   │
                                                 │
┌────────────────────────────────────────────────┘
│         Switch DIGICONECU (Interbancario)
│              35.208.155.21:9080
└─────────────────────────────────────────────────
```

### Microservicios

| Servicio | Puerto | Base de Datos | Descripción |
|----------|--------|---------------|-------------|
| **API Gateway** | 8080 | - | Enrutamiento y Swagger centralizado |
| **Micro Clientes** | 8083 | `microcliente` | Gestión de clientes y autenticación |
| **Micro Cuentas** | 8081 | `db_cuentas` | Administración de cuentas y saldos |
| **MS Transacciones** | 8082 | `db_transacciones` | Procesamiento de transacciones + Switch |
| **Frontend Web** | 3000 | - | Banca en línea (React + Vite) |
| **Frontend Cajero** | 3001 | - | Interfaz ATM (React + Tailwind) |

---

## 🛠️ Tecnologías

### Backend
- **Java 21** - Lenguaje de programación
- **Spring Boot 3.4** - Framework de aplicaciones
- **Spring Cloud Gateway** - API Gateway
- **Spring Data JPA** - ORM y persistencia
- **PostgreSQL 17** - Base de datos relacional
- **OpenFeign** - Cliente HTTP declarativo
- **Lombok** - Reducción de código boilerplate
- **SpringDoc OpenAPI** - Documentación Swagger

### Frontend
- **React 18** - Biblioteca de UI
- **Vite** - Build tool y dev server
- **Tailwind CSS** - Framework de estilos
- **Axios** - Cliente HTTP

### DevOps
- **Docker** - Contenedorización
- **Docker Compose** - Orquestación multi-contenedor
- **Nginx** - Reverse proxy y SSL termination
- **Let's Encrypt** - Certificados SSL gratuitos

---

## 📦 Requisitos Previos

- **Docker** 20.10+
- **Docker Compose** 2.0+
- **Git**
- **OpenSSL** (para generar certificados)
- **(Opcional) Java 21 + Maven** (para desarrollo local)

---

## 🚀 Despliegue Rápido

### 1. Clonar el Repositorio

```bash
git clone https://github.com/AlisonTamayo/BnacoBantec.git
cd BnacoBantec
```

### 2. Configurar Certificados SSL

#### Opción A: Certificados de Desarrollo (Autofirmados)
```bash
mkdir -p nginx/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/certs/privkey.pem \
  -out nginx/certs/fullchain.pem \
  -subj "/C=EC/ST=Pichincha/L=Quito/O=Bantec/CN=localhost"
```

#### Opción B: Certificados de Producción (Let's Encrypt)
Ver [GUIA_DESPLIEGUE_VM.md](GUIA_DESPLIEGUE_VM.md) para instrucciones completas.

### 3. Levantar los Servicios

#### Desarrollo Local
```bash
docker-compose up --build -d
```

#### Producción (Google Cloud VM)
```bash
docker-compose -f docker-compose.prod.yml up --build -d
```

### 4. Verificar Estado

```bash
# Ver contenedores corriendo
docker ps

# Ver logs en tiempo real
docker-compose logs -f

# Verificar salud de bases de datos
docker exec db-cuentas-arcbank pg_isready -U postgres
```

---

## 📁 Estructura del Proyecto

```
BnacoBantec/
├── api-gateway/              # Spring Cloud Gateway + Swagger centralizado
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── micro-clientes/           # Microservicio de Clientes
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── micro-cuentas/            # Microservicio de Cuentas
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── ms-transaccion/           # Microservicio de Transacciones + Switch
│   ├── src/
│   │   └── main/resources/certs/  # Certificados mTLS
│   ├── Dockerfile
│   └── pom.xml
├── frontendWeb/              # Frontend Banca Web (React)
│   ├── src/
│   ├── Dockerfile
│   └── package.json
├── frontendCajero/           # Frontend Cajero ATM (React)
│   ├── src/
│   ├── Dockerfile
│   └── package.json
├── nginx/                    # Configuración Nginx
│   ├── nginx.conf
│   └── certs/               # Certificados SSL
├── docker-compose.yml        # Orquestación local
├── docker-compose.prod.yml   # Orquestación producción
├── generate-mtls-certs.sh    # Script generación certificados mTLS
├── GUIA_DESPLIEGUE_VM.md     # Guía de despliegue en Google Cloud
├── CONTEXTO_PROYECTO.md      # Documentación técnica detallada
└── README.md                 # Este archivo
```

---

## 🌐 Acceso a las Aplicaciones

### Desarrollo Local

| Aplicación | URL | Credenciales |
|------------|-----|--------------|
| **Banca Web** | http://localhost:3000 | Ver base de datos |
| **Cajero ATM** | http://localhost:3001 | Ver base de datos |
| **API Gateway** | http://localhost:8080 | - |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | - |

### Producción (Google Cloud)

| Aplicación | URL |
|------------|-----|
| **Banca Web** | https://bantec.35-209-225-8.sslip.io |
| **Cajero ATM** | https://bantec.35-209-225-8.sslip.io:8443 |
| **API Gateway** | http://35.209.225.8:8080 |
| **Swagger UI** | http://35.209.225.8:8080/swagger-ui.html |

---

## 📚 Documentación

- **[CONTEXTO_PROYECTO.md](CONTEXTO_PROYECTO.md)**: Documentación técnica completa
  - Arquitectura detallada
  - Flujos de transacciones
  - Integración con Switch DIGICONECU
  - Configuración de microservicios

- **[GUIA_DESPLIEGUE_VM.md](GUIA_DESPLIEGUE_VM.md)**: Guía de despliegue en Google Cloud
  - Configuración de VMs
  - Instalación de Docker
  - Configuración de SSL/TLS
  - Comandos de mantenimiento
  - Troubleshooting

- **Swagger UI**: Documentación interactiva de APIs
  - Microservicio Clientes: `http://localhost:8083/swagger-ui.html`
  - Microservicio Cuentas: `http://localhost:8081/swagger-ui.html`
  - Microservicio Transacciones: `http://localhost:8082/swagger-ui.html`
  - **Centralizado**: `http://localhost:8080/swagger-ui.html`

---

## 🔐 Seguridad

### Configuración de mTLS

Para habilitar autenticación mutua con el Switch DIGICONECU:

```bash
# 1. Generar certificados
./generate-mtls-certs.sh

# 2. Enviar bantec.crt al Switch
cat ms-transaccion/src/main/resources/certs/bantec.crt

# 3. Recibir certificado del Switch y agregarlo al truststore
keytool -import -alias digiconecu -file switch.crt \
  -keystore ms-transaccion/src/main/resources/certs/bantec-truststore.p12 \
  -storepass bantec123

# 4. Habilitar mTLS en docker-compose.prod.yml
# Agregar: MTLS_ENABLED: "true"
```

### Cambiar Contraseñas en Producción

**IMPORTANTE**: Antes de desplegar en producción, cambiar:
- Contraseñas de PostgreSQL en `docker-compose.prod.yml`
- Contraseñas de keystores/truststores en variables de entorno
- Secretos de JWT (si se implementa autenticación)

---

## 🤝 Contribuir

Las contribuciones son bienvenidas! Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

## 📝 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

---

## 👥 Autores

- **Equipo de Desarrollo BANTEC** - *Desarrollo Inicial*

---

## 🙏 Agradecimientos

- Switch DIGICONECU por la integración interbancaria
- Google Cloud Platform por la infraestructura
- Spring Boot y la comunidad de código abierto

---

<div align="center">

**[⬆ Volver arriba](#-bantec---sistema-bancario-distribuido)**

Hecho con ❤️ por el equipo de BANTEC

</div>
