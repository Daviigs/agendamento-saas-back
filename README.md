# Sistema de Agendamento para Salão de Beleza - Backend

## 📋 Visão Geral

Sistema backend para gerenciamento de agendamentos de salão de beleza, desenvolvido em **Java 21** com **Spring Boot 4.0.0**. O sistema oferece suporte a **multi-tenancy**, permitindo que múltiplos salões utilizem a mesma aplicação com isolamento completo de dados.

### Funcionalidades Principais

- ✅ **Gestão de Agendamentos**: Criação, consulta e cancelamento de agendamentos
- ✅ **Múltiplos Serviços por Agendamento**: Um cliente pode agendar vários serviços de uma vez
- ✅ **Gestão de Serviços**: CRUD completo de serviços oferecidos
- ✅ **Bloqueio de Datas**: Bloqueio de datas específicas (feriados) ou dias da semana recorrentes (folgas)
- ✅ **🆕 Bloqueio de Horários Específicos**: Bloquear intervalos de tempo em datas específicas
- ✅ **🆕 Bloqueio de Horários Recorrentes**: Bloquear intervalos semanalmente (ex: almoço toda segunda)
- ✅ **🆕 Horários Personalizados por Profissional**: Cada tenant tem seu próprio horário de trabalho
- ✅ **🆕 Cálculo Inteligente de Disponibilidade**: Considera todos os bloqueios e agendamentos
- ✅ **Validação de Conflitos**: Impede agendamentos em horários já ocupados ou bloqueados
- ✅ **Notificações WhatsApp**: Confirmação, cancelamento e lembretes automáticos
- ✅ **Lembretes Automáticos**: Envio de lembretes 2 horas antes do agendamento
- ✅ **Multi-Tenancy**: Isolamento de dados por cliente (tenant)

## 🆕 Sistema Avançado de Bloqueio de Horários

**Nova Funcionalidade v1.0.0** - Sistema completo de gerenciamento de disponibilidade:

- **Horários de Trabalho Configuráveis**: Cada profissional define seu próprio horário (ex: kc: 09:00-18:00, mjs: 07:00-16:00)
- **Bloqueios Pontuais**: Bloqueie horários específicos em datas específicas (ex: 14:00-16:00 em 25/01/2026)
- **Bloqueios Recorrentes**: Bloqueie horários semanalmente (ex: 12:00-13:00 todas as segundas-feiras)
- **Desbloqueio Flexível**: Remova bloqueios tornando horários disponíveis novamente

📚 **Documentação Completa**: 
- [ADVANCED_BLOCKING_SYSTEM.md](ADVANCED_BLOCKING_SYSTEM.md) - Documentação do sistema
- [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) - Guia de migração
- [ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md) - Diagramas de arquitetura
- [API_EXAMPLES.json](API_EXAMPLES.json) - Exemplos de requisições

---

## 🏗️ Arquitetura

### Padrão Arquitetural

O projeto segue a arquitetura **MVC (Model-View-Controller)** em camadas, com separação clara de responsabilidades:

```
┌─────────────────────────────────────────────────┐
│           Controllers (REST API)                 │
│  - AppointmentsController                        │
│  - ServicesController                            │
│  - BlockedDayController                          │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│              Services (Lógica de Negócio)        │
│  - AppointmentsService                           │
│  - ServicesService                               │
│  - BlockedDayService                             │
│  - WhatsappService                               │
│  - TenantService                                 │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│         Repositories (Acesso a Dados)            │
│  - AppointmentsRepository                        │
│  - ServicesRepository                            │
│  - BlockedDayRepository                          │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│              Banco de Dados (PostgreSQL)         │
│  - tb_appointments                               │
│  - tb_services                                   │
│  - tb_blocked_days                               │
│  - tb_appointment_services (join table)          │
└──────────────────────────────────────────────────┘
```

### Componentes Principais

#### 1. **Controllers (Camada de Apresentação)**
- Expõem APIs REST
- Validam entrada de dados
- Gerenciam contexto de tenant
- Retornam respostas HTTP padronizadas

#### 2. **Services (Camada de Negócio)**
- Implementam regras de negócio
- Coordenam operações entre repositories
- Validam regras complexas
- Integram com serviços externos (WhatsApp)

#### 3. **Repositories (Camada de Dados)**
- Abstraem acesso ao banco de dados
- Utilizam Spring Data JPA
- Queries customizadas quando necessário

