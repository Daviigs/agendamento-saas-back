# Exemplos de Uso da API - Horários Disponíveis com Duração de Serviço

## 📌 Endpoint Principal

```
GET /appointments/available-slots
```

## 🔧 Parâmetros

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `professionalId` | UUID | Sim | ID do profissional |
| `date` | LocalDate (ISO) | Sim | Data para consulta (formato: YYYY-MM-DD) |
| `serviceIds` | List<UUID> | Não | IDs dos serviços selecionados |

## 📋 Exemplos de Requisição

### Exemplo 1: Consultando Horários SEM Serviços (Modo Legado)

```http
GET /appointments/available-slots?professionalId=550e8400-e29b-41d4-a716-446655440000&date=2026-02-15
```

**Response:**
```json
[
  "09:00:00",
  "09:30:00",
  "10:00:00",
  "10:30:00",
  "11:00:00",
  "11:30:00",
  "13:00:00",
  "13:30:00",
  "14:00:00"
]
```

**Observação:** Retorna todos os slots não bloqueados, sem considerar duração de serviços.

---

### Exemplo 2: Consultando Horários COM Serviço Único (50 minutos)

**Cenário:**
- Serviço: Design de Sobrancelhas (50 minutos)
- Bloqueio: 12:00 - 13:00

```http
GET /appointments/available-slots?professionalId=550e8400-e29b-41d4-a716-446655440000&date=2026-02-15&serviceIds=123e4567-e89b-12d3-a456-426614174000
```

**Response:**
```json
[
  "09:00:00",
  "09:30:00",
  "10:00:00",
  "10:30:00",
  "11:00:00",
  "13:00:00",
  "13:30:00",
  "14:00:00"
]
```

**Observação:** 
- ❌ `11:30` não aparece porque 11:30 + 50min = 12:20 (ultrapassa bloqueio às 12:00)
- ✅ `11:00` aparece porque 11:00 + 50min = 11:50 (termina antes das 12:00)

---

### Exemplo 3: Consultando Horários COM Múltiplos Serviços (100 minutos)

**Cenário:**
- Serviço 1: Design de Sobrancelhas (30 minutos)
- Serviço 2: Aplicação de Cílios (70 minutos)
- **Total: 100 minutos**
- Bloqueio: 12:00 - 13:00

```http
GET /appointments/available-slots?professionalId=550e8400-e29b-41d4-a716-446655440000&date=2026-02-15&serviceIds=123e4567-e89b-12d3-a456-426614174000&serviceIds=987e6543-e89b-12d3-a456-426614174000
```

**Response:**
```json
[
  "09:00:00",
  "09:30:00",
  "10:00:00",
  "13:00:00",
  "13:30:00"
]
```

**Observação:**
- ❌ `10:30` não aparece porque 10:30 + 100min = 12:10 (ultrapassa bloqueio às 12:00)
- ✅ `10:00` aparece porque 10:00 + 100min = 11:40 (termina antes das 12:00)

---

## 🔍 Exemplo Detalhado de Filtro

### Configuração do Sistema
```
Horário de Trabalho: 09:00 - 18:00
Intervalo de Slots: 30 minutos
Bloqueio: 12:00 - 13:00
Duração do Serviço: 50 minutos
```

### Processo de Filtro

| Slot | Cálculo | Término | Status |
|------|---------|---------|--------|
| 09:00 | 09:00 + 50min | 09:50 | ✅ Disponível |
| 09:30 | 09:30 + 50min | 10:20 | ✅ Disponível |
| 10:00 | 10:00 + 50min | 10:50 | ✅ Disponível |
| 10:30 | 10:30 + 50min | 11:20 | ✅ Disponível |
| 11:00 | 11:00 + 50min | 11:50 | ✅ Disponível |
| 11:30 | 11:30 + 50min | **12:20** | ❌ **Bloqueado** - Ultrapassa 12:00 |
| 12:00 | - | - | ❌ Período bloqueado |
| 12:30 | - | - | ❌ Período bloqueado |
| 13:00 | 13:00 + 50min | 13:50 | ✅ Disponível |
| 13:30 | 13:30 + 50min | 14:20 | ✅ Disponível |

---

## 🧪 Casos de Teste com cURL

### Teste 1: Horários Disponíveis com Serviço

```bash
curl -X GET "http://localhost:8080/appointments/available-slots?professionalId=550e8400-e29b-41d4-a716-446655440000&date=2026-02-15&serviceIds=123e4567-e89b-12d3-a456-426614174000" \
  -H "X-Tenant-Id: cliente1" \
  -H "Accept: application/json"
```

### Teste 2: Horários Disponíveis sem Serviço

```bash
curl -X GET "http://localhost:8080/appointments/available-slots?professionalId=550e8400-e29b-41d4-a716-446655440000&date=2026-02-15" \
  -H "X-Tenant-Id: cliente1" \
  -H "Accept: application/json"
```

### Teste 3: Horários com Múltiplos Serviços

