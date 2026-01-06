# 📚 Índice de Documentação - Sistema de Agendamentos

## 🎯 Visão Geral
Sistema de agendamento multi-tenant para salão de beleza com integração WhatsApp.

---

## 📖 Documentação Disponível

### 1. **API_DOCUMENTATION.md** 
📘 **Documentação Completa da API**
- Todos os endpoints disponíveis
- Métodos HTTP (GET, POST, PUT, DELETE)
- Exemplos de requisições e respostas
- Códigos de status
- Formatos de dados
- **Consulte este arquivo para saber COMO usar a API**

### 2. **API_EXAMPLES.md**
💻 **Exemplos Práticos de Implementação**
- Postman
- cURL
- JavaScript/TypeScript (Fetch API e Axios)
- Python (requests)
- Java (RestTemplate)
- **Consulte este arquivo para ver exemplos REAIS de código**

### 3. **CHANGES_SUMMARY.md**
📝 **Resumo das Alterações**
- Mudanças de abordagem (body JSON → header)
- Benefícios das mudanças
- Breaking changes
- Migração de endpoints
- **Consulte este arquivo para entender O QUE mudou**

### 4. **PROJECT_DOCUMENTATION.md**
📚 **Documentação Técnica do Projeto**
- Arquitetura do sistema
- Estrutura de pacotes
- Entidades e relacionamentos
- Serviços e funcionalidades
- **Consulte este arquivo para entender a ARQUITETURA**

### 5. **SERVICES_API.md**
🔧 **Documentação dos Serviços Internos**
- Lógica de negócio
- Métodos dos services
- Regras de validação
- **Consulte este arquivo para entender a LÓGICA INTERNA**

### 6. **MIGRATION_GUIDE.md**
🔄 **Guia de Migração (Antigo)**
- Documentação da versão anterior (tenant ID no body)
- Mantido para referência histórica

---

## 🚀 Guia Rápido de Início

### 1. **Para Desenvolvedores Frontend**
1. Leia **API_DOCUMENTATION.md** para conhecer os endpoints
2. Consulte **API_EXAMPLES.md** para ver exemplos em JavaScript
3. Configure o header `X-Tenant-Id` em todas as requisições

### 2. **Para Desenvolvedores Backend**
1. Leia **PROJECT_DOCUMENTATION.md** para entender a arquitetura
2. Consulte **SERVICES_API.md** para entender os services
3. Veja **CHANGES_SUMMARY.md** para entender as mudanças recentes

### 3. **Para Testadores/QA**
1. Leia **API_DOCUMENTATION.md** para conhecer os endpoints
2. Use **API_EXAMPLES.md** para copiar exemplos de cURL
3. Importe a collection do Postman

### 4. **Para DevOps/Infraestrutura**
1. Configure variável de ambiente para o banco H2
2. Certifique-se que a porta 8080 está disponível
3. Configure o header `X-Tenant-Id` no API Gateway se aplicável

---

## 🔐 Autenticação

**Todas as requisições requerem o header:**
```
X-Tenant-Id: cliente1
```

**Valores válidos de tenant:**
- `cliente1` - Cliente padrão
- Outros conforme cadastrado no sistema

---

## 🛠️ Tecnologias

- **Backend:** Spring Boot 3.x, Java 17+
- **Banco de Dados:** H2 (desenvolvimento), PostgreSQL (produção)
- **Integração:** WhatsApp Business API
- **Arquitetura:** Multi-tenant, REST API

---

## 📊 Recursos Principais

### ✅ Agendamentos (Appointments)
- Criar agendamento com múltiplos serviços
- Consultar horários disponíveis
- Listar agendamentos (futuros, passados, por data)
- Cancelar agendamentos
- Envio automático de WhatsApp

### ✅ Serviços (Services)
- CRUD completo de serviços
- Duração e preço configuráveis
- Isolamento por tenant

### ✅ Dias Bloqueados (Blocked Days)
- Bloquear datas específicas (feriados)
- Bloquear dias da semana recorrentes (folgas)
- Consultar datas disponíveis
- Gerenciar bloqueios