#### 4. **Entities (Modelo de Domínio)**
- Representam tabelas do banco
- Mapeamento JPA/Hibernate
- Validações básicas (Bean Validation)

#### 5. **DTOs (Data Transfer Objects)**
- Contratos de entrada/saída da API
- Validações de requisições
- Isolamento entre camadas

#### 6. **Exception Handlers**
- Tratamento centralizado de erros
- Respostas HTTP padronizadas
- Log de erros

#### 7. **Config (Configurações)**
- CORS
- Interceptors (Multi-tenancy)
- Constantes da aplicação

#### 8. **Scheduler**
- Tarefas agendadas
- Envio automático de lembretes

---

## 📁 Estrutura de Pacotes

```
lash_salao_kc.agendamento_back/
│
├── config/                         # Configurações da aplicação
│   ├── AppConstants.java           # Constantes centralizadas
│   ├── CorsConfig.java             # Configuração de CORS
│   ├── GlobalExceptionHandler.java # Tratamento global de exceções
│   ├── TenantContext.java          # Contexto de tenant (ThreadLocal)
│   ├── TenantInterceptor.java      # Interceptor de validação de tenant
│   └── WebConfig.java              # Configuração de interceptors
│
├── controller/                     # Controllers REST
│   ├── BaseController.java         # Controller base (métodos comuns)
│   ├── AppointmentsController.java # Endpoints de agendamentos
│   ├── BlockedDayController.java   # Endpoints de dias bloqueados
│   └── ServicesController.java     # Endpoints de serviços
│
├── domain/                         # Modelos de domínio
│   ├── dto/                        # Data Transfer Objects
│   │   ├── CreateAppointmentRequest.java
│   │   ├── CreateServiceRequest.java
│   │   ├── UpdateServiceRequest.java
│   │   ├── BlockSpecificDateRequest.java
│   │   ├── BlockRecurringDayRequest.java
│   │   └── Whats.java              # DTO para WhatsApp
│   │
│   └── entity/                     # Entidades JPA
│       ├── AppointmentsEntity.java # Agendamentos
│       ├── ServicesEntity.java     # Serviços
│       ├── BlockedDayEntity.java   # Dias bloqueados
│       └── UserEntity.java         # Usuário (não persistido)
│
├── exception/                      # Exceções personalizadas
│   ├── BusinessException.java      # Erros de regra de negócio
│   ├── ResourceNotFoundException.java # Recurso não encontrado
│   ├── DuplicateResourceException.java # Recurso duplicado
│   └── AppointmentConflictException.java # Conflito de horário
│
├── repository/                     # Repositórios JPA
│   ├── AppointmentsRepository.java
│   ├── ServicesRepository.java
│   └── BlockedDayRepository.java
│
├── scheduler/                      # Tarefas agendadas
│   └── AppointmentReminderScheduler.java # Envio de lembretes
│
├── service/                        # Serviços de negócio
│   ├── AppointmentsService.java    # Lógica de agendamentos
│   ├── BlockedDayService.java      # Lógica de bloqueios
│   ├── ServicesService.java        # Lógica de serviços
│   ├── WhatsappService.java        # Integração WhatsApp
│   └── TenantService.java          # Gestão de tenants
│
└── AgendamentoBackApplication.java # Classe principal
```

---

## 🗄️ Modelo de Dados

### Diagrama ER (Entidade-Relacionamento)

```
┌─────────────────────┐
│   tb_services       │
├─────────────────────┤
│ service_id (PK)     │
│ tenant_id           │
│ name                │
│ duration (minutes)  │
│ price               │
└──────────┬──────────┘
           │
           │ Many-to-Many
           │
           ▼
┌───────────────────────────┐
│ tb_appointment_services   │ (Join Table)
├───────────────────────────┤
│ appointment_id (FK)       │
│ service_id (FK)           │
└──────────┬────────────────┘
           │
           │
           ▼
┌─────────────────────┐
│  tb_appointments    │
├─────────────────────┤
│ appointment_id (PK) │
│ tenant_id           │
│ appointment_date    │
│ start_time          │
│ end_time            │
│ user_name           │
│ user_phone          │
│ reminder_sent       │
└─────────────────────┘


┌─────────────────────┐
│  tb_blocked_days    │
├─────────────────────┤
│ blocked_day_id (PK) │
│ tenant_id           │
│ specific_date       │
│ day_of_week         │
│ reason              │
│ is_recurring        │
└─────────────────────┘
```

