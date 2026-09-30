# Documentação de API com OpenAPI e Swagger

Três APIs convivendo no mesmo serviço — **v1 pública (depreciada)**, **v2 pública** e **admin protegida** — cada uma com seu próprio documento OpenAPI, e um **teste de contrato** que compara o documento gerado com um snapshot versionado.

O assunto aqui não é "colocar o Swagger no projeto". É o que acontece depois: como o documento fica **verdadeiro**, como ele descreve polimorfismo, autenticação e versionamento, e o que quebra quando ninguém está olhando.

## Status

✅ Implementado, testado e validado manualmente ponta a ponta. 34 testes, build limpo.

## Stack

- Java 21 + Spring Boot 3.5
- springdoc-openapi 2.9.1 (`springdoc-openapi-starter-webmvc-ui`) — OpenAPI **3.1.0**
- Spring Security 6 (bearer estático)
- Bean Validation (Hibernate Validator)
- Gradle (Kotlin DSL) + wrapper `gradlew`
- JUnit 5 + MockMvc + AssertJ — **sem banco e sem container**: o objeto de estudo é o contrato, e um Postgres aqui só deixaria a suíte lenta sem mudar uma linha do documento gerado

## Os três grupos

```
GET /v3/api-docs/public-v1     2 paths, 4 schemas    Public API v1 (deprecated)
GET /v3/api-docs/public-v2     2 paths, 7 schemas    Public API v2
GET /v3/api-docs/admin         2 paths, 7 schemas    Admin API
GET /swagger-ui.html                                 dropdown com os três
```

Agrupar com `GroupedOpenApi` não é organização: é decisão de exposição. Sem grupos, **um** documento lista tudo que o serviço tem — e uma API administrativa aparecendo no portal público é um convite que ninguém quis mandar.

```java
GroupedOpenApi.builder()
        .group("admin")
        .displayName("Admin API")
        .pathsToMatch("/api/admin/**")
        .build();
```

O teste `shouldKeepTheAdminApiOutOfThePublicDocuments` trava isso: se alguém mudar o `pathsToMatch`, a suíte reclama antes do deploy.

## O documento mente com facilidade

Esta é a parte que interessa. Cinco casos em que o documento gerado estava **sintaticamente perfeito e errado** — todos encontrados rodando o serviço, não lendo o código.

### 1. O discriminator sem `mapping`

`ProductV2Response` é uma `sealed interface` com duas implementações. Com `@Schema(oneOf = ..., discriminatorProperty = "type")` o springdoc gera:

```json
"discriminator": { "propertyName": "type" }
```

Sem `mapping`, a especificação diz que **o valor do discriminator É o nome do schema**. O Jackson manda `"type": "PHYSICAL"`; o schema se chama `PhysicalProductV2`. Um cliente gerado a partir desse documento procura um schema chamado `PHYSICAL`, não acha, e falha ao desserializar uma resposta que o serviço considera perfeitamente válida.

A correção é declarar o mapeamento:

```java
discriminatorMapping = {
        @DiscriminatorMapping(value = "PHYSICAL", schema = PhysicalProductV2Response.class),
        @DiscriminatorMapping(value = "DIGITAL", schema = DigitalProductV2Response.class)
}
```

E o teste garante que as chaves do `mapping` são exatamente os nomes que o Jackson escreve — que é o acoplamento que ninguém verifica.

### 2. O wrapper genérico que apaga o discriminator

O caso mais desagradável do projeto, porque o documento está **certo** e a resposta é que está errada.

`PagedResponse<ProductV2Response>` gera o schema correto: `items: { $ref: ProductV2 }`, discriminator e tudo. Em tempo de execução, o campo `type` **some** — mas só no endpoint de lista. Buscar um produto por id continua mandando.

O motivo: o Spring passa o tipo genérico de retorno para o Jackson, mas `AbstractJackson2HttpMessageConverter` só chama `ObjectWriter.forType(...)` quando esse tipo é `isContainerType()` — verdadeiro para uma `List`, falso para um record que apenas contém uma. O Jackson então serializa o wrapper pela classe em runtime, onde `content` está declarado como `List<T>` com `T` apagado para `Object`, e o type serializer não é aplicado.

```
antes:  {"content":[{"id":1,"name":"Mechanical keyboard",...}]}
depois: {"content":[{"type":"PHYSICAL","id":1,"name":"Mechanical keyboard",...}]}
```

