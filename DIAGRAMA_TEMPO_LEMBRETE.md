# 🔄 Fluxo de Funcionamento: Tempo de Lembrete Configurável

## 📊 Diagrama Visual do Sistema

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TENANT CONFIGURATION                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐          │
│  │  Tenant A    │    │  Tenant B    │    │  Tenant C    │          │
│  │  "kc"        │    │"salao-bella" │    │"lash-premium"│          │
│  ├──────────────┤    ├──────────────┤    ├──────────────┤          │
│  │ Lembrete:    │    │ Lembrete:    │    │ Lembrete:    │          │
│  │ 120 minutos  │    │ 30 minutos   │    │ 60 minutos   │          │
│  │ (2 horas)    │    │ (30 min)     │    │ (1 hora)     │          │
│  └──────────────┘    └──────────────┘    └──────────────┘          │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    SCHEDULER (Executa a cada minuto)                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  🔔 Iniciando verificação de lembretes...                           │
│  👥 Tenants ativos: [kc, salao-bella, lash-premium]                 │
│                                                                       │
│  Para cada tenant:                                                   │
│  ┌─────────────────────────────────────────────────────┐            │
│  │ 1. Buscar configuração do tenant                    │            │
│  │ 2. Calcular janela de tempo:                        │            │
│  │    now → now + tempoLembreteMinutos                 │            │
│  │ 3. Buscar agendamentos na janela                    │            │
│  │ 4. Enviar lembretes                                 │            │
│  │ 5. Marcar reminderSent = true                       │            │
│  └─────────────────────────────────────────────────────┘            │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         EXEMPLO PRÁTICO                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Horário atual: 13/02/2026 12:00                                    │
│                                                                       │
│  ┌────────────────────────────────────────────────────┐             │
│  │ TENANT A (kc) - 120 minutos                        │             │
│  ├────────────────────────────────────────────────────┤             │
│  │ Janela: 12:00 → 14:00                              │             │
│  │ Agendamento: 14:00 ✅ DENTRO                       │             │
│  │ Ação: ENVIA LEMBRETE                               │             │
│  └────────────────────────────────────────────────────┘             │
│                                                                       │
│  ┌────────────────────────────────────────────────────┐             │
│  │ TENANT B (salao-bella) - 30 minutos                │             │
│  ├────────────────────────────────────────────────────┤             │
│  │ Janela: 12:00 → 12:30                              │             │
│  │ Agendamento: 14:00 ❌ FORA                         │             │
│  │ Ação: NÃO ENVIA                                    │             │
│  └────────────────────────────────────────────────────┘             │
│                                                                       │
│  ┌────────────────────────────────────────────────────┐             │
│  │ TENANT C (lash-premium) - 60 minutos               │             │
│  ├────────────────────────────────────────────────────┤             │
│  │ Janela: 12:00 → 13:00                              │             │
│  │ Agendamento: 14:00 ❌ FORA                         │             │
│  │ Ação: NÃO ENVIA                                    │             │
│  └────────────────────────────────────────────────────┘             │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Fluxo de Cálculo Detalhado

### Fórmula Base
```
horarioEnvio = horarioAgendamento - tempoLembreteMinutos
```

### Janela de Envio
```
janelaInicio = agora
janelaFim = agora + tempoLembreteMinutos
```

### Condição para Envio
```
SE (horarioAgendamento >= janelaInicio) E
   (horarioAgendamento <= janelaFim) E
   (reminderSent == false)
ENTÃO
   enviarLembrete()
   marcarReminderSent(true)
FIM SE
```

---

## 📈 Timeline Visual - Exemplo Tenant com 120 minutos

```
Agendamento: 14:00
Tempo de lembrete: 120 minutos (2 horas)

Timeline:
─────────────────────────────────────────────────────────
11:00    11:30    12:00    12:30    13:00    13:30    14:00
  │        │        │        │        │        │        │
  │        │        │◄──── 120 minutos ───────►│        │
  │        │        │                           │        │
  │        │    [Janela de envio]               │   [Agendamento]
  │        │                                    │
  │        │                                    │
  │    ❌ Muito cedo                        ❌ Muito tarde
  │
❌ Muito cedo

Envio correto: Entre 12:00 e 14:00
```

