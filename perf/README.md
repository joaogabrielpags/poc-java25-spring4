# Teste de carga — `POST /api/v1/users` (T5.2 / RNF-04)

Cobre o critério de desempenho do PRD (seção 5, RNF-04):

- **p(99) < 800ms** com ViaCEP saudável.
- **p(99) < 300ms** com o circuito `viaCep` aberto (fallback sem chamada de rede).

Execução **manual** (não roda no CI): requer [k6](https://k6.io/docs/get-started/installation/)
e um WireMock local simulando o ViaCEP no lugar do endpoint real.

## 1. Subir o WireMock standalone

O jar já está no classpath de teste (`wiremockStandalone`, ver `gradle/libs.versions.toml`);
para uso avulso, baixe o "standalone jar" na página de releases do WireMock ou reaproveite
o já resolvido pelo Gradle em `~/.m2`/`~/.gradle/caches`.

**Cenário A — ViaCEP saudável** (`perf/wiremock/mappings/viacep-healthy.json`):

```bash
java -jar wiremock-standalone-<versão>.jar --port 9999 --root-dir perf/wiremock
```

**Cenário B — circuito forçado aberto** (`perf/wiremock/mappings/viacep-failing.json`):
copie apenas esse mapping para um diretório isolado (ou remova o `viacep-healthy.json`
antes de subir o WireMock), já que os dois stubs competem pela mesma `urlPattern`.

```bash
mkdir -p /tmp/wiremock-failing/mappings
cp perf/wiremock/mappings/viacep-failing.json /tmp/wiremock-failing/mappings/
java -jar wiremock-standalone-<versão>.jar --port 9999 --root-dir /tmp/wiremock-failing
```

## 2. Subir a aplicação apontando para o WireMock

```bash
SPRING_PROFILES_ACTIVE=local \
SPRING_HTTP_SERVICECLIENT_VIACEP_BASE-URL=http://localhost:9999 \
APP_HTTP_ALLOW-LOOPBACK=true \
./gradlew bootRun
```

`app.http.allow-loopback=true` é necessário porque o hardening SSRF (ADR-006) bloqueia
por padrão endereços de loopback/privados — o mesmo mecanismo usado pelos testes de
integração com WireMock (T3.6).

## 3. Rodar o k6

**Cenário A (ViaCEP saudável)**:

```bash
k6 run perf/k6/register-user.js
```

**Cenário B (circuito aberto)** — suba a aplicação apontando para o WireMock do cenário B
(stub 500), aqueça o circuito com ~10 requisições para garantir que ele já esteja aberto
(janela deslizante configurada em `resilience4j.circuitbreaker.instances.viaCep`), e então:

```bash
k6 run -e SCENARIO=circuit-open perf/k6/register-user.js
```

Parâmetros opcionais: `-e BASE_URL=http://host:porta`, `-e VUS=50`, `-e DURATION=60s`.

## Thresholds

Os thresholds (`p(99)<800`/`p(99)<300`) são embutidos no script conforme `SCENARIO` e
fazem o `k6` sair com código de erro se não forem atingidos — mas isso **não bloqueia
o CI** (script de execução manual, ver T5.2 nas Aceitações). Resultado deve ser
documentado manualmente (print/log) ao rodar em ambiente de staging.