A correção é nomear o tipo do elemento (`ProductV2Page` com `List<ProductV2Response>`), o que elimina o apagamento. `PagedResponse<T>` continua em uso na v1, onde os itens são um record simples e o apagamento não custa nada.

### 3. `*/*` no lugar do media type real

Sem `produces` no mapping, o springdoc escreve `*/*`. O serviço manda `application/json` no sucesso e `application/problem+json` no erro — dois media types diferentes, nenhum dos dois documentado.

```java
@RequestMapping(value = "/api/v2/products", produces = MediaType.APPLICATION_JSON_VALUE)
...
@ApiResponse(responseCode = "404", content = @Content(
        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorSchemas.class)))
```

O teste compara os dois lados: o media type do documento e o `Content-Type` que o MockMvc recebe de verdade.

### 4. O 401 que na verdade era 403 vazio

As anotações prometem `401` com corpo `ProblemDetail`. Uma filter chain sem mecanismo de login cai no `Http403ForbiddenEntryPoint` — responde **403**, com corpo vazio. Duas divergências de uma vez, invisíveis até alguém exercitar o caminho de erro.

`ProblemDetailAuthenticationEntryPoint` devolve o que está documentado, com `WWW-Authenticate: Bearer`.

### 5. O validador que virou campo do request

Regra condicional: `weightKg` é obrigatório quando `kind` é `PHYSICAL`, `downloadUrl` quando é `DIGITAL`. O caminho natural é `@AssertTrue` num método auxiliar — e ele cobra caro duas vezes.

O Bean Validation exige que o método pareça um getter; o Jackson lê getters como propriedades. O schema ganha um booleano fantasma `deliveryDetailValid` que nenhum cliente pode mandar e todo gerador oferece como campo (a menos que também leve `@JsonIgnore` e `@Schema(hidden = true)`). E a violação fica pendurada nessa pseudo-propriedade, então a mensagem de erro sai assim:

```
deliveryDetailValid requires weightKg when kind is PHYSICAL and downloadUrl when kind is DIGITAL
```

Uma constraint de nível de tipo (`@ConsistentDeliveryDetail`) não tem nenhum dos dois problemas: sem accessor, sem propriedade no schema, e a violação é um erro global cuja mensagem se sustenta sozinha.

O que nenhuma das duas resolve é o **schema**. "Obrigatório quando outro campo tem certo valor" é expressável em JSON Schema com `if`/`then`, mas nenhuma anotação gera isso e a maioria dos geradores ignora. Então a regra é enunciada na descrição da operação, onde um humano lê, e aplicada no validador.

## O teste de contrato

Nada em uma aplicação Spring falha quando o contrato público muda. Renomear um campo, remover um valor de enum, apertar uma constraint, acrescentar uma propriedade obrigatória — compila, os testes passam, e os consumidores descobrem em produção.

`ContractSnapshotTest` compara o documento de cada grupo com um snapshot em `src/test/resources/openapi/`. Quando diverge, escreve o documento atual em `build/openapi/` e falha dizendo o comando:

```
The OpenAPI document for group 'public-v2' changed.

If the change is intentional, review it for backwards compatibility and accept it with:
    cp build/openapi/public-v2.json src/test/resources/openapi/public-v2.json
```

Não é um teste do springdoc. É um teste de **intenção**: o snapshot só muda quando alguém decide que o contrato deve mudar, e revisar esse diff é o momento de perguntar se a mudança é retrocompatível. Durante o desenvolvimento deste projeto ele pegou exatamente isso — a inclusão do campo `errors` no `ProblemDetail` apareceu como diff nos três grupos:

```diff
+          "errors" : {
+            "type" : "array",
+            "items" : { "$ref" : "#/components/schemas/ValidationError" }
+          },
```

Um detalhe que o faz funcionar: `springdoc.writer-with-order-by-keys: true`. Sem isso a ordem das propriedades segue a reflexão, que não é estável entre versões da JVM — e um spec instável transforma todo diff em ruído. Gerado duas vezes, byte a byte idêntico.

`DocumentStructureTest` complementa com 13 asserções sobre a **forma** do documento: o snapshot pega qualquer mudança, essas pegam os erros específicos que produzem um documento plausível e enganoso, e continuam valendo depois que o snapshot for legitimamente atualizado.

## Versionamento visível no contrato

A v1 continua documentada. Tirar a versão depreciada da documentação é o movimento errado — quem ainda chama v1 é exatamente quem precisa achar a nota de migração.