---

## 🎯 Cenários Práticos

### Cenário 1: Salão de Luxo (2 horas de antecedência)
```
Configuração: tempoLembreteMinutos = 120
Objetivo: Cliente tem tempo de se preparar e se deslocar
Uso típico: Serviços longos, clientes VIP

Exemplo:
- Agendamento: 15:00
- Lembrete enviado: 13:00
- Cliente tem 2h para se preparar
```

### Cenário 2: Barbearia Express (30 minutos)
```
Configuração: tempoLembreteMinutos = 30
Objetivo: Lembrete próximo ao horário (evita esquecimento)
Uso típico: Serviços rápidos, alta rotatividade

Exemplo:
- Agendamento: 15:00
- Lembrete enviado: 14:30
- Cliente sai imediatamente
```

### Cenário 3: Estética Padrão (1 hora)
```
Configuração: tempoLembreteMinutos = 60
Objetivo: Equilíbrio entre preparação e pontualidade
Uso típico: Maioria dos salões

Exemplo:
- Agendamento: 15:00
- Lembrete enviado: 14:00
- Tempo adequado para deslocamento
```

---

## 🔍 Lógica de Decisão do Scheduler

```
┌─────────────────────────────────────────┐
│  Início da execução (a cada minuto)    │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│  Buscar todos os tenants ativos         │
└────────────────┬────────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │ Para cada tenant│
        └────────┬────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ Buscar configuração do tenant:          │
│ - tempoLembreteMinutos                  │
│ - timezone                               │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ Calcular janela:                        │
│ now → now + tempoLembreteMinutos        │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ Buscar agendamentos onde:               │
│ - data/hora dentro da janela            │
│ - reminderSent = false                  │
└────────────────┬────────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │ Agendamentos   │
        │   encontrados? │
        └────┬───────┬───┘
             │       │
           SIM      NÃO
             │       │
             ▼       ▼
    ┌────────────┐  ┌────────────┐
    │Para cada   │  │Próximo     │
    │agendamento:│  │tenant      │
    │            │  └────────────┘
    │1. Enviar   │
    │   WhatsApp │
    │2. Marcar   │
    │   enviado  │
    │3. Log      │
    └──────┬─────┘
           │
           ▼
    ┌────────────┐
    │Próximo     │
    │tenant      │
    └────────────┘
```

---

## 📊 Comparação Antes vs Depois

### ANTES (Sistema Antigo)

```
┌──────────────────────────────────────┐
│   CONFIGURAÇÃO GLOBAL FIXA           │
├──────────────────────────────────────┤
│   Todos os tenants: 2 horas          │
│   Não configurável                   │
│   Código hardcoded                   │
└──────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│   SCHEDULER                          │
├──────────────────────────────────────┤
│   limit = now + 2 hours              │
│   (fixo para todos)                  │
└──────────────────────────────────────┘
```

### DEPOIS (Sistema Novo)

```
┌──────────────────────────────────────┐
│   CONFIGURAÇÃO POR TENANT            │
├──────────────────────────────────────┤
│   Tenant A: 120 minutos              │
│   Tenant B: 30 minutos               │
│   Tenant C: 60 minutos               │
│   Totalmente flexível                │
└──────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│   SCHEDULER                          │
├──────────────────────────────────────┤
│   limit = now + tenant.tempo         │
│   (dinâmico por tenant)              │
└──────────────────────────────────────┘
```

---

## 🎭 Exemplo de Dia Real

### Cenário: Sistema rodando às 12:00 do dia 13/02/2026

