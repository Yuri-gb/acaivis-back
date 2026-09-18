# Açaívis API

Backend em Java 21 + Spring Boot + PostgreSQL para o projeto Açaívis.

## Stack
- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Bean Validation
- Spring Security + JWT
- SpringDoc OpenAPI
- Docker

## Executar localmente
1. Crie um PostgreSQL chamado `acaivis` ou use `docker compose up -d postgres`.
2. Rode `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
3. Swagger: `http://localhost:8080/swagger-ui.html`.

## Docker
```bash
./mvnw clean package -DskipTests
docker compose up --build
```

## Admin inicial
Por padrão, o projeto cria um admin a partir de `ADMIN_EMAIL` e `ADMIN_PASSWORD`.
Altere essas variáveis antes de qualquer ambiente real.

## Observação
O cliente compra sem conta. O carrinho continua no frontend/localStorage e o backend recebe os itens no checkout, recalculando preços, estoque, taxa de entrega e total.


## Entrega por CEP

O cálculo de entrega é feito pelo backend. O cliente informa apenas o CEP; a API consulta o ViaCEP, valida que o endereço pertence a Feira de Santana/BA e procura o bairro entre os bairros cadastrados em `delivery_zones`. A taxa e a disponibilidade são controladas pelo Admin.

Endpoint público:

`GET /api/delivery/calculate?zipCode=44050422`

Os bairros oficiais do distrito sede são pré-cadastrados pela migration `V6__seed_feira_de_santana_bairros.sql`. Os bairros novos entram inativos com taxa `0,00`; `Centro`, `Tomba` e `Brasília` preservam as tarifas atuais de teste.
