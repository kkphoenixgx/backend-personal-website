 # Arquitetura do Servidor do Site Pessoal
 
 O projeto é um backend em Spring Boot que atua como um servidor de conteúdo estático inteligente. Ele foi projetado para servir um site cujo conteúdo (HTML, CSS, imagens) é mantido em um repositório Git externo. A arquitetura é desacoplada, facilitando a manutenção e a troca de implementações.
 
 ## Diagrama da Arquitetura
 
 O diagrama abaixo ilustra os principais componentes e fluxos de dados, seguindo os princípios da **Arquitetura Hexagonal (Ports and Adapters)**.
 
 ```mermaid
 graph TD
     subgraph "Usuário e Fontes Externas"
         User[👤 Usuário]
         GitRepo[🌐 Repositório Git Externo]
     end
 
     subgraph "Spring Boot Application"
         subgraph "Adapters (Infraestrutura)"
             WebLayer[Camada Web]
             PersistenceLayer[Camada de Persistência]
         end
 
         subgraph "Application Core (Domínio)"
             Domain[Lógica de Negócio]
         end
 
         WebLayer -- Chama --> Domain
         Domain -- Usa a porta --> PersistenceLayer
     end
 
     User -- Requisições HTTP --> WebLayer
     PersistenceLayer -- Sincroniza com --> GitRepo
 
     style Domain fill:#D5E8D4,stroke:#82B366
     style WebLayer fill:#DAE8FC,stroke:#6C8EBF
     style PersistenceLayer fill:#F8CECC,stroke:#B85450
 ```

O projeto segue princípios de **Arquitetura Hexagonal (Ports and Adapters)** para desacoplar a lógica de negócio da infraestrutura.

*   **Domain (`com.kkphoenixgx.domain`)**: Contém as regras de negócio e modelos.
    *   **Model**: `Pages` representa a estrutura de uma página ou diretório.
    *   **Ports (Portas)**: Interfaces que definem os contratos de comunicação.
        *   `in/PagesServicePort`: A porta de entrada para a lógica de negócio (o que a aplicação *pode fazer*).
        *   `out/PagesRepositoryPort`: A porta de saída para a persistência (o que a aplicação *precisa* para funcionar).
    *   **Service**: `PagesService` implementa a lógica de negócio principal. Atualmente, possui uma regra de filtragem estática que exibe apenas as páginas/diretórios raiz especificados ("Programing", "RPG", "Study").
*   **Infrastructure (`com.kkphoenixgx.infrastructure`)**: Implementações concretas e adaptadores.
    *   **Persistence**:
        *   `IOPersistence`: Adaptador que implementa `PagesRepositoryPort`. Sua função é escanear o sistema de arquivos local de forma recursiva, construindo a árvore de páginas. Ele ignora a pasta `.git` e possui lógica específica para extrair o título das páginas (ex: nomeando baseado no diretório pai caso o arquivo seja um `index.html`).
        *   `git/GitPersistence`: Adaptador responsável por manter o conteúdo local sincronizado com o repositório Git remoto (usa a biblioteca JGit).
        *   `git/LoggingProgressMonitor`: Utilitário acoplado ao `GitPersistence` que intercepta os logs de progresso do JGit, enviando o status da clonagem para o console do Spring Boot de maneira legível.
    *   **Web**: Controladores REST e MVC.
        *   `PagesController`: API REST que expõe a estrutura do site para o frontend.
        *   `PageController`: Adaptador que gerencia o roteamento de "URLs Limpas" (sem `.html`).
        *   `CustomErrorController`: Adaptador que trata erros HTTP (como 404). Identifica a URI de origem e direciona inteligentemente para páginas de erro customizadas do próprio conteúdo estático (ex: se o erro ocorreu dentro de `/RPG/`, encaminha para `/RPG/404.html`).
        *   `WebConfig`: Configura o Spring MVC para servir os recursos estáticos. Também gerencia a política de **CORS**, liberando o acesso global se o modo de teste estiver ativo, ou restringindo aos domínios de produção (`kkphoenix.com.br`) caso contrário.
*   **Application (`com.kkphoenixgx.Application`)**: Camada de configuração e inicialização do Spring Boot.
    *   `App`: Classe principal que habilita o agendamento (`@EnableScheduling`) e a execução assíncrona (`@EnableAsync`).
    *   `BeanConfiguration`: Realiza a injeção de dependência, conectando os adaptadores às portas do domínio.

## Mecanismos e Fluxos Principais