```
┌──────────────────────────────────────────────────────────┐
│  TENANT: kc (120 minutos)                                │
├──────────────────────────────────────────────────────────┤
│  Janela: 12:00 → 14:00                                   │
│                                                           │
│  Agendamentos:                                           │
│  ✅ 12:30 - Maria Silva (dentro, envia)                 │
│  ✅ 13:45 - João Santos (dentro, envia)                 │
│  ❌ 11:30 - Ana Costa (já passou)                       │
│  ❌ 14:15 - Pedro Alves (fora da janela)                │
│  ❌ 13:00 - Carlos Lima (reminderSent=true)             │
│                                                           │
│  Resultado: 2 lembretes enviados                         │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  TENANT: salao-bella (30 minutos)                        │
├──────────────────────────────────────────────────────────┤
│  Janela: 12:00 → 12:30                                   │
│                                                           │
│  Agendamentos:                                           │
│  ✅ 12:15 - Juliana Melo (dentro, envia)                │
│  ❌ 12:45 - Roberto Silva (fora da janela)              │
│  ❌ 13:30 - Fernanda Costa (fora da janela)             │
│                                                           │
│  Resultado: 1 lembrete enviado                           │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  TENANT: lash-premium (60 minutos)                       │
├──────────────────────────────────────────────────────────┤
│  Janela: 12:00 → 13:00                                   │
│                                                           │
│  Agendamentos:                                           │
│  ✅ 12:30 - Amanda Souza (dentro, envia)                │
│  ✅ 12:45 - Beatriz Lima (dentro, envia)                │
│  ❌ 13:30 - Camila Rocha (fora da janela)               │
│                                                           │
│  Resultado: 2 lembretes enviados                         │
└──────────────────────────────────────────────────────────┘

🎯 TOTAL GERAL: 5 lembretes enviados às 12:00
```

---

## 🔐 Validações e Proteções

### Camada 1: API (DTO)
```
@Min(1) @Max(1440)
private Integer tempoLembreteMinutos;
```
**Proteção:** Rejeita valores inválidos na entrada

### Camada 2: Database (Constraint)
```sql
CHECK (tempo_lembrete_minutos > 0 
   AND tempo_lembrete_minutos <= 1440)
```
**Proteção:** Impede valores inválidos no banco

### Camada 3: Entity (Default)
```java
private Integer tempoLembreteMinutos = 120;
```
**Proteção:** Garante valor padrão se não configurado

### Camada 4: Scheduler (Lógica)
```java
if (appointment.isReminderSent()) {
    continue; // Não envia duplicado
}
```
**Proteção:** Evita envio duplicado

---

## 📱 Interface (Exemplo de Tela Admin)

```
┌────────────────────────────────────────────────┐
│  Configurações do Tenant: Salão Bella          │
├────────────────────────────────────────────────┤
│                                                 │
│  Nome do Negócio: [Salão Bella              ]  │
│  Email:           [contato@bella.com        ]  │
│  Telefone:        [(11) 99999-9999         ]  │
│  Timezone:        [America/Sao_Paulo ▼]       │
│                                                 │
│  ┌─────────────────────────────────────────┐  │
│  │ ⏰ Tempo de Lembrete                    │  │
│  ├─────────────────────────────────────────┤  │
│  │                                          │  │
│  │ Enviar lembrete [  30  ] minutos antes  │  │
│  │                                          │  │
│  │ Sugestões:                               │  │
│  │ • 30 minutos - Serviços rápidos         │  │
│  │ • 60 minutos - Padrão                   │  │
│  │ • 120 minutos - Serviços longos         │  │
│  │                                          │  │
│  │ Min: 1 minuto | Max: 1440 minutos (24h) │  │
│  └─────────────────────────────────────────┘  │
│                                                 │
│  [Cancelar]                        [Salvar] ✓  │
└────────────────────────────────────────────────┘
```

---

## 🎉 Resultado Final

### Sistema Antes
- ❌ Tempo fixo para todos
- ❌ Não configurável
- ❌ Inflexível

### Sistema Depois
- ✅ Tempo por tenant
- ✅ Totalmente configurável
- ✅ Flexível e extensível
- ✅ Retrocompatível
- ✅ Validado e seguro

---

**Desenvolvido com ❤️ por GitHub Copilot Assistant**

