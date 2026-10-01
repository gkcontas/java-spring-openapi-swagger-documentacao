# Documentação de API com OpenAPI e Swagger

Catálogo de produtos publicado como três APIs que convivem no mesmo serviço — uma versão pública antiga, já depreciada, a versão pública atual e uma API administrativa protegida por token — cada uma com seu próprio documento OpenAPI.

O foco do projeto é a documentação como contrato: agrupar APIs distintas, descrever respostas polimórficas, refletir as regras de validação no schema, marcar o que está depreciado de um jeito que as ferramentas enxerguem, e garantir por teste que o documento gerado continua igual ao que foi revisado.

## Tecnologias e bibliotecas

| | |
|---|---|
| Linguagem | Java 21 (records, sealed interfaces) |
| Framework | Spring Boot 3.5 |
| Documentação | springdoc-openapi 2.9 (`springdoc-openapi-starter-webmvc-ui`), gerando OpenAPI 3.1 |
| Interface | Swagger UI |
| Segurança | Spring Security 6, com bearer token |
| Validação | Bean Validation |
| Build | Gradle Kotlin DSL (wrapper `gradlew`) |
| Testes | JUnit 5, AssertJ, MockMvc, Spring Security Test |

Os dados ficam em memória: o objeto de estudo é o contrato, e um banco acrescentaria migrations e containers sem mudar uma linha do documento gerado.

## Pré-requisitos

- JDK 21 ou superior

Não precisa de Docker nem de banco de dados.

## Como rodar

```bash
./gradlew bootRun
```

| | |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Documento da v1 | `http://localhost:8080/v3/api-docs/public-v1` |
| Documento da v2 | `http://localhost:8080/v3/api-docs/public-v2` |
| Documento da admin | `http://localhost:8080/v3/api-docs/admin` |

No Swagger UI, o seletor no topo alterna entre os três grupos. Para usar a API administrativa, clique em **Authorize** e informe o token `demo-admin-token`.

## Os três grupos

| Grupo | Rotas | Observação |
|---|---|---|
| `public-v1` | `/api/v1/**` | Depreciada: todas as operações vêm marcadas com `deprecated: true` e nota de migração |
| `public-v2` | `/api/v2/**` | Versão atual |
| `admin` | `/api/admin/**` | Exige bearer token |

Separar em grupos também é decisão de exposição: em um documento único, a API administrativa apareceria no mesmo portal que a pública.

Entre v1 e v2 o contrato mudou de forma: o preço deixou de ser um número solto e virou um objeto com valor e moeda, a categoria virou lista, e a resposta passou a distinguir produto físico de digital por um discriminador `type`.

## Endpoints

| Método | Rota | Grupo | Acesso |
|---|---|---|---|
| `GET` | `/api/v1/products` | public-v1 | público (depreciado) |
| `GET` | `/api/v1/products/{id}` | public-v1 | público (depreciado) |
| `GET` | `/api/v2/products` | public-v2 | público |
| `GET` | `/api/v2/products/{id}` | public-v2 | público |
| `POST` | `/api/admin/products` | admin | bearer |
| `DELETE` | `/api/admin/products/{id}` | admin | bearer |

## Exemplos de uso

Versão antiga, com preço simples e uma categoria:

```bash
curl -s localhost:8080/api/v1/products/1
```

Versão atual, com objeto de moeda, lista de categorias e discriminador:

```bash
curl -s localhost:8080/api/v2/products/1
```

```bash
curl -s localhost:8080/api/v2/products/3
```

```bash
curl -s "localhost:8080/api/v2/products?page=0&size=2&category=peripherals"
```

Os limites documentados são aplicados de verdade — `size` acima de 100 responde 400 em formato RFC 7807:

```bash
curl -s "localhost:8080/api/v2/products?size=500"
```

API administrativa, sem e com token:

```bash
curl -s -X POST localhost:8080/api/admin/products \
  -H "Content-Type: application/json" -d '{}'
```

```bash
curl -s -X POST localhost:8080/api/admin/products \
  -H "Authorization: Bearer demo-admin-token" \
  -H "Content-Type: application/json" \
  -d '{"name":"Webcam","sku":"WC-321","price":330.00,"kind":"PHYSICAL","weightKg":0.2,"categories":["peripherals"]}'
```

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE localhost:8080/api/admin/products/4 \
  -H "Authorization: Bearer demo-admin-token"
```

Baixar o documento OpenAPI de um grupo:

```bash
curl -s localhost:8080/v3/api-docs/public-v2
```

## Teste de contrato

O documento de cada grupo é comparado com um snapshot versionado em `src/test/resources/openapi/`. Quando algo no contrato muda — um campo renomeado, uma constraint mais rígida, uma propriedade nova obrigatória — o teste falha, escreve o documento atual em `build/openapi/` e indica o comando para aceitar a mudança:

```
cp build/openapi/public-v2.json src/test/resources/openapi/public-v2.json
```

Assim, alterar o contrato público vira um ato deliberado, com diff para revisar, em vez de um efeito colateral que o consumidor descobre depois.

## Testes

```bash
./gradlew test
```

34 testes, sem Docker e sem banco de dados:

| Classe | O que cobre |
|---|---|
| `ContractSnapshotTest` | Documento de cada grupo contra o snapshot versionado |
| `DocumentStructureTest` | Isolamento entre grupos, discriminador, media types, constraints, exemplos e security scheme |
| `PublicApiTest` | Diferenças entre v1 e v2, paginação, formatos de resposta |
| `AdminApiTest` | 401, 201, 204, 404 e validação do payload |