---

## 📈 Endpoints Principais

| Recurso | Endpoint | Método | Descrição |
|---------|----------|--------|-----------|
| **Appointments** |
| Criar | `/appointments` | POST | Novo agendamento |
| Horários | `/appointments/available-slots?date={date}` | GET | Horários disponíveis |
| Listar | `/appointments` | GET | Todos agendamentos |
| Buscar | `/appointments/{id}` | GET | Por ID |
| Cancelar | `/appointments/{id}` | DELETE | Cancelar |
| **Services** |
| Criar | `/services` | POST | Novo serviço |
| Listar | `/services` | GET | Todos serviços |
| Buscar | `/services/{id}` | GET | Por ID |
| Atualizar | `/services/{id}` | PUT | Atualizar |
| Deletar | `/services/{id}` | DELETE | Remover |
| **Blocked Days** |
| Bloquear Data | `/blocked-days/specific` | POST | Data específica |
| Bloquear Dia | `/blocked-days/recurring` | POST | Dia da semana |
| Listar | `/blocked-days` | GET | Todos bloqueios |
| Datas Livres | `/blocked-days/available?start={start}&end={end}` | GET | Período disponível |
| Desbloquear | `/blocked-days/{id}` | DELETE | Remover bloqueio |

---

## 🧪 Como Testar

### Via cURL
```bash
# Listar serviços
curl -X GET http://localhost:8080/services \
  -H "X-Tenant-Id: cliente1"
```

### Via Browser (GET apenas)
```
http://localhost:8080/services
# Adicione extensão ModHeader ou similar para incluir o header X-Tenant-Id
```

### Via Postman
1. Importe a collection (se disponível)
2. Configure variável `tenantId` = `cliente1`
3. Adicione header `X-Tenant-Id: {{tenantId}}` em todas requests

---

## 🐛 Solução de Problemas

### Erro 400 - Bad Request
- Verifique se o JSON está no formato correto
- Verifique se todos os campos obrigatórios foram enviados

### Erro 404 - Not Found
- Verifique se o endpoint está correto
- Verifique se o ID existe no banco de dados

### Erro 500 - Internal Server Error
- Verifique os logs do servidor
- Verifique se o banco de dados está acessível

### Header ausente
- Sempre inclua `X-Tenant-Id` em todas as requisições
- Valor deve ser uma string não vazia

---

## 📞 Suporte

Para dúvidas ou problemas:
1. Consulte esta documentação
2. Verifique os exemplos em **API_EXAMPLES.md**
3. Revise as alterações em **CHANGES_SUMMARY.md**
4. Contate o time de desenvolvimento

---

## 🔄 Atualizações

**Última atualização:** 05/01/2026

**Versão da API:** 2.0
- Tenant ID via header
- Métodos HTTP RESTful
- Documentação completa

**Versão anterior:** 1.0
- Tenant ID via body JSON
- Métodos POST para tudo
- [Ver MIGRATION_GUIDE.md]

---

## 📝 Arquivos do Projeto

```
lash-salao-kc-back/
├── API_DOCUMENTATION.md      ← Documentação completa da API
├── API_EXAMPLES.md            ← Exemplos práticos de código
├── CHANGES_SUMMARY.md         ← Resumo das alterações
├── README_INDEX.md            ← Este arquivo
├── PROJECT_DOCUMENTATION.md   ← Documentação técnica
├── SERVICES_API.md            ← Documentação dos services
├── MIGRATION_GUIDE.md         ← Guia de migração (legado)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── lash_salao_kc/
│   │   │       └── agendamento_back/
│   │   │           ├── controller/
│   │   │           │   ├── AppointmentsController.java
│   │   │           │   ├── ServicesController.java
│   │   │           │   └── BlockedDayController.java
│   │   │           ├── service/
│   │   │           ├── repository/
│   │   │           ├── domain/
│   │   │           └── config/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── pom.xml
└── mvnw
```

---

*Criado em: 05/01/2026*
*Sistema de Agendamentos Multi-Tenant v2.0*

