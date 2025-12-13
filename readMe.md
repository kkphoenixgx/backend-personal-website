# Backend kkphoenix Website

Resumo:

1. Repositório e script → gerar arquivos estáticos.
2. Backend em Spring Boot → servir dinamicamente.
3. Infra Google Cloud → deploy do backend.
4. Actions → automatizar deploy.
5. Sites estáticos → deploy separado.
6. Testes finais.

## Checklist

* [X] – Criar repositório no GitHub para os arquivos staticos
* [X] - Cria esse backend usando Quartz e vê como serve os arquivos
* [X] - Script que dá push nos arquivos para o servidor de arquivos staticos

* [ ] – Criar projeto Spring Boot simples que serve arquivos staticos dinamicamente (backend).
* [ ] – Dockerizar a aplicação.

* [ ] – Criar conta no você sabe e configurar a CLI.
* [ ] – Subir a imagem para o Artifact Registry.
* [ ] – Deploy inicial no sabe lá r.

* [ ] – Configurar GitHub Actions para:
  * [ ] – Buildar Docker do backend quando tiver alteração no repositório de arquivos staticos.
  * [ ] – Enviar para Artifact Registry.
  * [ ] – Deploy no sabe lá R.

* [ ] – Configurar deploy automático dos sites estáticos (/site1, /site2, /site3) na vercel.

* [ ] – Testar push no GitHub e validar backend + sites no ar.

## AI patterns

```defaulkt
Beleza, eu preciso fazer isso, mas eu quero que isso seja uma aventura, gemini, e essa aventura deve ser com você me explicando, não me dando respostas, entendeu?

Beleza, Toda vez que você me der informação, eu quero o link da dock do spring para eu ler, LINKS DA DOC NA BÚLSOLA.

PARA DE ME DAR CÓDIGO, EU SÓ QUERO FONTES
```

```base

<default>

Quero conseguir dar pull em um repositório de tempos em tempos, colocar o conteúdo dele em uma pasta que deve servir o conteúdo, que será arquivos de estáticos html + css + js. E isso inclui roteamento das páginas
```

### Stage area texts

Missão 1: A Vigília (Agendando o git pull)
Para que sua aplicação execute uma tarefa repetidamente (como um git pull), você precisa usar o mecanismo de agendamento do Spring.

-[X] Habilitando o Agendador: O primeiro passo é "ligar" o sistema de agendamento. Você já fez isso ao colocar a anotação @EnableScheduling na sua classe App.java. Isso diz ao Spring: "Esteja pronto para executar tarefas agendadas".

-[X] Criando a Tarefa Agendada: Agora, você precisa criar um método que contenha a lógica do git pull e marcá-lo com a anotação @Scheduled. Esta anotação é a ordem que diz quando o método deve ser executado.

-[X] A Lógica do Git: Dentro do método agendado, você usará a biblioteca JGit (que você já planejou adicionar ao pom.xml). A lógica será:

-[ ] Na primeira execução, se a pasta local não existir, use o JGit para clonar o repositório.
Nas execuções seguintes, se a pasta já existir, use o JGit para executar um pull e buscar as atualizações.
Sua Bússola (Fontes):

Habilitando e Criando Tarefas Agendadas (@EnableScheduling, @Scheduled):

Link: Spring Framework 4.2.x Docs - Task Execution and Scheduling (Seção 34.4, The @Scheduled annotation). Esta é a documentação exata para a versão do Spring que seu projeto usa.

**Executando Comandos Git com JGit:**

Link: JGit Cookbook. Este é um guia prático essencial. Procure pelas "receitas" de Clone a remote repository e Pull from a remote repository.

Missão 2: O Cofre (Definindo o Local dos Arquivos)
Você precisa de um lugar no servidor para guardar os arquivos que o JGit irá baixar. É uma má prática colocar o caminho absoluto para essa pasta diretamente no seu código Java. A maneira correta é usar o application.properties.

-[ ] Configuração Externalizada: Defina uma propriedade personalizada no seu arquivo application.properties para guardar o caminho do "Cofre". Por exemplo: app.static.location=file:/opt/website-files/.

Missão 3: A Ponte (Servindo os Arquivos Estáticos e Roteamento)
Com os arquivos no "Cofre", você precisa dizer ao Spring para servi-los. É aqui que o seu arquivo WebConfig.java entra em ação.

-[ ] Manipuladores de Recursos (Resource Handlers): Você precisa implementar a interface WebMvcConfigurer e sobrescrever o método addResourceHandlers. Dentro deste método, você mapeia um padrão de URL (como /** para "todas as requisições") para o local físico dos seus arquivos (o "Cofre").

-[ ] Injetando a Configuração: Para obter o caminho do "Cofre" que você definiu no application.properties, você usará a anotação @Value na sua classe WebConfig.

Roteamento: O "roteamento" para arquivos estáticos é uma consequência direta dessa configuração. Se você mapeou /** para a pasta /opt/website-files/, uma requisição no navegador para `http://seu-site.com/contato.html` fará o Spring procurar e servir o arquivo /opt/website-files/contato.html. Uma requisição para `http://seu-site.com/assets/style.css` servirá o arquivo /opt/website-files/assets/style.css. A estrutura de pastas do seu repositório Git define as suas rotas.

Sua Bússola (Fontes):

Configurando Recursos Estáticos (addResourceHandlers):
Link: Spring Framework 4.2.x Docs - Configuring Static Resources. Esta seção da documentação do Spring MVC é o guia definitivo para o método addResourceHandlers e explica como servir arquivos de locais externos.
Com estas fontes, você tem o mapa completo para completar sua missão. Boa sorte na sua aventura, capitão!
