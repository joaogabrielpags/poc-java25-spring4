<div align="center">

# ☕ Java 25 + Spring Boot 4 na prática

**Um cadastro simples. Uma stack inteira para explorar.**

[![build](https://github.com/joaogabrielpags/poc-java25-spring4/actions/workflows/build.yml/badge.svg)](https://github.com/joaogabrielpags/poc-java25-spring4/actions/workflows/build.yml)
![Java 25](https://img.shields.io/badge/Java-25_LTS-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 4.1](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)

[🚀 Rodar localmente](#-rode-localmente) · [🧭 Explorar o código](#-por-onde-começar-no-código) · [📚 Guia completo](docs/guia-de-exploracao.md)

</div>

---

Uma PoC que **cadastra usuários, enriquece endereços via ViaCEP e continua funcionando quando a consulta fica indisponível** — nesse caso, salva apenas o CEP e sinaliza `addressEnriched=false`.

Feita para **abrir na IDE, seguir uma requisição e experimentar**, não para ser um template de produção.

## ✨ O que vale explorar

| Na stack | Na prática |
|---|---|
| 🧵 **Java 25** | Virtual threads e domínio com records, separado do JPA |
| 🌐 **Spring Boot 4 / Framework 7** | Clientes HTTP declarativos com `@HttpExchange` |
| 🛡️ **Resiliência + observabilidade** | Circuit breaker, fallback, métricas e traces com OpenTelemetry |
| 🧩 **Design + qualidade** | Vertical Slice, JSpecify + NullAway e regras de arquitetura com ArchUnit |

## 🚀 Rode localmente

Você precisa de **JDK 25**. O Gradle Wrapper vem no repo; o banco é H2 em memória.

```bash
git clone https://github.com/joaogabrielpags/poc-java25-spring4.git
cd poc-java25-spring4
./gradlew bootRun
```

No Windows, use `.\gradlew.bat` no lugar de `./gradlew`.

Em outro terminal, faça o primeiro cadastro:

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Maria Silva","email":"maria.silva@example.com","cep":"70040-010"}'
```

O retorno é `201 Created`, com o `id` e o endereço. Use esse `id` em `GET /api/v1/users/{id}` para consultar o usuário. E-mail duplicado retorna `409`; CEP inexistente, `422`.

> [!NOTE]
> O H2 em memória descarta os dados ao encerrar a aplicação. A telemetria aponta para um collector local — veja como visualizar traces e métricas no [guia de observabilidade](docs/guia-de-exploracao.md#-observabilidade-local-opcional).

## 🧭 Por onde começar no código

Siga o cadastro do HTTP ao domínio e depois explore como ele lida com falhas:

| Comece aqui | O que observar |
|---|---|
| 📥 [RegisterUserController](src/main/java/com/pagbank/userregistration/user/register/RegisterUserController.java) | Entrada da API e contrato HTTP |
| ⚙️ [RegisterUserUseCase](src/main/java/com/pagbank/userregistration/user/register/RegisterUserUseCase.java) | Cadastro, enriquecimento e evento de domínio |
| 🛡️ [ViaCepAddressLookup](src/main/java/com/pagbank/userregistration/addresslookup/viacep/ViaCepAddressLookup.java) | Circuit breaker e degradação quando o ViaCEP falha |
| 🧱 [ArchitectureTest](src/test/java/com/pagbank/userregistration/ArchitectureTest.java) | Limites entre domínio, slices e infraestrutura |

> [!TIP]
> **Aprenda provocando uma falha.** Rode o [teste de integração do ViaCEP](src/test/java/com/pagbank/userregistration/addresslookup/viacep/ViaCepWireMockIntegrationTest.java) e acompanhe o circuito abrir. Como o cadastro se comporta sem uma nova chamada de rede?

```bash
./gradlew test --tests '*ViaCepWireMockIntegrationTest'
```

## 🧪 Rode os testes

```bash
./gradlew test    # Testes unitários, de integração e de arquitetura
./gradlew check   # Inclui o gate de cobertura JaCoCo
```

## 📚 Vá além

**[Guia de exploração](docs/guia-de-exploracao.md)** — API, trilha de estudo, arquitetura, observabilidade e desafios.

**[Testes de carga](perf/README.md)** — k6 + WireMock para comparar o serviço com ViaCEP saudável e circuito aberto.

---

💬 **Encontrou outra forma de fazer?** Abra uma issue ou PR — a PoC existe para essa troca.