```bash
curl -X GET "http://localhost:8080/appointments/available-slots?professionalId=550e8400-e29b-41d4-a716-446655440000&date=2026-02-15&serviceIds=123e4567-e89b-12d3-a456-426614174000&serviceIds=987e6543-e89b-12d3-a456-426614174000" \
  -H "X-Tenant-Id: cliente1" \
  -H "Accept: application/json"
```

---

## 💻 Exemplo de Integração Frontend (JavaScript)

### Função para Buscar Horários Disponíveis

```javascript
/**
 * Busca horários disponíveis considerando duração dos serviços
 * 
 * @param {string} professionalId - UUID do profissional
 * @param {string} date - Data no formato YYYY-MM-DD
 * @param {string[]} serviceIds - Array de UUIDs dos serviços selecionados
 * @param {string} tenantId - ID do tenant
 * @returns {Promise<string[]>} Array de horários disponíveis (formato HH:mm:ss)
 */
async function getAvailableTimeSlots(professionalId, date, serviceIds = [], tenantId) {
  const baseUrl = 'http://localhost:8080/appointments/available-slots';
  
  // Monta a query string
  const params = new URLSearchParams({
    professionalId: professionalId,
    date: date
  });
  
  // Adiciona cada serviceId (suporta múltiplos valores)
  serviceIds.forEach(serviceId => {
    params.append('serviceIds', serviceId);
  });
  
  const url = `${baseUrl}?${params.toString()}`;
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'X-Tenant-Id': tenantId,
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    }
  });
  
  if (!response.ok) {
    throw new Error(`Erro ao buscar horários: ${response.statusText}`);
  }
  
  return await response.json();
}

// Exemplo de uso:
const professionalId = '550e8400-e29b-41d4-a716-446655440000';
const date = '2026-02-15';
const serviceIds = [
  '123e4567-e89b-12d3-a456-426614174000', // Design de Sobrancelhas
  '987e6543-e89b-12d3-a456-426614174000'  // Aplicação de Cílios
];
const tenantId = 'cliente1';

getAvailableTimeSlots(professionalId, date, serviceIds, tenantId)
  .then(slots => {
    console.log('Horários disponíveis:', slots);
    // ["09:00:00", "09:30:00", "10:00:00", ...]
  })
  .catch(error => {
    console.error('Erro:', error);
  });
```

### Exemplo React com useState

```jsx
import React, { useState, useEffect } from 'react';

function AgendamentoForm() {
  const [professionalId, setProfessionalId] = useState('');
  const [date, setDate] = useState('');
  const [selectedServices, setSelectedServices] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [loading, setLoading] = useState(false);

  // Busca horários quando profissional, data ou serviços mudarem
  useEffect(() => {
    if (professionalId && date && selectedServices.length > 0) {
      fetchAvailableSlots();
    }
  }, [professionalId, date, selectedServices]);

  const fetchAvailableSlots = async () => {
    setLoading(true);
    try {
      const slots = await getAvailableTimeSlots(
        professionalId, 
        date, 
        selectedServices,
        'cliente1'
      );
      setAvailableSlots(slots);
    } catch (error) {
      console.error('Erro ao buscar horários:', error);
      setAvailableSlots([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Seleção de profissional */}
      {/* Seleção de data */}
      {/* Seleção de serviços */}
      
      {loading ? (
        <p>Carregando horários...</p>
      ) : (
        <div>
          <h3>Horários Disponíveis:</h3>
          {availableSlots.map(slot => (
            <button key={slot} onClick={() => handleSelectSlot(slot)}>
              {slot.substring(0, 5)} {/* Mostra apenas HH:mm */}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
```

---

## 📱 Exemplo de Integração Mobile (React Native)

```javascript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

export const appointmentService = {
  /**
   * Busca horários disponíveis
   */
  async getAvailableSlots(professionalId, date, serviceIds, tenantId) {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/appointments/available-slots`,
        {
          params: {
            professionalId,
            date,
            serviceIds // axios automaticamente converte array para múltiplos params
          },
          headers: {
            'X-Tenant-Id': tenantId
          }
        }
      );
      
      return response.data;
    } catch (error) {
      console.error('Erro ao buscar horários:', error);
      throw error;
    }
  }
};

// Uso no componente
const [availableSlots, setAvailableSlots] = useState([]);

const loadAvailableSlots = async () => {
  const slots = await appointmentService.getAvailableSlots(
    professionalId,
    selectedDate,
    selectedServiceIds,
    tenantId
  );
  setAvailableSlots(slots);
};
```

---

## ⚠️ Notas Importantes

1. **Formato de Data:** Sempre use formato ISO (YYYY-MM-DD)
2. **Header Obrigatório:** `X-Tenant-Id` deve ser enviado em todas as requisições
3. **Múltiplos Serviços:** Use `serviceIds` múltiplas vezes na query string
4. **Retrocompatibilidade:** O parâmetro `serviceIds` é opcional

---

**Data:** 31/01/2026  
**Versão da API:** 1.0

