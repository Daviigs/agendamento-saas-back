# 📱 Notificação de Cancelamento via WhatsApp

## 📋 Visão Geral

O sistema agora envia automaticamente uma notificação via WhatsApp quando um agendamento é cancelado.

## 🔄 Fluxo de Cancelamento

```
Cliente cancela agendamento
         ↓
DELETE /appointments/{id}
         ↓
AppointmentsService.cancelAppointment()
         ↓
1. Busca dados do agendamento
         ↓
2. WhatsappService.enviarCancelamento()
         ↓
3. POST http://localhost:3001/whatsapp/cancelamento
         ↓
4. Remove agendamento do banco
         ↓
5. Retorna 204 No Content
```

## 📤 Payload Enviado

Quando um agendamento é cancelado, o sistema envia um POST para a API do WhatsApp com o seguinte formato:

```json
{
  "telefone": "5581999999999",
  "nome": "Maria",
  "data": "25/01/2026",
  "hora": "14:00",
  "servico": "Escova",
  "clienteId": "kc"
}
```

### Campos

| Campo | Tipo | Descrição | Exemplo |
|-------|------|-----------|---------|
| `telefone` | String | Telefone normalizado (sem +) | `"5581999999999"` |
| `nome` | String | Nome do cliente | `"Maria Silva"` |
| `data` | String | Data do agendamento cancelado (dd/MM/yyyy) | `"25/01/2026"` |
| `hora` | String | Hora do agendamento cancelado (HH:mm) | `"14:00"` |
| `servico` | String | Nome(s) do(s) serviço(s) | `"Escova, Manicure"` |
| `clienteId` | String | Identificador do tenant | `"kc"` ou `"mjs"` |

## 🎯 Exemplo de Uso na API

### Requisição

```http
DELETE http://localhost:8080/appointments/550e8400-e29b-41d4-a716-446655440000
X-Tenant-Id: kc
```

### Logs do Sistema

```
INFO  - Notificação de cancelamento enviada para 5581999999999
INFO  - Mensagem de cancelamento enviada com sucesso para Maria Silva
```

## 🛡️ Tratamento de Erros

### Cenário 1: API do WhatsApp Indisponível

Se a API do WhatsApp estiver offline ou não responder:

```
ERROR - Erro ao enviar notificação de cancelamento (prosseguindo com cancelamento): Connection refused
```

**Comportamento**: O agendamento **ainda será cancelado**. A notificação é um recurso adicional, não crítico.

### Cenário 2: Agendamento Não Encontrado

Se o ID do agendamento não existir:

```json
{
  "timestamp": "2026-01-24T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Agendamento não encontrado"
}
```

**Comportamento**: Retorna 404 e nenhuma notificação é enviada.

## 🔧 Configuração da API do WhatsApp

A API do WhatsApp deve estar rodando em `http://localhost:3001` e deve implementar o seguinte endpoint:

### POST /whatsapp/cancelamento

**Request Body**:
```json
{
  "telefone": "5581999999999",
  "nome": "Maria",
  "data": "25/01/2026",
  "hora": "14:00",
  "servico": "Escova",
  "clienteId": "kc"
}
```

**Response**: 200 OK (qualquer resposta é aceita)

### Exemplo de Implementação (Node.js/Express)

```javascript
app.post('/whatsapp/cancelamento', async (req, res) => {
  const { telefone, nome, data, hora, servico, clienteId } = req.body;
  
  const mensagem = `Olá ${nome}! 😔\n\n` +
    `Seu agendamento foi cancelado:\n` +
    `📅 Data: ${data}\n` +
    `🕐 Horário: ${hora}\n` +
    `💅 Serviço: ${servico}\n\n` +
    `Se precisar reagendar, estamos à disposição!`;
  
  await sendWhatsAppMessage(clienteId, telefone, mensagem);
  
  res.status(200).send('OK');
});
```

## 📊 Monitoramento

### Logs a Serem Observados