```java
@Operation(summary = "List products (deprecated)", deprecated = true, description = """
        **Migration:** `GET /api/v2/products` returns `price` as an object with `amount`
        and `currency`, and `categories` as a list. ...
        """)
```

`deprecated = true` é o que coloca o texto riscado no Swagger UI e o `"deprecated": true` no documento — que é o que um gerador de cliente lê para emitir um warning de compilação. Uma nota em prosa que a ferramenta não enxerga não chega a ninguém.

| | v1 | v2 |
|---|---|---|
| Preço | `"price": 450.00` | `"price": {"amount": 450.00, "currency": "BRL"}` |
| Categoria | `"category": "peripherals"` | `"categories": ["peripherals", "input"]` |
| Tipos | um só | `PHYSICAL` / `DIGITAL` com discriminator |
| Contrato | `deprecated: true` em toda operação | atual |

## Validação como documentação

As anotações do Bean Validation viram constraints do schema. A regra é declarada uma vez e aplicada em runtime, então o documento não consegue descrever um limite que o serviço não impõe — a falha clássica de quando os dois são escritos separadamente.

```java
@Pattern(regexp = "^[A-Z]{2}-\\d{3}$", message = "must look like AB-123")
String sku
```

```json
"sku": { "type": "string", "pattern": "^[A-Z]{2}-\\d{3}$", "minLength": 1 }
```

E todas as violações voltam de uma vez, não a primeira que o Hibernate Validator produziu. A ordem de validação não é definida: devolver uma mensagem arbitrária faz o cliente corrigir um campo, reenviar, e ser informado do próximo — e faz um teste que afirma uma mensagem específica passar ou falhar por sorte.

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "weightKg is required when kind is PHYSICAL, and downloadUrl when kind is DIGITAL; name must be between 2 and 120 characters; price must be greater than zero; sku must look like AB-123",
  "instance": "/api/admin/products",
  "errors": [
    { "field": null,    "message": "weightKg is required when kind is PHYSICAL, and downloadUrl when kind is DIGITAL" },
    { "field": "name",  "message": "must be between 2 and 120 characters" },
    { "field": "price", "message": "must be greater than zero" },
    { "field": "sku",   "message": "must look like AB-123" }
  ]
}
```

`errors` é um **extension member** do RFC 7807 — a especificação permite, e é a forma legível por máquina do que o `detail` diz em prosa. Um extension member não documentado é indistinguível de um bug, então ele está no schema.

## Segurança no documento x segurança de verdade

`@SecurityRequirement(name = "bearerAuth")` na classe marca toda operação como protegida e liga o cadeado e o botão **Authorize** do Swagger UI. O scheme é declarado uma vez em `OpenApiConfig`; a anotação só referencia pelo nome — e um nome que não casa com nenhum scheme declarado falha em silêncio: o cadeado simplesmente não aparece.

O ponto importante: a anotação **documenta** a exigência, ela não a aplica. Quem aplica é a filter chain. Nada liga as duas coisas, então um documento pode prometer cadeado sobre um endpoint escancarado. `AdminApiTest` é essa ligação — exercita sem token, com token errado e com token válido.

```
POST /api/admin/products  (sem token)        401 application/problem+json
POST /api/admin/products  (token invalido)   401
POST /api/admin/products  (valido)           201
GET  /v3/api-docs/admin                      200   <- documentação aberta, API fechada
```

> Os endpoints de documentação estão públicos aqui porque isto é uma demonstração. Numa API interna real eles não estariam: um spec é um mapa completo da superfície de ataque, e publicá-lo é uma decisão, não um default.

## Um exemplo não basta num payload polimórfico

`@ExampleObject` nomeado, dois deles, na operação de buscar um produto:

```java
examples = {
    @ExampleObject(name = "physical", summary = "A product that ships",  value = """..."""),
    @ExampleObject(name = "digital",  summary = "A product delivered by download", value = """...""")
}
```

Quem só vê a forma física escreve código que quebra no primeiro produto digital que aparecer. No Swagger UI os dois viram um dropdown na resposta.

## `@ParameterObject`

Paginação e filtro como um objeto, não como quatro `@RequestParam` soltos. `@ParameterObject` achata o record em parâmetros individuais no documento, então o contrato continua mostrando `?page=&size=&category=` — o agrupamento é do lado Java, e a descrição de cada parâmetro fica ao lado do campo que ela descreve.

Armadilha relacionada, não demonstrada aqui porque o projeto não usa Spring Data: passar o `Pageable` do Spring direto gera um schema expondo a classe interna, com campos que nenhum cliente consegue mandar. A correção é a mesma anotação (`@ParameterObject Pageable`) mais o converter de pageable do springdoc.

## Como rodar

```bash
./gradlew bootRun
```

```
http://localhost:8080/swagger-ui.html      interface, com o dropdown dos três grupos
http://localhost:8080/v3/api-docs/public-v2
```

No Swagger UI: **Authorize** → cole `demo-admin-token` → os endpoints de admin passam a responder 201/204 em vez de 401.

## Como rodar os testes

```bash
./gradlew test
```

34 testes, suíte em ~1,8 s — um único contexto Spring em cache, sem banco e sem container.

| Classe | Testes | O que cobre |
|---|---|---|
| `ContractSnapshotTest` | 3 | documento de cada grupo x snapshot versionado |
| `DocumentStructureTest` | 13 | isolamento entre grupos, discriminator, media types, constraints, exemplos, security scheme |
| `PublicApiTest` | 9 | v1 x v2, discriminator dentro da lista, defaults, media types reais |
| `AdminApiTest` | 9 | 401/201/204/404, validação, todas as violações de uma vez |

Para exportar o spec sem rodar a aplicação: `./gradlew test` escreve os três documentos em `build/openapi/`.

## Endpoints

| Método | Rota | Grupo | Proteção |
|---|---|---|---|
| `GET` | `/api/v1/products` | public-v1 | — (depreciado) |
| `GET` | `/api/v1/products/{id}` | public-v1 | — (depreciado) |
| `GET` | `/api/v2/products` | public-v2 | — |
| `GET` | `/api/v2/products/{id}` | public-v2 | — |
| `POST` | `/api/admin/products` | admin | bearer |
| `DELETE` | `/api/admin/products/{id}` | admin | bearer |

## Exemplo de uso

```bash
# v1: preco plano, uma categoria
curl -s localhost:8080/api/v1/products/1
# {"id":1,"name":"Mechanical keyboard","price":450.00,"category":"peripherals"}

