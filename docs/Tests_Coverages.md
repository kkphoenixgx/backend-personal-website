# Cobertura de Testes - Backend Personal Website

Este documento descreve a cobertura de testes atual da API do site pessoal (`personal-website`), detalhando as lógicas validadas nas camadas da Arquitetura Hexagonal adotada e levantando os cenários faltantes.

## Objetivo Geral
Garantir que as regras de filtragem de domínios, a leitura do sistema de arquivos e as lógicas de roteamento web dinâmico funcionem conforme o esperado para servir conteúdo estático provisionado a partir de um repositório Git.

## O que já está coberto (Cenários Atuais)

### 1. Camada de Domínio (`PagesServiceTest`)
- **Filtragem de Pastas Raiz:** Verifica a regra de negócio central. Dado um repositório lendo várias pastas ("Programing", "Personal", "Another", "RPG", "Study"), o serviço valida se filtra e devolve exclusivamente os diretórios base configurados ("Programing", "RPG", "Study").

### 2. Camada de Persistência / IO (`IOPersistenceTest`)
- **Leitura Recursiva do Diretório:** Verifica se a montagem da árvore de páginas a partir dos arquivos físicos ocorre corretamente e com as propriedades de sub-itens preenchidas.
- **Ignorar Arquivos de Sistema:** Garante que pastas internas do versionamento, como `.git`, sejam completamente ignoradas na geração da árvore.
- **Nomenclatura Baseada em Index:** Valida se a aplicação identifica inteligentemente pastas contendo `index.html` e atribui o nome em formato legível ao diretório, apontando seu caminho diretamente para o arquivo `index`.
- **Estresse e Escalabilidade (Large Scale Tree):** Valida se o sistema consegue processar eficientemente (sem estourar memória ou a pilha de execução) uma árvore com 10.000 arquivos gerados dinamicamente.

### 3. Camada Web e Controladores (`PagesControllerTest`, `PageControllerTest`, `CustomErrorControllerTest`)
- **API de Páginas (`PagesControllerTest`):**
  - Garante que a requisição GET retorne o header `X-Sync-Status: COMPLETE` e a lista em JSON quando o repositório possuir arquivos.
  - Verifica o header `X-Sync-Status: PENDING` e corpo vazio caso os dados ainda estejam sendo baixados pela persistência.
- **Roteamento Clean URLs (`PageControllerTest`):**
  - Valida o forward dinâmico de diretórios base para a raiz respectiva (ex: `/blog/` -> `/blog/index.html`).
  - Valida a reescrita invisível e o forward de requisições de páginas sem extensão (ex: `/blog/my-post` -> `/blog/my-post.html`).
  - Verifica o servimento e tipo de resposta de assets (ex: `.jpg`) localizados em subdiretórios profundos da aplicação local.
  - **Prevenção de Vazamento de Abstração:** Garante o redirecionamento permanente (301) de acessos diretos a arquivos `.html` para suas versões amigáveis (Clean URLs).
  - **Segurança contra Path Traversal:** Bloqueia acessos indevidos e tentativas de escapar da pasta base utilizando manipulação de caminhos (ex: `../`).
- **Erros Personalizados (`CustomErrorControllerTest`):**
  - Assegura que um 404 em sub-rotas leve a páginas exclusivas de falha dependendo da pasta (ex: erro dentro de `/RPG/*` vai para `/RPG/404.html`).
  - Garante retorno à view padrão `error` caso ocorra um 404 na raiz ou em locais não mapeados.

### 4. Camada de Configuração (`WebConfigTest`)
- **Mapeamento de CORS:** Verifica se `CorsRegistry` bloqueia origens externas quando em modo de produção e permite `*` quando em teste.
- **Recursos Estáticos:** Valida se a adição do prefixo `file:` e o mapeamento de recursos dinâmicos estão sendo passados corretamente ao Spring.

### 5. Casos Limite em Diretórios (`IOPersistenceTest`)
- **Comportamentos Iesperados:** Valida o início da aplicação apontando para um diretório inexistente ou a exclusão do diretório em tempo de execução.
- **Arquivos Insuportados:** Garante que a inserção de arquivos não mapeados não quebra a estrutura JSON da API.

### 6. Serviço de Sincronização (`GitPersistenceTest`)
- **Clonagem e Sincronização Dinâmica:** Valida a criação segura (Clonagem) e as rotinas diárias de atualizações (*force pull* de *fetch + hard reset*).
- **Resiliência:** Testa exceções controladas com URLs inválidas para que uma quebra de rede não faça o servidor cair.
- **Auto-recuperação (Corrupção do Git):** Simula a corrupção severa do `.git` local para assegurar que a aplicação se recupere automaticamente limpando o diretório e clonando-o do zero.
- **Controle de Concorrência (Race Conditions):** Previne a sobreposição de execuções de sincronização através da thread assíncrona agendada utilizando controle de estado (`AtomicBoolean`).