### Descrição das Tabelas

#### **tb_appointments**
Armazena os agendamentos realizados pelos clientes.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| appointment_id | UUID | Identificador único |
| tenant_id | VARCHAR | ID do salão (multi-tenancy) |
| appointment_date | DATE | Data do agendamento |
| start_time | TIME | Horário de início |
| end_time | TIME | Horário de término |
| user_name | VARCHAR | Nome do cliente |
| user_phone | VARCHAR | Telefone do cliente |
| reminder_sent | BOOLEAN | Lembrete foi enviado? |

#### **tb_services**
Catálogo de serviços oferecidos pelo salão.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| service_id | UUID | Identificador único |
| tenant_id | VARCHAR | ID do salão |
| name | VARCHAR | Nome do serviço |
| duration | INTEGER | Duração em minutos |
| price | DOUBLE | Preço do serviço |

#### **tb_blocked_days**
Datas ou dias da semana bloqueados para agendamento.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| blocked_day_id | UUID | Identificador único |
| tenant_id | VARCHAR | ID do salão |
| specific_date | DATE | Data específica bloqueada |
| day_of_week | VARCHAR | Dia da semana (enum) |
| reason | VARCHAR | Motivo do bloqueio |
| is_recurring | BOOLEAN | Bloqueio recorrente? |

---

## 🔐 Multi-Tenancy

### Como Funciona

O sistema utiliza **discriminação por coluna** (`tenant_id`) para isolar dados de diferentes clientes:

1. **Header HTTP**: Toda requisição deve incluir `X-Tenant-Id` (ou `X-Client-Id`)
2. **Interceptor**: Valida o tenant e injeta no contexto (ThreadLocal)
3. **Context**: `TenantContext` armazena o tenant da requisição atual
4. **Repositories**: Filtram automaticamente por `tenant_id`

### Exceções (Endpoints sem validação de tenant)

Os seguintes endpoints **não exigem** o header `X-Tenant-Id`:

