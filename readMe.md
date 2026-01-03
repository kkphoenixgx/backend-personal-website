# Backend kkphoenix Website

Este projeto é um backend em Spring Boot projetado para servir um site estático (como um portfólio ou blog) cujo conteúdo é gerenciado em um repositório Git externo. Ele atua como um servidor de arquivos estáticos inteligente com capacidades de sincronização automática e uma API para listar o conteúdo.

## Arquitetura

O projeto segue princípios de **Arquitetura Hexagonal (Ports and Adapters)** para desacoplar a lógica de negócio da infraestrutura.

*   **Domain (`com.kkphoenixgx.domain`)**: Contém as regras de negócio e modelos.
    *   **Model**: `Pages` representa a estrutura de uma página ou diretório.
    *   **Ports**: Interfaces que definem as entradas (`PagesServicePort`) e saídas (`PagesRepositoryPort`) do domínio.
    *   **Service**: `PagesService` implementa a lógica de negócio.
*   **Infrastructure (`com.kkphoenixgx.infrastructure`)**: Implementações concretas e adaptadores.
    *   **Persistence**:
        *   **IO**: `IOPersistence` implementa `PagesRepositoryPort` para ler a estrutura de arquivos do disco.
        *   **Git**: `GitPersistence` gerencia a clonagem e atualização do repositório de conteúdo usando JGit.
    *   **Web**: Controladores REST e MVC.
        *   `PagesController`: API que expõe a estrutura do site.
        *   `PageController`: Gerencia o roteamento de URLs limpas (sem `.html`) para os arquivos estáticos.
        *   `WebConfig`: Configura o Spring para servir recursos estáticos do diretório clonado.

## Funcionalidades

1.  **Sincronização com Git**:
    *   Ao iniciar, o sistema verifica se o conteúdo estático existe localmente.
    *   Se não existir, clona o repositório configurado (`git.repo.url`).
    *   Possui uma tarefa agendada (`@Scheduled`) que executa um `git pull` periodicamente (padrão: a cada 24h) para manter o conteúdo atualizado.
    *   Suporta autenticação via Username/Token.

2.  **Servidor de Conteúdo Estático**:
    *   Serve arquivos HTML, CSS, JS e imagens diretamente do diretório sincronizado.
    *   **Clean URLs**: O `PageController` intercepta requisições e encaminha URLs amigáveis (ex: `/blog/meu-post`) para o arquivo físico correspondente (ex: `/blog/meu-post.html` ou `/blog/meu-post/index.html`).

3.  **API de Estrutura**:
    *   Endpoint `GET /api/pages/`: Retorna um JSON representando a árvore de diretórios e arquivos do site, útil para gerar menus ou mapas do site dinamicamente no frontend.

## Como Funciona

1.  **Inicialização**: O Spring Boot sobe e o bean `GitPersistence` é instanciado. Após 10 segundos, ele tenta clonar ou atualizar o repositório Git no caminho definido em `app.static.pages`.
2.  **Requisição Web**:
    *   Se o usuário acessa uma URL, o `PageController` verifica se é um diretório ou arquivo e faz o *forward* interno para o recurso estático correto.
    *   O `WebConfig` mapeia as requisições para o sistema de arquivos local onde o repo foi clonado.
3.  **Requisição API**:
    *   Ao chamar `/api/pages/`, o `IOPersistence` varre o diretório de conteúdo, ignorando arquivos de sistema (como `.git`), e constrói uma lista hierárquica de objetos `Pages`.

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

## Checklist

* [X] – Servir os paths corretamentes
* [X] – White lable fix
* [X] - Home melhor | A home vai ser a do site mesmo
