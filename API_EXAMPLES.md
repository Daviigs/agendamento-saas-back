# 🚀 Exemplos Práticos de Uso da API

## 📋 Índice
- [Postman](#postman)
- [cURL](#curl)
- [JavaScript/TypeScript](#javascripttypescript)
- [Python](#python)
- [Java](#java)

---

## Postman

### Configuração Inicial
1. Crie uma variável de ambiente chamada `tenantId` com valor `cliente1`
2. Adicione o header `X-Tenant-Id: {{tenantId}}` em todas as requisições

### Collection de Exemplo

```json
{
  "info": {
    "name": "Lash Salão API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Listar Serviços",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "X-Tenant-Id",
            "value": "{{tenantId}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/services",
          "host": ["{{baseUrl}}"],
          "path": ["services"]
        }
      }
    }
  ]
}
```

---

## cURL

### Listar Serviços
```bash
curl -X GET "http://localhost:8080/services" \
  -H "X-Tenant-Id: cliente1"
```

### Criar Serviço
```bash
curl -X POST "http://localhost:8080/services" \
  -H "X-Tenant-Id: cliente1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Extensão de Cílios",
    "duration": 90,
    "price": 150.00
  }'
```

### Buscar Horários Disponíveis
```bash
curl -X GET "http://localhost:8080/appointments/available-slots?date=2026-01-15" \
  -H "X-Tenant-Id: cliente1"
```

### Criar Agendamento
```bash
curl -X POST "http://localhost:8080/appointments" \
  -H "X-Tenant-Id: cliente1" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceIds": ["550e8400-e29b-41d4-a716-446655440000"],
    "date": "2026-01-15",
    "startTime": "10:00",
    "userName": "João Silva",
    "userPhone": "5511999999999"
  }'
```

### Atualizar Serviço
```bash
curl -X PUT "http://localhost:8080/services/550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Tenant-Id: cliente1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Extensão de Cílios Premium",
    "duration": 120,
    "price": 200.00
  }'
```

### Deletar Serviço
```bash
curl -X DELETE "http://localhost:8080/services/550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Tenant-Id: cliente1"
```

### Bloquear Data Específica
```bash
curl -X POST "http://localhost:8080/blocked-days/specific" \
  -H "X-Tenant-Id: cliente1" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-12-25",
    "reason": "Natal"
  }'
```

### Listar Agendamentos por Data
```bash
curl -X GET "http://localhost:8080/appointments?date=2026-01-15" \
  -H "X-Tenant-Id: cliente1"
```

---

## JavaScript/TypeScript

### Configuração com Fetch API

```javascript
// Configuração base
const API_BASE_URL = 'http://localhost:8080';
const TENANT_ID = 'cliente1';

// Helper para adicionar header automaticamente
const apiFetch = async (endpoint, options = {}) => {
  const config = {
    ...options,
    headers: {
      'X-Tenant-Id': TENANT_ID,
      'Content-Type': 'application/json',
      ...options.headers,
    },
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  // No Content (DELETE)
  if (response.status === 204) {
    return null;
  }

  return response.json();
};

// Exemplos de uso
const api = {
  // Services
  async getServices() {
    return apiFetch('/services');
  },

  async createService(data) {
    return apiFetch('/services', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async updateService(id, data) {
    return apiFetch(`/services/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async deleteService(id) {
    return apiFetch(`/services/${id}`, {
      method: 'DELETE',
    });
  },

  // Appointments
  async getAvailableSlots(date) {
    return apiFetch(`/appointments/available-slots?date=${date}`);
  },

  async createAppointment(data) {
    return apiFetch('/appointments', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async getFutureAppointments(userPhone) {
    return apiFetch(`/appointments/future?userPhone=${userPhone}`);
  },

  async cancelAppointment(id) {
    return apiFetch(`/appointments/${id}`, {
      method: 'DELETE',
    });
  },

  // Blocked Days
  async getAvailableDates(startDate, endDate) {
    return apiFetch(`/blocked-days/available?startDate=${startDate}&endDate=${endDate}`);
  },

  async blockSpecificDate(data) {
    return apiFetch('/blocked-days/specific', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

// Uso
(async () => {
  // Listar serviços
  const services = await api.getServices();
  console.log('Serviços:', services);

  // Criar serviço
  const newService = await api.createService({
    name: 'Extensão de Cílios',
    duration: 90,
    price: 150.00,
  });
  console.log('Serviço criado:', newService);

  // Buscar horários disponíveis
  const slots = await api.getAvailableSlots('2026-01-15');
  console.log('Horários disponíveis:', slots);

  // Criar agendamento
  const appointment = await api.createAppointment({
    serviceIds: [newService.id],
    date: '2026-01-15',
    startTime: '10:00',
    userName: 'João Silva',
    userPhone: '5511999999999',
  });
  console.log('Agendamento criado:', appointment);
})();
```

### Configuração com Axios

```javascript
import axios from 'axios';

// Criar instância do axios
const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'X-Tenant-Id': 'cliente1',
  },
});

// Interceptor para tratamento de erros
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('Erro na API:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// Exemplos de uso
const apiMethods = {
  // Services
  getServices: () => api.get('/services'),
  createService: (data) => api.post('/services', data),
  updateService: (id, data) => api.put(`/services/${id}`, data),
  deleteService: (id) => api.delete(`/services/${id}`),

  // Appointments
  getAvailableSlots: (date) => api.get(`/appointments/available-slots?date=${date}`),
  createAppointment: (data) => api.post('/appointments', data),
  getAppointments: (date) => api.get('/appointments', { params: { date } }),
  cancelAppointment: (id) => api.delete(`/appointments/${id}`),

  // Blocked Days
  getAvailableDates: (startDate, endDate) => 
    api.get('/blocked-days/available', { params: { startDate, endDate } }),
  blockSpecificDate: (data) => api.post('/blocked-days/specific', data),
};

// Uso
(async () => {
  try {
    const { data: services } = await apiMethods.getServices();
    console.log('Serviços:', services);

    const { data: slots } = await apiMethods.getAvailableSlots('2026-01-15');
    console.log('Horários:', slots);
  } catch (error) {
    console.error('Erro:', error);
  }
})();
```

---

## Python

### Usando requests

```python
import requests
from datetime import date, time

class LashSalaoAPI:
    def __init__(self, base_url='http://localhost:8080', tenant_id='cliente1'):
        self.base_url = base_url
        self.tenant_id = tenant_id
        self.session = requests.Session()
        self.session.headers.update({'X-Tenant-Id': tenant_id})

    def _request(self, method, endpoint, **kwargs):
        url = f"{self.base_url}{endpoint}"
        response = self.session.request(method, url, **kwargs)
        response.raise_for_status()
        
        if response.status_code == 204:
            return None
        
        return response.json()

    # Services
    def get_services(self):
        return self._request('GET', '/services')

    def create_service(self, name, duration, price):
        return self._request('POST', '/services', json={
            'name': name,
            'duration': duration,
            'price': price
        })

    def update_service(self, service_id, name, duration, price):
        return self._request('PUT', f'/services/{service_id}', json={
            'name': name,
            'duration': duration,
            'price': price
        })

    def delete_service(self, service_id):
        return self._request('DELETE', f'/services/{service_id}')

    # Appointments
    def get_available_slots(self, date):
        return self._request('GET', f'/appointments/available-slots?date={date}')

    def create_appointment(self, service_ids, date, start_time, user_name, user_phone):
        return self._request('POST', '/appointments', json={
            'serviceIds': service_ids,
            'date': date,
            'startTime': start_time,
            'userName': user_name,
            'userPhone': user_phone
        })

    def get_future_appointments(self, user_phone):
        return self._request('GET', f'/appointments/future?userPhone={user_phone}')

    def cancel_appointment(self, appointment_id):
        return self._request('DELETE', f'/appointments/{appointment_id}')

    # Blocked Days
    def get_available_dates(self, start_date, end_date):
        return self._request('GET', 
            f'/blocked-days/available?startDate={start_date}&endDate={end_date}')

    def block_specific_date(self, date, reason):
        return self._request('POST', '/blocked-days/specific', json={
            'date': date,
            'reason': reason
        })

# Uso
if __name__ == '__main__':
    api = LashSalaoAPI()

    # Listar serviços
    services = api.get_services()
    print('Serviços:', services)

    # Criar serviço
    service = api.create_service(
        name='Extensão de Cílios',
        duration=90,
        price=150.00
    )
    print('Serviço criado:', service)

    # Buscar horários disponíveis
    slots = api.get_available_slots('2026-01-15')
    print('Horários disponíveis:', slots)

    # Criar agendamento
    appointment = api.create_appointment(
        service_ids=[service['id']],
        date='2026-01-15',
        start_time='10:00',
        user_name='João Silva',
        user_phone='5511999999999'
    )
    print('Agendamento criado:', appointment)
```

---

## Java

### Usando RestTemplate (Spring)

```java
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

public class LashSalaoApiClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String tenantId;

    public LashSalaoApiClient(String baseUrl, String tenantId) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.tenantId = tenantId;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", tenantId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // Services
    public List<Map<String, Object>> getServices() {
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(
            baseUrl + "/services",
            HttpMethod.GET,
            entity,
            List.class
        );
        return response.getBody();
    }

    public Map<String, Object> createService(String name, int duration, double price) {
        Map<String, Object> request = Map.of(
            "name", name,
            "duration", duration,
            "price", price
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
            baseUrl + "/services",
            HttpMethod.POST,
            entity,
            Map.class
        );
        return response.getBody();
    }

    // Appointments
    public List<String> getAvailableSlots(String date) {
        String url = UriComponentsBuilder
            .fromHttpUrl(baseUrl + "/appointments/available-slots")
            .queryParam("date", date)
            .toUriString();
        
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            List.class
        );
        return response.getBody();
    }

    public Map<String, Object> createAppointment(
        List<String> serviceIds,
        String date,
        String startTime,
        String userName,
        String userPhone
    ) {
        Map<String, Object> request = Map.of(
            "serviceIds", serviceIds,
            "date", date,
            "startTime", startTime,
            "userName", userName,
            "userPhone", userPhone
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
            baseUrl + "/appointments",
            HttpMethod.POST,
            entity,
            Map.class
        );
        return response.getBody();
    }

    // Uso
    public static void main(String[] args) {
        LashSalaoApiClient api = new LashSalaoApiClient("http://localhost:8080", "cliente1");

        // Listar serviços
        List<Map<String, Object>> services = api.getServices();
        System.out.println("Serviços: " + services);

        // Criar serviço
        Map<String, Object> service = api.createService("Extensão de Cílios", 90, 150.00);
        System.out.println("Serviço criado: " + service);

        // Buscar horários disponíveis
        List<String> slots = api.getAvailableSlots("2026-01-15");
        System.out.println("Horários disponíveis: " + slots);
    }
}
```

---

## 💡 Dicas

### 1. **Gerenciamento de Erros**
```javascript
try {
  const services = await api.getServices();
} catch (error) {
  if (error.response) {
    // Erro da API
    console.error('Status:', error.response.status);
    console.error('Dados:', error.response.data);
  } else {
    // Erro de rede
    console.error('Erro de rede:', error.message);
  }
}
```

### 2. **Interceptores para Logging**
```javascript
// Axios
api.interceptors.request.use(request => {
  console.log('Request:', request.method, request.url);
  return request;
});

api.interceptors.response.use(response => {
  console.log('Response:', response.status, response.data);
  return response;
});
```

### 3. **Retry em Caso de Falha**
```javascript
const retryRequest = async (fn, maxRetries = 3) => {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
    }
  }
};

// Uso
const services = await retryRequest(() => api.getServices());
```

---

*Criado em: 05/01/2026*

