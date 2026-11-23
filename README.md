# FinTrack

Aplicação desktop de controle financeiro pessoal, feita em **Java 21 + JavaFX**, que registra receitas e despesas em um banco **SQLite** local e mostra o saldo atual.

Repositório: https://github.com/jdavidbrandao/FinTrack

---

## 1. Como funciona

### Visão geral

O usuário usa duas telas:

1. **Tela principal** (`Principal.fxml`): tabela com todas as transações (ID, descrição, valor, tipo, data) e o **saldo total** no topo. O saldo aparece com estilo diferente se for positivo, negativo ou zero. Botões **Adicionar** e **Remover**.
2. **Tela de cadastro** (`NovaTransacao.fxml`): escolhe o tipo, digita valor, descrição e data, e salva ou cancela.

**Tipos de transação:** `Receita`, `Despesa`, `Receita mensal` e `Despesa mensal`.

### Fluxo de uma ação (exemplo: adicionar transação)

```
NovaTransacaoController.salvar()
   └─ valida/converte o valor (formato pt-BR, ex: 1234,56)
   └─ cria Transacao (ou TransacaoMensal)  → valida valor ≥ 0 e descrição não vazia
   └─ FinTracker.adicionarTransacao(t)
        └─ TransacaoDao.salvar(t)          → INSERT no SQLite
   └─ volta para a tela principal, que recarrega a lista e o saldo
```

### Arquitetura em camadas

```
View (FXML + CSS)
   ↓
Controller  (PrincipalController, NovaTransacaoController)
   ↓
FinTracker  (fachada com as operações: adicionar, remover, listar, saldo)
   ↓
DAO         (TransacaoDao: acesso ao banco via JDBC)
   ↓
Conexao     (abre a conexão com o SQLite)
```

### Estrutura de pastas

```
FinTrack/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/fintrack/
    │   │   ├── app/         FinApp.java               → ponto de entrada (JavaFX Application)
    │   │   ├── controller/  PrincipalController.java
    │   │   │                NovaTransacaoController.java
    │   │   │                FinTracker.java           → regra de negócio / fachada
    │   │   ├── dao/         TransacaoDao.java         → CRUD + cálculo de saldo
    │   │   ├── db/          Conexao.java              → jdbc:sqlite:fintrack.db
    │   │   ├── exception/   ValorInvalidoException.java
    │   │   └── model/       Transacao.java
    │   │                    TransacaoMensal.java      → herda de Transacao
    │   └── resources/fintrack/view/
    │                        Principal.fxml, NovaTransacao.fxml, style.css
    └── test/java/fintrack/  TransacaoTest, TransacaoDaoTest, AppTest
```

### Detalhes importantes de implementação

- **Valores em centavos:** o valor é arredondado (`Math.round(valor * 100)`) e gravado como inteiro (`INTEGER`) ao salvar, e dividido por 100 ao ler, para evitar erros de ponto flutuante no banco.
- **Saldo calculado no SQL:** um `SUM(CASE ...)` soma `Receita`/`Receita mensal` e subtrai `Despesa`/`Despesa mensal`.
- **Banco criado automaticamente:** ao instanciar o `TransacaoDao`, o `CREATE TABLE IF NOT EXISTS transacoes` é executado. O arquivo `fintrack.db` é criado na pasta de onde a aplicação é executada.
- **Erros de banco propagam até a tela:** `salvar`, `remover`, `buscarTodos` e `buscarSaldo` lançam `SQLException` em vez de só imprimir no console, então a interface mostra um `Alert` quando algo falha. As conexões são fechadas com try-with-resources.
- **Validação no modelo:** `Transacao` lança `ValorInvalidoException` se o valor for negativo ou a descrição estiver vazia.
- **Formatação pt-BR:** valores exibidos como `R$ 1.234,56`. Na digitação, use vírgula como separador decimal e sem ponto de milhar (ex: `1234,56` ou `10,5`). Textos como `10.5` ou `12abc` são rejeitados com mensagem de valor inválido.

---

## 2. Tecnologias usadas

| Tecnologia | Versão | Para que serve |
|---|---|---|
| Java | 21 | Linguagem e runtime |
| JavaFX (`controls`, `fxml`) | 21 | Interface gráfica (telas em FXML + CSS) |
| Maven | - | Build e gerenciamento de dependências |
| `javafx-maven-plugin` | 0.0.8 | Executar o app com `mvn javafx:run` |
| SQLite (`sqlite-jdbc`) | 3.45.1.0 | Banco de dados local em arquivo |
| JDBC | - | Acesso ao banco (`PreparedStatement`) |
| JUnit Jupiter (JUnit 5) | 5.10.2 | Testes automatizados |
| Maven Surefire | 3.2.5 | Execução dos testes |
| `slf4j-simple` | 1.7.36 | Logs do driver SQLite |
| PostgreSQL JDBC | 42.7.3 | Declarado no `pom.xml`, mas **não é usado** no código atual |

---

## 3. Como inicializar

### Pré-requisitos

- **JDK 21** (ou superior compatível)
- **Maven 3.8+**
- Ambiente com interface gráfica (o JavaFX abre uma janela desktop)

Confira as versões:

```bash
java -version
mvn -version
```

### Passo a passo

```bash
# 1. Clonar o repositório
git clone https://github.com/jdavidbrandao/FinTrack.git
cd FinTrack

# 2. Baixar dependências e compilar
mvn clean compile

# 3. Executar a aplicação
mvn javafx:run
```

A classe principal é `fintrack.app.FinApp`. Na primeira execução, o arquivo `fintrack.db` é criado automaticamente na pasta do projeto, sem necessidade de configurar banco.

### Rodar os testes

```bash
mvn test
```

> **Atenção:** os testes do DAO (`TransacaoDaoTest`) usam o mesmo `fintrack.db` da aplicação. Eles inserem registros com a descrição `teste` e os removem ao final, mas é melhor não rodar com dados importantes no banco.

### Problemas comuns

- **`mvn javafx:run` não abre a janela:** verifique se o `JAVA_HOME` aponta para o JDK 21 e se você está em um ambiente com display (não funciona em terminal puro/SSH sem X).
- **Erro de versão do Java:** o `pom.xml` exige `source/target 21`. Com JDK menor, a compilação falha.
- **Resetar os dados:** apague o arquivo `fintrack.db`. Ele será recriado vazio na próxima execução.
- **"Valor inválido" ao salvar:** use vírgula como separador decimal e não use ponto (ex: `10,50`).
