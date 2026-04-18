# Backend kkphoenix Website

Este projeto é um backend em Spring Boot projetado para servir um site estático (como um portfólio ou blog) cujo conteúdo é gerenciado em um repositório Git externo. Ele atua como um servidor de arquivos estáticos inteligente com capacidades de sincronização automática e uma API para listar o conteúdo.

## Como Rodar

### Configuração
As configurações principais estão em `src/main/resources/application.properties`. Você pode sobrescrevê-las com variáveis de ambiente:

*   `GIT_REPO_URL`: URL do repositório de conteúdo.
*   `GIT_REPO_BRANCH`: Branch a ser utilizada (padrão: `main`).
*   `GIT_USERNAME` / `GIT_TOKEN`: Credenciais (opcional).
*   `PORT`: Porta do servidor (padrão: `8081`).

### Executando

```bash
# Usando Maven
mvn spring-boot:run
```