# JDK 25 (Microsoft Build of OpenJDK) + usuário "vscode". O Gradle vem pelo wrapper do repo.
FROM mcr.microsoft.com/devcontainers/java:25-bookworm

# k6 para os testes de carga em perf/ (ver perf/README.md).
ARG K6_VERSION=2.3.0
RUN ARCH="$(dpkg --print-architecture)" \
    && curl -fsSL "https://github.com/grafana/k6/releases/download/v${K6_VERSION}/k6-v${K6_VERSION}-linux-${ARCH}.tar.gz" \
       | tar -xz -C /tmp \
    && mv "/tmp/k6-v${K6_VERSION}-linux-${ARCH}/k6" /usr/local/bin/k6 \
    && rm -rf "/tmp/k6-v${K6_VERSION}-linux-${ARCH}"

# Pré-cria o diretório do cache do Gradle para que o volume nomeado herde o dono "vscode".
RUN mkdir -p /home/vscode/.gradle && chown -R vscode:vscode /home/vscode/.gradle