#### ✅ Sucesso
```
INFO  lash_salao_kc.agendamento_back.service.AppointmentsService - Notificação de cancelamento enviada para 5581999999999
INFO  lash_salao_kc.agendamento_back.service.WhatsappService - Mensagem de cancelamento enviada com sucesso para Maria Silva
```

#### ❌ Erro (não crítico)
```
ERROR lash_salao_kc.agendamento_back.service.WhatsappService - Erro ao enviar mensagem de cancelamento: Connection refused
ERROR lash_salao_kc.agendamento_back.service.AppointmentsService - Erro ao enviar notificação de cancelamento (prosseguindo com cancelamento): Connection refused
```

## 🧪 Testes

### Teste Manual

1. Crie um agendamento:
```bash
curl -X POST http://localhost:8080/appointments \
  -H "X-Tenant-Id: kc" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceIds": ["uuid-do-servico"],
    "date": "2026-01-25",
    "startTime": "14:00",
    "userName": "Maria",
    "userPhone": "+5581999999999"
  }'
```

2. Anote o `appointmentId` retornado

3. Cancele o agendamento:
```bash
curl -X DELETE http://localhost:8080/appointments/{appointmentId} \
  -H "X-Tenant-Id: kc"
```

4. Verifique os logs do backend e da API do WhatsApp

### Verificação

- [ ] Agendamento foi removido do banco de dados
- [ ] Log mostra "Notificação de cancelamento enviada"
- [ ] API do WhatsApp recebeu o POST
- [ ] Mensagem foi enviada para o telefone correto

## 📝 Código Fonte

### WhatsappService.java

```java
public void enviarCancelamento(AppointmentsEntity appointment) {
    String url = WHATSAPP_BASE_URL + CANCELAMENTO_ENDPOINT;
    
    String telefoneNormalizado = normalizarTelefone(appointment.getUserPhone());
    String servicosNomes = concatenarNomesServicos(appointment);
    
    Whats dto = buildCancelamentoDto(appointment, telefoneNormalizado, servicosNomes);
    
    try {
        restTemplate.postForEntity(url, dto, String.class);
        log.info("Mensagem de cancelamento enviada com sucesso para {}", appointment.getUserName());
    } catch (Exception e) {
        log.error("Erro ao enviar mensagem de cancelamento: {}", e.getMessage());
        throw e;
    }
}
```

### AppointmentsService.java

```java
@Transactional
public void cancelAppointment(UUID appointmentId) {
    AppointmentsEntity appointment = getAppointmentById(appointmentId);
    
    // Envia notificação de cancelamento via WhatsApp
    try {
        whatsAppService.enviarCancelamento(appointment);
        log.info("Notificação de cancelamento enviada para {}", appointment.getUserPhone());
    } catch (Exception e) {
        log.error("Erro ao enviar notificação de cancelamento (prosseguindo com cancelamento): {}", e.getMessage());
    }
    
    appointmentsRepository.delete(appointment);
}
```

## 🚀 Endpoints WhatsApp Disponíveis

| Endpoint | Descrição | Quando é Chamado |
|----------|-----------|------------------|
| `/whatsapp/agendamento` | Confirmação de novo agendamento | Ao criar agendamento |
| `/whatsapp/lembrete` | Lembrete antes do horário | 2 horas antes (scheduler) |
| `/whatsapp/cancelamento` | Notificação de cancelamento | Ao cancelar agendamento |

## 📞 Suporte

Para problemas com a integração do WhatsApp:

1. Verifique se a API está rodando: `http://localhost:3001/health`
2. Verifique os logs do backend: `tail -f logs/application.log`
3. Verifique os logs da API WhatsApp
4. Teste o endpoint diretamente:

```bash
curl -X POST http://localhost:3001/whatsapp/cancelamento \
  -H "Content-Type: application/json" \
  -d '{
    "telefone": "5581999999999",
    "nome": "Teste",
    "data": "25/01/2026",
    "hora": "14:00",
    "servico": "Teste",
    "clienteId": "kc"
  }'
```

---

**Versão**: 1.0.0  
**Data**: 24/01/2026  
**Autor**: Sistema de Agendamento - Backend

