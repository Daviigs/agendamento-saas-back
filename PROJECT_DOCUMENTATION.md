# 📚 Documentação Completa do Sistema de Agendamentos com Automação WhatsApp

## 📋 Índice
1. [Visão Geral do Projeto](#-visão-geral-do-projeto)
2. [Tecnologias Utilizadas](#-tecnologias-utilizadas)
3. [Arquitetura do Sistema](#-arquitetura-do-sistema)
4. [Endpoints da API](#-endpoints-da-api)
5. [Entidades do Banco de Dados](#-entidades-do-banco-de-dados)
6. [Serviços e Funções](#-serviços-e-funções)
7. [Sistema de Automação WhatsApp](#-sistema-de-automação-whatsapp)
8. [Scheduler e Lembretes Automáticos](#-scheduler-e-lembretes-automáticos)
9. [Regras de Negócio](#-regras-de-negócio)
10. [Configuração e Execução](#-configuração-e-execução)
11. [Integrações](#-integrações)

---

## 🎯 Visão Geral do Projeto

### Propósito
Sistema completo de agendamentos online para salão de beleza com automação de mensagens via WhatsApp. O sistema gerencia serviços, horários disponíveis, agendamentos de clientes e envia confirmações e lembretes automáticos.

### Principais Funcionalidades
- ✅ **Gestão de Serviços**: Criar, listar, editar e deletar serviços oferecidos
- ✅ **Sistema de Agendamentos**: Clientes podem agendar serviços em horários disponíveis
- ✅ **Gerenciamento de Disponibilidade**: Bloquear dias específicos ou dias da semana recorrentes
- ✅ **Automação WhatsApp**: Envio automático de confirmações e lembretes
- ✅ **Lembretes Inteligentes**: Notificações automáticas 2 horas antes do agendamento
- ✅ **Validações em Tempo Real**: Verificação de conflitos, horários disponíveis e dias bloqueados
- ✅ **Console Administrativo**: Interface H2 para visualização do banco de dados

---

## 🛠 Tecnologias Utilizadas

### Backend
- **Java 21** - Linguagem de programação
- **Spring Boot 4.0.0** - Framework principal
- **Spring Data JPA** - Persistência de dados
- **Spring Web MVC** - API REST
- **Spring Validation** - Validação de dados
- **Spring Scheduling** - Tarefas agendadas (lembretes)

### Banco de Dados
- **H2 Database** - Banco em memória para desenvolvimento
- **PostgreSQL** - Banco configurado para produção (opcional)

### Bibliotecas e Ferramentas
- **Lombok** - Redução de boilerplate code
- **SpringDoc OpenAPI** - Documentação automática da API
- **Selenium WebDriver** - Automação do WhatsApp Web
- **Maven** - Gerenciamento de dependências

### Integrações Externas
- **WhatsApp Web** - Envio de mensagens automatizadas
- **API de WhatsApp (localhost:3001)** - Serviço Node.js para comunicação com WhatsApp

---

## 🏗 Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│              (React/Angular/Vue - Separado)                  │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP REST API
┌────────────────────▼────────────────────────────────────────┐
│                    Spring Boot Backend                       │
├─────────────────────────────────────────────────────────────┤
│  Controllers (REST Endpoints)                               │
│    ├── AppointmentsController                               │
│    ├── ServicesController                                   │
│    └── BlockedDayController                                 │
├─────────────────────────────────────────────────────────────┤
│  Services (Lógica de Negócio)                               │
│    ├── AppointmentsService                                  │
│    ├── ServicesService                                      │
│    ├── BlockedDayService                                    │
│    └── WhatsappService                                      │
├─────────────────────────────────────────────────────────────┤
│  Repositories (Acesso a Dados)                              │
│    ├── AppointmentsRepository                               │
│    ├── ServicesRepository                                   │
│    └── BlockedDayRepository                                 │
├─────────────────────────────────────────────────────────────┤
│  Entities (Modelos de Dados)                                │
│    ├── AppointmentsEntity                                   │
│    ├── ServicesEntity                                       │
│    └── BlockedDayEntity                                     │
├─────────────────────────────────────────────────────────────┤
│  Scheduler                                                   │
│    └── AppointmentReminderScheduler (a cada 6 segundos)    │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│              H2 Database (In-Memory)                         │
│   Tables: tb_appointments, tb_services, tb_blocked_days     │
└─────────────────────────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│            WhatsApp API (Node.js)                            │
│         http://localhost:3001/whatsapp/*                     │
│    ├── POST /whatsapp/agendamento (confirmação)             │
│    └── POST /whatsapp/lembrete (lembrete 2h antes)          │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                  WhatsApp Web                                │
│            (Selenium WebDriver)                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🌐 Endpoints da API

### Base URL
```
http://localhost:8080
```

---

## 📅 Agendamentos (Appointments)

### 1. Criar Agendamento
**POST** `/appointments`

Cria um novo agendamento para um cliente.

**Request Body:**
```json
{
  "serviceId": "uuid-do-servico",
  "date": "2025-12-27",
  "startTime": "10:00",
  "userName": "Maria Silva",
  "userPhone": "5511999999999"
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid-do-agendamento",
  "date": "2025-12-27",
  "startTime": "10:00:00",
  "endTime": "11:00:00",
  "service": {
    "id": "uuid-do-servico",
    "name": "Design de Sobrancelhas",
    "duration": 60,
    "price": 80.00
  },
  "userName": "Maria Silva",
  "userPhone": "5511999999999",
  "reminderSent": false
}
```

**Validações:**
- Verifica se o serviço existe
- Valida se a data não está bloqueada
- Valida se o horário está dentro do expediente (09:00 - 18:00)
- Verifica conflitos com outros agendamentos
- **Envia confirmação automática via WhatsApp**

---

### 2. Buscar Horários Disponíveis
**GET** `/appointments/available-slots?date=2025-12-27`

Retorna todos os horários disponíveis para uma data específica.

**Query Parameters:**
- `date` (obrigatório): Data no formato YYYY-MM-DD

**Response:** `200 OK`
```json
["09:00", "09:30", "10:00", "10:30", "11:00", ...]
```

**Regras:**
- Horários de 30 em 30 minutos
- Das 09:00 às 18:00
- Exclui horários já agendados
- Retorna vazio `[]` se o dia estiver bloqueado

---

### 3. Listar Todos os Agendamentos
**GET** `/appointments`

Lista todos os agendamentos ou filtra por data.

**Query Parameters (opcional):**
- `date`: Filtrar por data específica (YYYY-MM-DD)

**Response:** `200 OK`
```json
[
  {
    "id": "uuid-1",
    "date": "2025-12-27",
    "startTime": "10:00:00",
    "endTime": "11:00:00",
    "service": {...},
    "userName": "Maria Silva",
    "userPhone": "5511999999999",
    "reminderSent": false
  }
]
```

---

### 4. Buscar Agendamentos Futuros por Telefone
**GET** `/appointments/future?userPhone=5511999999999`

Lista agendamentos futuros de um cliente específico.

**Query Parameters:**
- `userPhone`: Número de telefone do cliente

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "date": "2025-12-28",
    "startTime": "14:00:00",
    ...
  }
]
```

---

### 5. Buscar Agendamentos Passados por Telefone
**GET** `/appointments/past?userPhone=5511999999999`

Lista histórico de agendamentos de um cliente.

**Response:** `200 OK` (ordenado do mais recente)

---

### 6. Buscar Agendamento por ID
**GET** `/appointments/{appointmentId}`

Retorna detalhes de um agendamento específico.

---

### 7. Cancelar Agendamento
**DELETE** `/appointments/{appointmentId}`

Cancela um agendamento e libera o horário.

**Response:** `204 No Content`

---

## 🔧 Serviços (Services)

### 1. Criar Serviço
**POST** `/services`

Cadastra um novo serviço no sistema.

**Request Body:**
```json
{
  "name": "Design de Sobrancelhas",
  "duration": 60,
  "price": 80.00
}
```

**Response:** `201 Created`

---

### 2. Listar Todos os Serviços
**GET** `/services`

Retorna lista de todos os serviços disponíveis.

**Response:** `200 OK`
```json
[
  {
    "id": "uuid-1",
    "name": "Design de Sobrancelhas",
    "duration": 60,
    "price": 80.00
  },
  {
    "id": "uuid-2",
    "name": "Alongamento de Cílios",
    "duration": 90,
    "price": 120.00
  }
]
```

---

### 3. Buscar Serviço por ID
**GET** `/services/{id}`

Retorna detalhes de um serviço específico.

---

### 4. Atualizar Serviço
**PUT** `/services/{id}`

Atualiza nome, duração ou preço de um serviço.

**Request Body:**
```json
{
  "name": "Design de Sobrancelhas Premium",
  "duration": 75,
  "price": 100.00
}
```

**Response:** `200 OK`

---

### 5. Deletar Serviço
**DELETE** `/services/{id}`

Remove um serviço do sistema.

**Response:** `204 No Content`

---

## 🚫 Dias Bloqueados (Blocked Days)

### 1. Bloquear Data Específica
**POST** `/blocked-days/specific`

Bloqueia uma data específica (feriados, eventos especiais).

**Request Body:**
```json
{
  "date": "2025-12-25",
  "reason": "Natal"
}
```

**Response:** `201 Created`

---

### 2. Bloquear Dia da Semana Recorrente
**POST** `/blocked-days/recurring`

Bloqueia um dia da semana permanentemente (ex: todo domingo).

**Request Body:**
```json
{
  "dayOfWeek": "SUNDAY",
  "reason": "Folga semanal"
}
```

**Dias da semana válidos:**
- `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

**Response:** `201 Created`

---

### 3. Listar Todos os Bloqueios
**GET** `/blocked-days`

Lista todos os dias bloqueados (específicos e recorrentes).

---

### 4. Listar Datas Específicas Bloqueadas
**GET** `/blocked-days/specific`

Lista apenas bloqueios de datas específicas.

---

### 5. Listar Dias Recorrentes Bloqueados
**GET** `/blocked-days/recurring`

Lista apenas bloqueios recorrentes (dias da semana).

---

### 6. Buscar Datas Disponíveis
**GET** `/blocked-days/available?startDate=2025-12-01&endDate=2025-12-31`

Retorna lista de datas **não bloqueadas** em um período.

**Query Parameters:**
- `startDate`: Data inicial (YYYY-MM-DD)
- `endDate`: Data final (YYYY-MM-DD)

**Response:** `200 OK`
```json
["2025-12-01", "2025-12-02", "2025-12-03", ...]
```

**Uso:** Exibir calendário com apenas dias disponíveis clicáveis.

---

### 7. Remover Bloqueio
**DELETE** `/blocked-days/{id}`

Remove um bloqueio (libera o dia).

**Response:** `204 No Content`

---

## 🗄 Entidades do Banco de Dados

### 1. AppointmentsEntity
**Tabela:** `tb_appointments`

Armazena agendamentos de clientes.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| date | LocalDate | Data do agendamento |
| startTime | LocalTime | Horário de início |
| endTime | LocalTime | Horário de término (calculado) |
| service | ServicesEntity | Serviço agendado (FK) |
| userName | String | Nome do cliente |
| userPhone | String | Telefone do cliente (com DDI 55) |
| reminderSent | boolean | Se lembrete foi enviado |

**Relacionamentos:**
- `@ManyToOne` com `ServicesEntity`

---

### 2. ServicesEntity
**Tabela:** `tb_services`

Armazena serviços oferecidos pelo salão.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| name | String | Nome do serviço |
| duration | Integer | Duração em minutos |
| price | Double | Preço do serviço |

---

### 3. BlockedDayEntity
**Tabela:** `tb_blocked_days`

Armazena dias bloqueados (feriados, folgas).

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| specificDate | LocalDate | Data específica bloqueada (ou null) |
| dayOfWeek | DayOfWeek | Dia da semana (ou null) |
| reason | String | Motivo do bloqueio |
| recurring | boolean | Se é bloqueio recorrente |

**Lógica:**
- `recurring = false`: Bloqueio de data específica
- `recurring = true`: Bloqueio de dia da semana recorrente

---

## ⚙️ Serviços e Funções

### AppointmentsService

#### `getAvailableTimeSlots(LocalDate date)`
**Descrição:** Retorna horários disponíveis para uma data.

**Lógica:**
1. Verifica se a data está bloqueada → retorna `[]`
2. Busca agendamentos existentes na data
3. Gera slots de 30 em 30 minutos (09:00 - 18:00)
4. Remove slots ocupados
5. Retorna lista de horários disponíveis

**Parâmetros:**
- `date`: Data para verificar

**Retorno:** `List<LocalTime>`

---

#### `createAppointment(...)`
**Descrição:** Cria um novo agendamento com validações completas.

**Fluxo:**
1. Valida se a data está bloqueada
2. Busca o serviço no banco
3. Calcula `endTime = startTime + duration`
4. Valida horário de funcionamento
5. Valida conflitos com outros agendamentos
6. Cria e salva o agendamento
7. **Envia confirmação via WhatsApp**

**Parâmetros:**
- `serviceId`: ID do serviço
- `date`: Data do agendamento
- `startTime`: Horário de início
- `userName`: Nome do cliente
- `userPhone`: Telefone (formato: 5511999999999)

**Retorno:** `AppointmentsEntity`

**Validações:**
- Data não bloqueada
- Horário entre 09:00 e 18:00
- Sem conflitos com outros agendamentos
- Serviço existe

---

#### `getFutureAppointmentsByPhone(String userPhone)`
**Descrição:** Busca agendamentos futuros de um cliente.

**Lógica:**
- Filtra agendamentos com `date >= hoje`
- Ordena por data e hora ascendente

---

#### `getPastAppointmentsByPhone(String userPhone)`
**Descrição:** Busca histórico de agendamentos de um cliente.

**Lógica:**
- Filtra agendamentos com `date < hoje`
- Ordena por data decrescente (mais recente primeiro)

---

#### `cancelAppointment(UUID appointmentId)`
**Descrição:** Cancela um agendamento e libera o horário.

---

### BlockedDayService

#### `isDateBlocked(LocalDate date)`
**Descrição:** Verifica se uma data está bloqueada.

**Lógica:**
1. Verifica bloqueio de data específica
2. Verifica bloqueio recorrente do dia da semana
3. Retorna `true` se bloqueada, `false` caso contrário

---

#### `blockSpecificDate(LocalDate date, String reason)`
**Descrição:** Bloqueia uma data específica.

**Validações:**
- Data não pode já estar bloqueada

---

#### `blockRecurringDayOfWeek(DayOfWeek dayOfWeek, String reason)`
**Descrição:** Bloqueia um dia da semana permanentemente.

**Exemplo:** Bloquear todos os domingos

---

#### `getAvailableDates(LocalDate startDate, LocalDate endDate)`
**Descrição:** Retorna datas disponíveis em um período.

**Lógica:**
- Itera por cada dia do período
- Verifica se está bloqueado
- Adiciona à lista apenas dias disponíveis

**Uso:** Exibir calendário no frontend

---

### ServicesService

#### `saveService(ServicesEntity entity)`
**Descrição:** Cria ou atualiza um serviço.

---

#### `findAll()`
**Descrição:** Lista todos os serviços.

---

#### `updateService(UUID id, ServicesEntity updatedService)`
**Descrição:** Atualiza nome, duração e preço de um serviço.

---

#### `deleteService(UUID id)`
**Descrição:** Remove um serviço do sistema.

---

### WhatsappService

#### `enviarAgendamento(Whats dto)`
**Descrição:** Envia confirmação de agendamento via WhatsApp.

**Endpoint chamado:** `POST http://localhost:3001/whatsapp/agendamento`

**Payload enviado:**
```json
{
  "telefone": "5511999999999",
  "nome": "Maria Silva",
  "data": "27/12/2025",
  "hora": "14:30",
  "servico": "Design de Sobrancelhas"
}
```

**Processamento:**
- Remove "+" do telefone se presente
- Formata data como dd/MM/yyyy
- Formata hora como HH:mm

---

#### `enviarLembrete(AppointmentsEntity appointment)`
**Descrição:** Envia lembrete 2 horas antes do agendamento.

**Endpoint chamado:** `POST http://localhost:3001/whatsapp/lembrete`

**Payload:** Mesmo formato do agendamento

---

## 📲 Sistema de Automação WhatsApp

### Arquitetura da Integração

```
┌─────────────────────────────────────────────────────────┐
│         Spring Boot Backend (Java)                      │
│                                                          │
│  WhatsappService                                        │
│    ├── enviarAgendamento()                              │
│    └── enviarLembrete()                                 │
│         │                                                │
│         │ HTTP POST                                      │
│         ▼                                                │
└─────────────────────────────────────────────────────────┘
            │
            │ RestTemplate
            ▼
┌─────────────────────────────────────────────────────────┐
│    WhatsApp API (Node.js - Separado)                    │
│    http://localhost:3001                                 │
│                                                          │
│  Endpoints:                                             │
│    POST /whatsapp/agendamento                           │
│    POST /whatsapp/lembrete                              │
│         │                                                │
│         │ Selenium WebDriver                             │
│         ▼                                                │
└─────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────┐
│              WhatsApp Web                                │
│         (web.whatsapp.com)                              │
└─────────────────────────────────────────────────────────┘
```

### Funcionalidades do Bot WhatsApp

#### 1. Confirmação de Agendamento (Imediata)
**Quando:** Logo após o cliente criar um agendamento

**Mensagem enviada:**
```
✅ Olá, Maria Silva!

Seu agendamento foi confirmado! 🎉

📅 Data: 27/12/2025
🕐 Horário: 14:30
💅 Serviço: Design de Sobrancelhas

Nos vemos em breve! 💖
```

**Formato do payload:**
```json
{
  "telefone": "5511999999999",
  "nome": "Maria Silva",
  "data": "27/12/2025",
  "hora": "14:30",
  "servico": "Design de Sobrancelhas"
}
```

---

#### 2. Lembrete Automático (2 Horas Antes)
**Quando:** Exatamente 2 horas antes do horário agendado

**Mensagem enviada:**
```
⏰ Lembrete de Agendamento

Olá, Maria Silva!

Seu horário está chegando! ⏰

📅 Data: 27/12/2025
🕐 Horário: 14:30
💅 Serviço: Design de Sobrancelhas

Até já! 💖
```

**Funcionamento:**
- Scheduler verifica agendamentos a cada 6 segundos
- Busca agendamentos nas próximas 2 horas
- Filtra apenas os que ainda não receberam lembrete (`reminderSent = false`)
- Envia mensagem via WhatsApp
- Marca `reminderSent = true` para não enviar novamente

---

### Configuração do WhatsApp

O sistema **depende de um serviço Node.js separado** que gerencia a conexão com WhatsApp Web usando Selenium.

**Requisitos:**
- Serviço Node.js rodando em `http://localhost:3001`
- WhatsApp Web conectado (QR Code escaneado)
- Sessão salva em `whatsapp-session/`

**Formato do telefone:**
- **Correto:** `5511999999999` (DDI + DDD + número)
- **Incorreto:** `+5511999999999` (o sistema remove o "+" automaticamente)

---

## ⏰ Scheduler e Lembretes Automáticos

### AppointmentReminderScheduler

**Classe:** `AppointmentReminderScheduler`

**Anotação:** `@Scheduled(fixedRate = 6000)` - Executa a cada 6 segundos

#### Fluxo de Execução

```java
@Scheduled(fixedRate = 6000)
@Transactional
public void sendReminders() {
    // 1. Define intervalo de 2 horas
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime limit = now.plusHours(2);
    
    // 2. Busca agendamentos nas próximas 2 horas que ainda não receberam lembrete
    List<AppointmentsEntity> appointments = 
        appointmentsRepository.findAppointmentsToRemind(
            now.toLocalDate(),
            now.toLocalTime(),
            limit.toLocalDate(),
            limit.toLocalTime()
        );
    
    // 3. Para cada agendamento encontrado
    for (AppointmentsEntity appointment : appointments) {
        // Envia lembrete via WhatsApp
        whatsappService.enviarLembrete(appointment);
        
        // Marca como enviado
        appointment.setReminderSent(true);
    }
}
```

#### Query do Repository

```java
@Query("""
SELECT a FROM AppointmentsEntity a
WHERE a.reminderSent = false
  AND (a.date > :nowDate OR (a.date = :nowDate AND a.startTime >= :nowTime))
  AND (a.date < :limitDate OR (a.date = :limitDate AND a.startTime <= :limitTime))
""")
List<AppointmentsEntity> findAppointmentsToRemind(
    @Param("nowDate") LocalDate nowDate,
    @Param("nowTime") LocalTime nowTime,
    @Param("limitDate") LocalDate limitDate,
    @Param("limitTime") LocalTime limitTime
);
```

#### Características
- ✅ **Automático:** Não requer intervenção manual
- ✅ **Transacional:** Garante consistência dos dados
- ✅ **Idempotente:** Não envia o mesmo lembrete duas vezes
- ✅ **Preciso:** Envia exatamente 2 horas antes
- ✅ **Resiliente:** Continua executando mesmo se houver erro em um envio

---

## 📜 Regras de Negócio

### Horários de Funcionamento
- **Início:** 09:00
- **Fim:** 18:00
- **Intervalo:** Slots de 30 em 30 minutos
- **Horário de término pode ultrapassar 18:00** (validação removida)

### Agendamentos
1. ✅ Cliente pode agendar apenas em datas futuras ou hoje
2. ✅ Horário deve estar disponível (não ocupado)
3. ✅ Data não pode estar bloqueada
4. ✅ Serviço deve existir e estar ativo
5. ✅ Duração do serviço é considerada para calcular horário de término
6. ✅ Confirmação via WhatsApp é enviada imediatamente
7. ✅ Lembrete é enviado 2 horas antes automaticamente

### Dias Bloqueados
1. ✅ Podem ser específicos (ex: 25/12/2025 - Natal)
2. ✅ Ou recorrentes (ex: todo domingo)
3. ✅ Dias bloqueados não aparecem como disponíveis
4. ✅ Tentativa de agendar em dia bloqueado retorna erro
5. ✅ Horários disponíveis retornam `[]` em dias bloqueados

### Serviços
1. ✅ Nome, duração e preço são obrigatórios
2. ✅ Duração define o slot de tempo ocupado
3. ✅ Podem ser editados a qualquer momento
4. ✅ Deletar serviço pode causar erro se houver agendamentos associados

### WhatsApp
1. ✅ Telefone deve estar no formato: `5511999999999` (DDI + DDD + número)
2. ✅ Sistema remove "+" automaticamente se presente
3. ✅ Mensagens são enviadas via API Node.js externa
4. ✅ Erros no envio não impedem a criação do agendamento

---

## 🚀 Configuração e Execução

### Pré-requisitos
- **Java 21** instalado
- **Maven** instalado
- **Node.js** (para API WhatsApp - separado)
- **WhatsApp Web** conectado

### 1. Clonar o Repositório
```bash
git clone <url-do-repositorio>
cd lash-salao-kc-back
```

### 2. Configurar Banco de Dados

**Desenvolvimento (H2 - Padrão):**
Já configurado em `application.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:agendamento
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**Console H2:**
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:agendamento`
- Username: `sa`
- Password: (vazio)

**Produção (PostgreSQL):**
Altere o `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agendamento
spring.datasource.username=postgres
spring.datasource.password=sua-senha
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 3. Instalar Dependências
```bash
mvn clean install
```

### 4. Executar a Aplicação
```bash
mvn spring-boot:run
```

**Ou com Maven Wrapper:**
```bash
./mvnw spring-boot:run    # Linux/Mac
.\mvnw.cmd spring-boot:run  # Windows
```

### 5. Verificar Inicialização
A aplicação estará disponível em:
```
http://localhost:8080
```

**Endpoints de teste:**
```bash
# Listar serviços
curl http://localhost:8080/services

# Console H2
http://localhost:8080/h2-console
```

### 6. Configurar API WhatsApp (Separado)

**⚠️ Importante:** O sistema depende de um serviço Node.js separado para WhatsApp.

**Configuração necessária:**
1. Inicie o serviço Node.js em `http://localhost:3001`
2. Implemente os endpoints:
   - `POST /whatsapp/agendamento`
   - `POST /whatsapp/lembrete`
3. Configure Selenium WebDriver para WhatsApp Web
4. Escaneie QR Code para conectar
5. Sessão será salva em `whatsapp-session/`

**Exemplo de estrutura do serviço Node.js:**
```javascript
const express = require('express');
const app = express();

app.post('/whatsapp/agendamento', (req, res) => {
  const { telefone, nome, data, hora, servico } = req.body;
  // Lógica para enviar mensagem via Selenium/Puppeteer
  res.send('OK');
});

app.post('/whatsapp/lembrete', (req, res) => {
  const { telefone, nome, data, hora, servico } = req.body;
  // Lógica para enviar lembrete
  res.send('OK');
});

app.listen(3001);
```

---

## 🔗 Integrações

### 1. API REST com Frontend
O backend expõe endpoints REST que podem ser consumidos por qualquer frontend (React, Angular, Vue, etc).

**CORS configurado:**
```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
```

### 2. WhatsApp Web (via Node.js)
- Comunicação via HTTP REST
- Formato JSON padronizado
- Timeout configurável
- Retry em caso de falha (implementar no serviço Node)

### 3. H2 Console (Desenvolvimento)
- Acesso via navegador
- Permite executar queries SQL
- Visualizar estrutura das tabelas
- Útil para debug e testes

---

## 📊 Estrutura de Pastas

```
lash-salao-kc-back/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── lash_salao_kc/
│   │   │       └── agendamento_back/
│   │   │           ├── AgendamentoBackApplication.java
│   │   │           ├── config/
│   │   │           │   └── CorsConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── AppointmentsController.java
│   │   │           │   ├── ServicesController.java
│   │   │           │   └── BlockedDayController.java
│   │   │           ├── domain/
│   │   │           │   ├── dto/
│   │   │           │   │   ├── CreateAppointmentRequest.java
│   │   │           │   │   ├── BlockSpecificDateRequest.java
│   │   │           │   │   ├── BlockRecurringDayRequest.java
│   │   │           │   │   └── Whats.java
│   │   │           │   └── entity/
│   │   │           │       ├── AppointmentsEntity.java
│   │   │           │       ├── ServicesEntity.java
│   │   │           │       ├── BlockedDayEntity.java
│   │   │           │       ├── UserEntity.java
│   │   │           │       └── AppointmentReminderScheduler.java
│   │   │           ├── repository/
│   │   │           │   ├── AppoitmentsRepository.java
│   │   │           │   ├── ServicesRepository.java
│   │   │           │   └── BlockedDayRepository.java
│   │   │           └── service/
│   │   │               ├── AppointmentsService.java
│   │   │               ├── ServicesService.java
│   │   │               ├── BlockedDayService.java
│   │   │               └── WhatsappSerivce.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
├── target/
├── whatsapp-session/
├── pom.xml
└── README.md
```

---

## 🧪 Testes e Validações

### Testar Criar Agendamento
```bash
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": "uuid-do-servico",
    "date": "2025-12-28",
    "startTime": "14:00",
    "userName": "Teste Cliente",
    "userPhone": "5511999999999"
  }'
```

### Testar Horários Disponíveis
```bash
curl "http://localhost:8080/appointments/available-slots?date=2025-12-28"
```

### Testar Bloqueio de Dia
```bash
# Bloquear domingo
curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": "SUNDAY",
    "reason": "Folga semanal"
  }'

# Verificar horários em um domingo
curl "http://localhost:8080/appointments/available-slots?date=2025-12-28"
# Deve retornar: []
```

### Testar Scheduler (Lembretes)
1. Criar agendamento para daqui 1h30min
2. Aguardar o scheduler rodar
3. Verificar se lembrete foi enviado após 2h
4. Verificar no banco se `reminderSent = true`

---

## 📝 Notas Adicionais

### Segurança
⚠️ **Atenção:** Este sistema não possui autenticação implementada.

**Para produção, considere:**
- Implementar Spring Security
- Adicionar JWT para autenticação
- Proteger endpoints administrativos
- Validar permissões por papel (ADMIN, USER)

### Performance
- Banco H2 é em memória - dados são perdidos ao reiniciar
- Para produção, migrar para PostgreSQL ou MySQL
- Considerar cache para horários disponíveis
- Implementar paginação para listas grandes

### Melhorias Futuras
- [ ] Sistema de autenticação
- [ ] Painel administrativo web
- [ ] Notificações por email
- [ ] Pagamento online integrado
- [ ] Histórico de alterações
- [ ] Relatórios e estatísticas
- [ ] Avaliações de clientes
- [ ] Sistema de fidelidade

### Troubleshooting

**Problema:** WhatsApp não envia mensagens
- Verificar se serviço Node.js está rodando em `localhost:3001`
- Verificar se WhatsApp Web está conectado
- Verificar logs do Selenium
- Verificar formato do telefone (deve ser `5511999999999`)

**Problema:** H2 Console não abre
- Verificar se `spring.h2.console.enabled=true`
- Acessar `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:agendamento`

**Problema:** Scheduler não roda
- Verificar se `@EnableScheduling` está na classe principal
- Verificar logs de inicialização
- Verificar se há agendamentos nas próximas 2 horas

---

## 📞 Suporte

Para dúvidas ou problemas:
1. Verificar logs da aplicação
2. Consultar documentação dos endpoints
3. Verificar console H2 para dados do banco
4. Testar endpoints com curl ou Postman

---

## 📄 Licença

Este projeto é de código fechado e proprietário.

---

## ✅ Checklist de Deploy

- [ ] Migrar banco H2 para PostgreSQL/MySQL
- [ ] Implementar autenticação e autorização
- [ ] Configurar variáveis de ambiente
- [ ] Configurar HTTPS
- [ ] Configurar backup do banco
- [ ] Testar todos os endpoints em produção
- [ ] Configurar WhatsApp Web em servidor
- [ ] Implementar monitoramento e logs
- [ ] Configurar alertas de erro
- [ ] Documentar processo de deploy

---

**Versão da Documentação:** 1.0.0  
**Data:** 27/12/2025  
**Autor:** Sistema de Agendamentos Lash Salão KC

