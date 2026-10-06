<div align="center">

# ☕ poc-java25-spring4

### *User Registration Service* — uma PoC para aprender **Java 25 + Spring Boot 4** com código real

[![build](https://github.com/joaogabrielpags/poc-java25-spring4/actions/workflows/build.yml/badge.svg)](https://github.com/joaogabrielpags/poc-java25-spring4/actions/workflows/build.yml)
![Java 25](https://img.shields.io/badge/Java-25_LTS-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 4.1](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Gradle 9](https://img.shields.io/badge/Gradle-9-02303A?logo=gradle&logoColor=white)
![Tests](https://img.shields.io/badge/tests-60%2B-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-%E2%89%A5%2080%25-blue)

**Cadastra usuários · Enriquece endereço via ViaCEP · Não cai quando o ViaCEP cai**

[🚀 Quickstart](#-quickstart-5-minutos) ·
[📡 API](#-operações-disponíveis) ·
[🎓 Trilha de estudo](#-trilha-de-estudo-o-que-você-vai-aprender) ·
[🧭 Mapa do código](#-mapa-de-pacotes) ·
[🧪 Testes](#-rodando-a-bateria-completa) ·
[💡 Desafios](#-desafios-para-continuar-explorando)

</div>

---

## 🤔 Por que este repositório existe?

Java 25 (LTS) e Spring Boot 4 / Spring Framework 7 são recentes — e mudaram coisas de forma
**silenciosa, mas importante**:

| Antes | Agora |
|---|---|
| `spring-boot-starter-web` monolítico | *Starters* fatiados: `starter-webmvc`, `restclient`, `http-client`… |
| `RestTemplate` / Feign | **HTTP Service Clients declarativos** (`@HttpExchange`) |
| `@NonNull` da sorte | Nulidade como **contrato de API** via JSpecify + NullAway no build |
| Agente Java para tracing | **OpenTelemetry nativo** (`starter-opentelemetry`) |
| Tuning de thread pool | **Virtual threads** com uma linha de YAML |

Em vez de ler release notes, aqui você **lê código que compila, passa em 60+ testes e sobe em
segundos** — e cada decisão tem um comentário explicando *por quê*.

> 💬 Este repo é para você abrir no IDE, **quebrar, consertar e aprender**.

---

## 🧩 O que o serviço faz

```
Cliente ──POST /api/v1/users──▶ [RegisterUserController] ──▶ [RegisterUserUseCase]
                                                                     │
                                            ┌────────────────────────┼─────────────────────┐
                                            ▼                        ▼                     ▼
                                   [CepClient @HttpExchange     [UserRepository]   [Observation
                                    + @CircuitBreaker]            (JPA / H2)        "user.registration"]
                                            │
                                            ▼
                                     ViaCEP (externo)      ──OTLP──▶ Collector ──▶ Jaeger + Prometheus
```

1. 📥 Recebe **nome, e-mail e CEP**.
2. 🚫 Rejeita e-mail duplicado (`409`).
3. 🌐 Consulta o ViaCEP com um client HTTP **declarativo** — uma interface, zero implementação.
4. 🛡️ Se o ViaCEP cair, estourar o timeout ou o circuito abrir, **degrada graciosamente**:
   salva só com o CEP e marca `addressEnriched=false`. *Nunca* falha o cadastro por culpa de terceiro.
5. 💾 Persiste, publica um evento de domínio, emite métricas e um *span* customizado.

### 📡 Operações disponíveis

| | Rota | O que faz | Respostas |
|---|---|---|---|
| 🟢 `POST` | `/api/v1/users` | Cadastra usuário com enriquecimento de endereço | `201` + `Location` · `400` validação · `409` e-mail duplicado · `422` CEP inexistente |
| 🔵 `GET` | `/api/v1/users/{id}` | Consulta usuário por UUID | `200` · `404` |
| ❤️ `GET` | `/actuator/health` | Liveness / readiness probes | `200` |
| 📊 `GET` | `/actuator/metrics` | Métricas Micrometer (ex.: `user.registration.total`) | `200` |
| ⚡ `GET` | `/actuator/circuitbreakers` | Estado do circuito `viaCep` (`CLOSED` / `OPEN` / `HALF_OPEN`) | `200` |
| 📜 `GET` | `/actuator/circuitbreakerevents` | Histórico de transições do circuito | `200` |

> 📐 Todos os erros seguem **RFC 9457 (Problem Details)** — veja `shared/web/GlobalExceptionHandler.java`.

---

## 🚀 Quickstart (5 minutos)

> **Pré-requisito:** JDK 25. O Gradle Wrapper já vem no repo.

```bash
git clone https://github.com/joaogabrielpags/poc-java25-spring4.git
cd poc-java25-spring4
./gradlew bootRun
```

Em outro terminal:

```bash
curl -s -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Maria Silva","email":"maria.silva@example.com","cep":"70040-010"}' | jq
```

<details>
<summary>📦 Resposta esperada (<code>201 Created</code>)</summary>

```json
{
  "id": "6f1c…",
  "name": "Maria Silva",
  "email": "maria.silva@example.com",
  "address": {
    "cep": "70040-010",
    "street": "Setor Bancário Sul Quadra 2 Bloco Q",
    "neighborhood": "Asa Sul",
    "city": "Brasília",
    "state": "DF"
  },
  "addressEnriched": true,
  "createdAt": "2026-10-06T03:41:00Z"
}
```
</details>

```bash
# 🔍 Consulta
curl -s http://localhost:8080/api/v1/users/<id> | jq

# ❌ Erros em formato Problem Details
curl -s -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' -d '{"name":"X","email":"nope","cep":"1"}' | jq
```

### ⚡ Veja o circuit breaker abrir

Desligue a rede (ou aponte `spring.http.serviceclient.viacep.base-url` para uma porta morta)
e dispare ~10 cadastros. Depois:

```bash
curl -s http://localhost:8080/actuator/circuitbreakers | jq                      # 👉 state: "OPEN"
curl -s http://localhost:8080/actuator/metrics/user.registration.fallback | jq  # 👉 contador subindo
```

Os cadastros **continuam retornando `201`** — agora com `"addressEnriched": false` e só o
`cep` dentro de `address`. Isso é degradação graciosa na prática. 🎯

### 🔭 Observabilidade local (opcional)

```bash
docker compose up -d                                        # OTel Collector + Jaeger + Prometheus
./gradlew bootRun --args='--spring.profiles.active=local'   # logs ECS/JSON + H2 console
```

| Ferramenta | URL | O que procurar |
|---|---|---|
| 🔎 Jaeger | <http://localhost:16686> | span `register-user` com tags `enriched` / `outcome` |
| 📈 Prometheus | <http://localhost:9090> | `user_registration_total{enriched="false"}` |
| 🗄️ H2 Console | <http://localhost:8080/h2-console> | tabela de usuários |

---

## 🎓 Trilha de estudo: o que você vai aprender

Cada linha aponta para um arquivo. **Abra, leia o Javadoc, rode o teste.** As seções estão
em ordem sugerida de leitura.

<details open>
<summary><h3>🧵 1. Java 25 & virtual threads</h3></summary>

| Onde | O que ver |
|---|---|
| `build.gradle` → `toolchain` | JDK 25 via Gradle Toolchain — o build **falha** se o JDK não existir, em vez de compilar com a versão errada. |
| `application.yaml` → `spring.threads.virtual.enabled` | Virtual threads ligadas por padrão, sem tuning de pool do Tomcat. |
| `user/domain/*.java` | Domínio 100 % em **records** com construtores compactos validando invariantes (`Cep`, `Email`, `User`). Nenhum `@Entity` — e o `ArchitectureTest` garante. |

> 🧪 **Experimente:** logue `Thread.currentThread().isVirtual()` dentro de um controller e dispare uma request.
</details>

<details open>
<summary><h3>🌐 2. HTTP Service Clients declarativos (Spring 7 / Boot 4)</h3></summary>

| Onde | O que ver |
|---|---|
| `addresslookup/viacep/CepClient.java` | A interface **inteira** do cliente ViaCEP: `@HttpExchange` + `@GetExchange`. Sem `RestTemplate`, sem Feign, sem implementação. |
| `addresslookup/viacep/HttpClientsConfig.java` | `@ImportHttpServices(group = "viacep")` registra o client; `spring.http.serviceclient.viacep.*` configura base-url e timeouts por YAML. |
| ↳ mesmo arquivo | 🔒 **Hardening anti-SSRF** com `InetAddressFilter.externalAddresses()` — API nova do Boot 4 que impede o client de falar com `localhost` / redes privadas. Veja como o perfil `test` relaxa isso para o WireMock. |

> 🧪 **Experimente:** rode `./gradlew bootRun` com `--spring.http.serviceclient.viacep.base-url=http://127.0.0.1:9` e veja o SSRF filter bloquear antes mesmo de conectar.
</details>

<details open>
<summary><h3>🚫 3. Null safety como contrato — JSpecify + NullAway</h3></summary>

| Onde | O que ver |
|---|---|
| `**/package-info.java` | Todo pacote é `@NullMarked`: **não-nulo é o default**, `@Nullable` é a exceção explícita. |
| `build.gradle` → `errorprone { error('NullAway') }` | 💥 **O build quebra** se você desreferenciar um `@Nullable` sem checar. |
| `ArchitectureTest.everyPackageWithClassesHasNullMarkedPackageInfo` | Teste que impede alguém de criar um pacote sem `package-info.java`. |

> 🧪 **Experimente:** remova o `null` check em `GlobalExceptionHandler.toFieldErrorMap` e rode `./gradlew compileJava`. O NullAway vai te dizer exatamente onde o `null` vazaria.
</details>

<details open>
<summary><h3>🔭 4. Observabilidade OpenTelemetry nativa</h3></summary>

| Onde | O que ver |
|---|---|
| `build.gradle` → `spring-boot-starter-opentelemetry` | Starter novo do Boot 4: traces e métricas via OTLP **sem agente Java**. |
| `user/register/RegisterUserUseCase.java` | `Observation` manual (span + timer) com tags de **baixa cardinalidade** (`enriched`, `outcome`) — e o comentário explicando por que *não* usar `@Observed` aqui. |
| `user/register/UserRegistrationMetricsListener.java` | Métrica de negócio alimentada por **evento de domínio** (`UserRegistered`), desacoplando o use case do `MeterRegistry`. |
| `shared/config/StructuredLoggingCorrelationTest.java` | Logs ECS/JSON correlacionados a `trace_id` / `span_id` — sem MDC manual. |

> 🧪 **Experimente:** suba o `docker compose`, faça um cadastro e encontre o span `register-user` no Jaeger. Depois force um fallback e compare as tags.
</details>

<details open>
<summary><h3>⚡ 5. Resiliência — Resilience4j no Boot 4</h3></summary>

| Onde | O que ver |
|---|---|
| `addresslookup/viacep/ViaCepAddressLookup.java` | `@CircuitBreaker(name = "viaCep", fallbackMethod = …)` e o truque das **duas sobrecargas de fallback**: `CepNotFoundException` (erro de negócio → `422`) não derruba o circuito nem aciona a degradação. |
| `application.yaml` → `resilience4j.circuitbreaker` | `ignore-exceptions` vs `record-exceptions`, janela deslizante, half-open automático. |
| `ViaCepWireMockIntegrationTest` | **4 cenários**: sucesso · 10 falhas → `OPEN` → fallback sem rede · `erro:true` → `422` · read timeout. WireMock em porta dinâmica com `@DynamicPropertySource`. |

> 🧪 **Experimente:** mude `minimum-number-of-calls` para `20` e veja qual cenário do `ViaCepWireMockIntegrationTest` quebra — e por quê.
</details>

<details open>
<summary><h3>🏛️ 6. Arquitetura DDD tático + Vertical Slice</h3></summary>

| Onde | O que ver |
|---|---|
| `user/register/`, `user/find/` | Cada *slice* carrega controller, use case e DTOs **juntos**. Nada de `controller/`, `service/`, `repository/` na raiz — `ArchitectureTest.noLegacyLayeredPackagesAtRoot` proíbe. |
| `addresslookup/AddressLookup.java` | Port de domínio; `viacep/` é o adapter. O slice `user` **não enxerga** `viacep` (regra ArchUnit). |
| `shared/persistence/` | Único lugar com JPA. `UserJpaEntity` ↔ `User` via mapper explícito. |
| `shared/web/GlobalExceptionHandler.java` | `ProblemDetail` nativo do Spring, sem biblioteca extra. |

> 🧪 **Experimente:** importe `ViaCepResponse` dentro de `RegisterUserUseCase` e rode `./gradlew test --tests ArchitectureTest`.
</details>

<details open>
<summary><h3>🧪 7. Testes com os novos módulos de teste do Boot 4</h3></summary>

| Onde | O que ver |
|---|---|
| `build.gradle` → dependências de teste | `spring-boot-webmvc-test`, `spring-boot-data-jpa-test`, `spring-boot-resttestclient` — no Boot 4 os *slices* de teste são **módulos separados**, não vêm todos no `starter-test`. |
| `RegisterUserControllerTest` / `FindUserControllerTest` | `@WebMvcTest` focado só na camada web. |
| `SecurityHardeningIntegrationTest` | Container real: header `Server` suprimido, cabeçalho > 8 KB rejeitado. |
| `ProdProfileHardeningTest` | Garante que o perfil `prod` tem actuator restrito e sampling de 10 %. |
</details>

---

## 🧭 Mapa de pacotes

```
com.pagbank.userregistration
├── 👤 user
│   ├── domain          # User, Email, Address, UserId, UserRegistered — records puros, sem framework
│   ├── register        # POST /api/v1/users — controller, use case, DTOs, listener de métricas
│   └── find            # GET  /api/v1/users/{id}
├── 📍 addresslookup    # port AddressLookup + VO Cep + CepNotFoundException
│   └── viacep          # adapter: CepClient (@HttpExchange), circuit breaker, SSRF hardening
└── 🔧 shared
    ├── config          # Clock bean (testabilidade de tempo)
    ├── persistence     # UserJpaEntity, mapper, JpaUserRepository (único lugar com JPA)
    └── web             # GlobalExceptionHandler (RFC 9457)
```

---

## 🧪 Rodando a bateria completa

```bash
./gradlew test                 # ✅ unitários + integração (ArchUnit, WireMock, @SpringBootTest)
./gradlew check                # 🛡️ test + NullAway + cobertura JaCoCo ≥ 80 % em user.. e addresslookup..
./gradlew jacocoTestReport     # 📊 build/reports/jacoco/test/html/index.html
```

O mesmo `./gradlew build` roda no GitHub Actions (`.github/workflows/build.yml`) com Temurin 25.

### 🎛️ Perfis

| Perfil | Para quê | Diferenças |
|---|---|---|
| *(default)* | Dev rápido | H2 em memória, OTLP → `localhost:4318`, sampling 100 % |
| `local` | Dev com stack de observabilidade | + H2 console, logs ECS/JSON |
| `prod` | Referência de hardening | actuator só `health,info`, sampling 10 %, `show-details: never` |
| `test` | Usado pelos testes | OTLP desligado, `app.http.allow-loopback=true` para WireMock |

### 🏋️ Teste de carga (manual)

`perf/` contém um script **k6** e stubs WireMock para medir p(99) com ViaCEP saudável
(< 800 ms) e com circuito aberto (< 300 ms). Instruções em [`perf/README.md`](perf/README.md).

---

<details>
<summary><h2>⚙️ Propriedades-chave</h2></summary>

| Propriedade | Descrição | Default |
|---|---|---|
| `spring.threads.virtual.enabled` | Virtual threads no Tomcat e executors do Spring | `true` |
| `spring.http.serviceclient.viacep.base-url` | URL base do ViaCEP | `https://viacep.com.br` |
| `spring.http.serviceclient.viacep.connect-timeout` / `.read-timeout` | Timeouts do client declarativo | `2s` / `3s` |
| `resilience4j.circuitbreaker.instances.viaCep.*` | Janela, limiar de falha, half-open | janela 10, limiar 50 %, 5 s em OPEN |
| `management.opentelemetry.tracing.export.otlp.endpoint` | Destino OTLP de traces | `http://localhost:4318/v1/traces` |
| `management.otlp.metrics.export.url` | Destino OTLP de métricas | `http://localhost:4318/v1/metrics` |
| `management.tracing.sampling.probability` | Amostragem de traces | `1.0` (`0.1` em `prod`) |
| `server.max-http-request-header-size` | Limite de cabeçalhos HTTP | `8KB` |
| `app.http.allow-loopback` | Desliga o bloqueio SSRF para `localhost` (apenas `test`) | `false` |
</details>

---

## 💡 Desafios para continuar explorando

Forke e tente — cada mudança ensina algo diferente sobre a stack:

- [ ] 🐘 Trocar o H2 por **PostgreSQL** com Testcontainers e `@ServiceConnection`.
- [ ] 🔁 Adicionar um segundo HTTP Service Client (ex.: BrasilAPI) e fazer *fallback em cascata* antes de degradar.
- [ ] 🧬 Introduzir um `sealed interface AddressLookupResult` com **pattern matching** em `switch` no lugar do `Optional<Address>`.
- [ ] ⏱️ Substituir `@CircuitBreaker` por `@Retry` + `@TimeLimiter` e comparar as métricas no Prometheus.
- [ ] 🗜️ Rodar com `-XX:+UseCompactObjectHeaders` (JEP 519, Java 25) e medir o heap.
- [ ] 🧱 Quebrar uma regra do `ArchitectureTest` de propósito e ler a mensagem de erro.
- [ ] 🕳️ Remover um `@Nullable` e ver o NullAway explicar exatamente onde o `null` vazaria.

---

<details>
<summary><h2>✅ Checklist de nulidade para PRs</h2></summary>

- [ ] Todo pacote novo em `src/main` tem `package-info.java` com `@NullMarked`.
- [ ] Ausência é modelada com `@Nullable` em parâmetros/retornos (ou `Optional` quando a
      ausência é resultado de negócio em port/use case) — nunca `Optional` em campos.
- [ ] `./gradlew compileJava` passa sem violações de NullAway (`ERROR` em `src/main`,
      `WARN` em `src/test`).
- [ ] Nenhum `@SuppressWarnings("NullAway")` novo sem justificativa em comentário.
</details>

---

<div align="center">

Encontrou algo que poderia ser mais idiomático em Java 25 ou Spring Boot 4?
**Abra uma issue ou PR** — a PoC existe exatamente para essa conversa. 🙌

</div>