# v2: objeto de dinheiro, lista de categorias, discriminator
curl -s localhost:8080/api/v2/products/1
curl -s localhost:8080/api/v2/products/3   # type=DIGITAL, com downloadUrl

# o discriminator sobrevive dentro da lista
curl -s 'localhost:8080/api/v2/products?size=2' | grep -o '"type":"[A-Z]*"'

# limite documentado, aplicado
curl -s 'localhost:8080/api/v2/products?size=500'   # 400 application/problem+json

# admin sem token
curl -s -X POST localhost:8080/api/admin/products -H 'Content-Type: application/json' -d '{}'
# 401 {"title":"Unauthorized",...}

# admin com token
curl -s -X POST localhost:8080/api/admin/products \
  -H 'Authorization: Bearer demo-admin-token' -H 'Content-Type: application/json' \
  -d '{"name":"Webcam","sku":"WC-321","price":330.00,"kind":"PHYSICAL","weightKg":0.2}'
```

## Decisões de escopo

- **Sem banco.** O `ProductStore` é um `ConcurrentHashMap`. Um Postgres traria migrations, container e uma suíte mais lenta sem mudar uma linha do documento OpenAPI. Armazenamento é a única parte aqui que é genuinamente detalhe de implementação.
- **Bearer estático.** Emitir e validar JWT de verdade é o assunto de [dois](../java-spring-jwt-api-auth) [outros](../java-spring-jwt-claims-customizados) projetos deste portfólio. Refazer isso aqui somaria cem linhas que não têm nada a ver com a pergunta que este projeto responde, que é como uma API protegida é **descrita**.
- **Sem Lombok.** Todo tipo aqui é record ou classe de configuração; um annotation processor sem uso é build mais lento sem contrapartida.

## O que levar de lição

O documento gerado automaticamente é um **rascunho**, não um contrato. Ele nasce sintaticamente válido e semanticamente otimista: o discriminator sem mapeamento, o `*/*`, o 401 que é 403, o campo que some na serialização — todos passam em qualquer linter de OpenAPI.

O que transforma o rascunho em contrato é exercitar os dois lados ao mesmo tempo: o documento **e** a resposta real, no mesmo teste. E congelar o resultado num snapshot, para que mudar o contrato seja um ato deliberado com diff para revisar.