- ✅ **OPTIONS** - Requisições CORS preflight
- ✅ **/error** - Páginas de erro do Spring
- ✅ **/h2-console/** - Console do H2 Database
- ✅ **/swagger-ui/** - Documentação Swagger/OpenAPI
- ✅ **/v3/api-docs/** - Especificação OpenAPI
- ✅ **/actuator/** - Endpoints do Spring Actuator
- ✅ **/static/** - Recursos estáticos

### Fluxo de Requisição

```
Cliente → X-Tenant-Id: kc
    ↓
TenantInterceptor (valida tenant)
    ↓
TenantContext.setTenantId("kc")
    ↓
Controller (usa tenant)
    ↓
Service (usa tenant do contexto)
    ↓
Repository (filtra por tenant_id)
    ↓
Banco de Dados
    ↓
TenantContext.clear() (finally)
```

### Tenants Configurados

- `kc` - KC Lash Studio
- `mjs` - MJS Beauty

**NOTA**: Em produção, buscar tenants de tabela `tb_tenants` no banco.

---

## 🚀 Tecnologias Utilizadas

### Backend
- **Java 21** - Linguagem de programação
- **Spring Boot 4.0.0** - Framework principal
- **Spring Data JPA** - Persistência de dados
- **Spring Validation** - Validação de entrada
- **Lombok** - Redução de boilerplate
- **PostgreSQL** - Banco de dados principal
- **H2 Database** - Banco em memória (testes)

### Ferramentas
- **Maven** - Gerenciamento de dependências
- **Selenium** - Automação de navegador (WhatsApp Web)
- **SpringDoc OpenAPI** - Documentação automática da API
- **SLF4J/Logback** - Sistema de logs

### Integrações
- **WhatsApp API** - Envio de notificações
  - Endpoint: `http://localhost:3001/whatsapp`
  - Endpoints disponíveis:
    - `/agendamento` - Confirmação de novo agendamento
    - `/lembrete` - Lembrete 2 horas antes
    - `/cancelamento` - Notificação de cancelamento

---

## ⚙️ Configuração e Execução

### Pré-requisitos

- Java 21 ou superior
- Maven 3.8+
- PostgreSQL 14+
- API de WhatsApp rodando na porta 3001

### Configuração do Banco de Dados

1. **Criar banco de dados**:
```sql
CREATE DATABASE agendamentodb;
CREATE USER appuser WITH ENCRYPTED PASSWORD '40028922Aa!';
GRANT ALL PRIVILEGES ON DATABASE agendamentodb TO appuser;
```

2. **Configurar `application.properties`**:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agendamentodb
spring.datasource.username=appuser
spring.datasource.password=40028922Aa!
```

### Executar Aplicação

#### Desenvolvimento (IDE)
```bash
# Executar classe principal
AgendamentoBackApplication.java
```

#### Linha de Comando
```bash
# Compilar
mvn clean install

# Executar
mvn spring-boot:run

# Ou executar JAR
java -jar target/agendamento-back-0.0.1-SNAPSHOT.jar
```

### Perfis de Execução

- `dev` - Desenvolvimento (ativo por padrão)
- `prod` - Produção (ajustar `application.properties`)

### Portas

- **Backend**: `8080` (padrão Spring Boot)
- **WhatsApp API**: `3001` (externa)

---

## 📡 Endpoints da API

### Base URL
```
http://localhost:8080
```

### Headers Obrigatórios
```
X-Tenant-Id: kc|mjs
Content-Type: application/json
```

---

### 🗓️ Agendamentos

#### Criar Agendamento
```http
POST /appointments
```

**Body**:
```json
{
  "serviceIds": ["uuid1", "uuid2"],
  "date": "2026-01-15",
  "startTime": "10:00",
  "userName": "Maria Silva",
  "userPhone": "+5511999999999"
}
```

#### Consultar Horários Disponíveis
```http
GET /appointments/available-slots?date=2026-01-15
```

#### Listar Agendamentos por Data
```http
GET /appointments?date=2026-01-15
```

#### Agendamentos Futuros por Telefone
```http
GET /appointments/future?userPhone=5511999999999
```

#### Agendamentos Passados por Telefone
```http
GET /appointments/past?userPhone=5511999999999
```

#### Consultar Agendamento por ID
```http
GET /appointments/{appointmentId}
```

#### Cancelar Agendamento
```http
DELETE /appointments/{appointmentId}
```

---

### 💅 Serviços

#### Criar Serviço
```http
POST /services
```

**Body**:
```json
{
  "name": "Design de Sobrancelhas",
  "duration": 60,
  "price": 80.00
}
```

#### Listar Serviços
```http
GET /services
```

#### Consultar Serviço
```http
GET /services/{serviceId}
```

#### Atualizar Serviço
```http
PUT /services/{serviceId}
```

#### Deletar Serviço
```http
DELETE /services/{serviceId}
```

---

### 🚫 Dias Bloqueados

#### Bloquear Data Específica
```http
POST /blocked-days/specific
```

**Body**:
```json
{
  "date": "2026-12-25",
  "reason": "Natal"
}
```

#### Bloquear Dia da Semana Recorrente
```http
POST /blocked-days/recurring
```

**Body**:
```json
{
  "dayOfWeek": "SUNDAY",
  "reason": "Folga semanal"
}
```

#### Listar Datas Bloqueadas
```http
GET /blocked-days
GET /blocked-days/specific
GET /blocked-days/recurring
```

#### Consultar Datas Disponíveis
```http
GET /blocked-days/available?startDate=2026-01-01&endDate=2026-01-31
```

#### Desbloquear Data
```http
DELETE /blocked-days/{blockedDayId}
```

---

## 🔔 Sistema de Lembretes

### Funcionamento

O `AppointmentReminderScheduler` executa **a cada minuto** e:

1. Busca agendamentos que ocorrerão em **2 horas**
2. Filtra apenas agendamentos sem lembrete enviado
3. Envia mensagem via WhatsApp para o cliente
4. Marca agendamento como `reminderSent = true`

### Configuração

```java
REMINDER_HOURS_BEFORE = 2;        // Anteced ência do lembrete
SCHEDULER_INTERVAL_MS = 60000;    // Intervalo de execução (1 min)
```

---

## 📝 Regras de Negócio

### Horário de Funcionamento
- **Abertura**: 09:00
- **Fechamento**: 18:00
- **Último Horário**: 16:00 (para início de agendamento)
- **Intervalo de Slots**: 30 minutos

### Validações de Agendamento

1. ✅ Data não pode estar bloqueada
2. ✅ Horário deve estar dentro do expediente
3. ✅ Não pode haver conflito com agendamentos existentes
4. ✅ Serviços devem existir
5. ✅ Duração total não pode exceder horário de fechamento

### Cálculo de Horário

```java
endTime = startTime + Σ(durations dos serviços)
```

**Exemplo**:
- Serviço 1: 60 min
- Serviço 2: 30 min
- Start: 10:00
- **End: 11:30**

---

## 🎯 Boas Práticas Aplicadas

### Clean Code
- ✅ Nomes descritivos e auto-explicativos
- ✅ Métodos pequenos e coesos (SRP)
- ✅ Comentários apenas quando necessário
- ✅ Formatação consistente

### SOLID
- ✅ **S**ingle Responsibility Principle
- ✅ **O**pen/Closed Principle (via interfaces)
- ✅ **L**iskov Substitution Principle
- ✅ **I**nterface Segregation Principle
- ✅ **D**ependency Inversion (Injeção de Dependência)

### DRY (Don't Repeat Yourself)
- ✅ `BaseController` elimina duplicação nos controllers
- ✅ Constantes centralizadas em `AppConstants`
- ✅ Métodos auxiliares reutilizáveis

### Separação de Responsabilidades
- ✅ Controllers: apenas entrada/saída HTTP
- ✅ Services: lógica de negócio
- ✅ Repositories: acesso a dados
- ✅ Entities: modelo de domínio
- ✅ DTOs: contratos da API

### Tratamento de Erros
- ✅ Exceções personalizadas por tipo de erro
- ✅ Handler global centralizado
- ✅ Códigos HTTP semânticos
- ✅ Mensagens descritivas

### Documentação
- ✅ Javadoc em todas as classes e métodos públicos
- ✅ README completo
- ✅ Comentários explicativos em lógicas complexas

---

## 🧪 Testes

### Executar Testes

```bash
mvn test
```

### Cobertura de Testes

**TODO**: Implementar testes unitários e de integração

Sugestões:
- Unit tests para Services
- Integration tests para Controllers
- Tests de Repository com H2

---

## 📊 Swagger/OpenAPI

Documentação automática da API disponível em:

```
http://localhost:8080/swagger-ui.html
```

---

## 🐛 Troubleshooting

### Erro de Conexão com Banco

```
Verifique:
- PostgreSQL está rodando?
- Credenciais corretas em application.properties?
- Banco 'agendamentodb' existe?
```

### Erro de Multi-Tenancy

```
Status 400: Header X-Client-Id é obrigatório
→ Adicionar header em todas as requisições (exceto OPTIONS/error/swagger)
```

```
Status 403: Tenant não autorizado
→ Tenant deve estar em TenantService.getAllActiveTenants()
```

### Erros CORS/Preflight

```
❌ Requisição bloqueada: Header X-Client-Id ausente | Endpoint: OPTIONS
→ RESOLVIDO: Requisições OPTIONS agora são permitidas automaticamente
→ Certifique-se de reiniciar a aplicação após atualização
```

### Lembretes Não Enviando

```
Verifique:
- API do WhatsApp está rodando em http://localhost:3001?
- Scheduler está ativo (@EnableScheduling)?
- Logs de erro no console?
```

---

## 🚀 Melhorias Futuras

### Funcionalidades
- [ ] Autenticação e autorização (Spring Security)
- [ ] Painel administrativo
- [ ] Relatórios e métricas
- [ ] Pagamentos online
- [ ] Avaliações de serviços
- [ ] Sistema de fidelidade

### Técnicas
- [ ] Cache (Redis)
- [ ] Filas de mensagens (RabbitMQ/Kafka)
- [ ] Containerização (Docker)
- [ ] CI/CD (GitHub Actions)
- [ ] Monitoramento (Prometheus/Grafana)
- [ ] Testes automatizados completos

---

## 👥 Contato e Suporte

**Desenvolvedor**: [Seu Nome]
**Email**: [seu-email@exemplo.com]
**Repositório**: [URL do repositório]

---

## 📄 Licença

[Definir licença - MIT, Apache 2.0, etc.]

---

**Última Atualização**: 12/01/2026
**Versão**: 0.0.1-SNAPSHOT