### 1. Sincronização com Repositório Git
Esta é a funcionalidade central que alimenta o servidor estático com conteúdo gerenciado via Git.
1.  **Início da Aplicação**: O `GitPersistence` é inicializado.
2.  **Tarefa Agendada**: Uma tarefa (`@Scheduled` e `@Async`) é executada 10 segundos após o início e, depois, a cada 24 horas.
3.  **Clonagem ou Atualização**:
    *   **Se o diretório `.git` não existe (ou está vazio)**: O repositório (`git.repo.url`) é clonado para o local definido em `app.static.pages` usando a branch configurada.
    *   **Se o diretório `.git` já existe**: Para evitar conflitos de merge, a estratégia adotada é um "force pull" seguro. O sistema executa um `git fetch` seguido de um `git reset --hard origin/<branch>`, forçando o diretório local a ser um espelho exato do repositório remoto, descartando quaisquer alterações locais que não foram comitadas.
4.  **Comunicação com Frontend**: Durante a sincronização/clonagem inicial, o servidor não possui os arquivos. A API `/api/pages/` retorna uma lista vazia e injeta um cabeçalho `X-Sync-Status: PENDING`. 
5.  **Polling do Cliente (`index.html`)**: A página inicial do sistema (`index.html`) possui um script JS nativo que verifica o status. Se receber `PENDING`, exibe um *spinner* de carregamento e faz um *polling* tentando novamente após 5 segundos, exibindo o conteúdo apenas quando receber o `X-Sync-Status: COMPLETE`.

### 2. Servindo uma Página Estática (Clean URL)
O sistema permite acessar páginas usando URLs amigáveis, sem a extensão `.html`.
1.  **Requisição do Usuário**: Um usuário acessa, por exemplo, `/rpg/personagens`.
2.  **Interceptação pelo `PageController`**: Como a URL não contém um `.` (indicando uma extensão de arquivo), ela é capturada pelo `PageController`.
3.  **Encaminhamento (Forward)**: O controlador analisa a URI e faz um *forward* interno para `/rpg/personagens.html`. Se a URI terminasse em `/`, como `/rpg/`, seria encaminhada para `/rpg/index.html`.
4.  **Resolução de Recurso Estático**: O `WebConfig` configura o Spring para procurar recursos estáticos no diretório clonado (`static-sites/`). O servidor encontra o arquivo `static-sites/rpg/personagens.html` e o entrega ao usuário.

### 3. Geração da Estrutura de Páginas (API)
O frontend pode construir um menu dinâmico consumindo a API.
1.  **Requisição à API**: O frontend (ou um cliente) faz uma chamada `GET` para `/api/pages/`.
2.  **`PagesController`**: Recebe a requisição e chama o `PagesService`.
3.  **`PagesService`**: Invoca o `PagesRepositoryPort` para obter a lista de todas as páginas. Em seguida, aplica uma regra de negócio, filtrando o resultado para incluir apenas os diretórios de nível superior desejados (ex: "Programing", "RPG", "Study").
4.  **`IOPersistence`**: Varre o sistema de arquivos apontado por `app.static.pages`, construindo recursivamente uma árvore do tipo `Pages`. Durante este processo:
    * Arquivos do repositório (`.git`) são sumariamente ignorados.
    * Para pastas que possuem um `index.html` na raiz, o nome da pasta com primeira letra em maiúscula é utilizado como título, para gerar uma navegação mais semântica.
5.  **Resposta**: O `PagesController` recebe a lista filtrada, define o cabeçalho `X-Sync-Status: COMPLETE` e retorna a estrutura em formato JSON.

## Configurações Chave e Variáveis de Ambiente

O sistema é configurado pelo arquivo `application.properties` e aceita sobreposições via variáveis de ambiente (ideal para deploy em contêineres e serviços de nuvem):

*   **Spring Boot Internals**:
    *   `PORT` (padrão `8081`): Porta onde a aplicação rodará.
*   `spring.mvc.pathmatch.matching-strategy=ant_path_matcher`: Essencial para o funcionamento do `PageController` e o mecanismo de "Clean URLs".
    *   `spring.web.resources.add-mappings=true`: Habilita roteamento de recursos estáticos do Spring.
    *   `spring.thymeleaf.cache=false`: Desativa o cache do Thymeleaf, útil para permitir que arquivos injetados/alterados pelo repositório sincronizado reflitam as mudanças instantaneamente.
*   **Aplicação (Domínio e Persistência)**:
    *   `app.static.pages` (padrão `file:./static-sites/`): Define o caminho local no disco onde o conteúdo servido e o repositório ficarão hospedados.
    *   `app.test.cors` (padrão `true`): Flag de segurança. Se verdadeiro, permite requisições de origens indiscriminadas (`*`). Se falso, o CORS é estrito às origens `https://www.kkphoenix.com.br` e a API correspondente.
*   **Conexão Git**:
    *   `GIT_REPO_URL`: Endereço HTTP(S) do repositório fonte de conteúdo.
    *   `GIT_REPO_BRANCH` (padrão `main`): Branch a ser espelhada.
    *   `GIT_USERNAME` e `GIT_TOKEN`: (Opcionais) Credenciais seguras para clonagem de repositórios privados ou contorno de limites de requisições.
