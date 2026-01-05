# Quick API Reference - Tenant ID via JSON

## All Endpoints Summary

| Category | Old Endpoint | New Endpoint | Method | Body Example |
|----------|-------------|--------------|--------|--------------|
| **APPOINTMENTS** |
| Create | `POST /appointments` | `POST /appointments` | POST | `{"tenantId":"cliente1","serviceIds":[...],"date":"2026-01-15","startTime":"10:00","userName":"João","userPhone":"5511999999999"}` |
| Available Slots | `GET /appointments/available-slots?date=X` | `POST /appointments/available-slots` | POST | `{"tenantId":"cliente1","date":"2026-01-15"}` |
| Future | `GET /appointments/future?userPhone=X` | `POST /appointments/future` | POST | `{"tenantId":"cliente1","userPhone":"5511999999999"}` |
| Past | `GET /appointments/past?userPhone=X` | `POST /appointments/past` | POST | `{"tenantId":"cliente1","userPhone":"5511999999999"}` |
| List All | `GET /appointments` | `POST /appointments/list` | POST | `{"tenantId":"cliente1","date":"2026-01-15"}` |
| By ID | `GET /appointments/id/{id}` | `POST /appointments/by-id` | POST | `{"tenantId":"cliente1","id":"uuid"}` |
| Cancel | `DELETE /appointments/{id}` | `POST /appointments/cancel` | POST | `{"tenantId":"cliente1","id":"uuid"}` |
| **SERVICES** |
| Create | `POST /services` | `POST /services/create` | POST | `{"tenantId":"cliente1","name":"Extensão","duration":90,"price":150.00}` |
| List All | `GET /services` | `POST /services/list` | POST | `{"tenantId":"cliente1"}` |
| By ID | `GET /services/{id}` | `POST /services/by-id` | POST | `{"tenantId":"cliente1","id":"uuid"}` |
| Update | `PUT /services/{id}` | `POST /services/update` | POST | `{"tenantId":"cliente1","id":"uuid","name":"Nome","duration":60,"price":80.00}` |
| Delete | `DELETE /services/{id}` | `POST /services/delete` | POST | `{"tenantId":"cliente1","id":"uuid"}` |
| **BLOCKED DAYS** |
| Block Specific | `POST /blocked-days/specific` | `POST /blocked-days/specific` | POST | `{"tenantId":"cliente1","date":"2026-12-25","reason":"Natal"}` |
| Block Recurring | `POST /blocked-days/recurring` | `POST /blocked-days/recurring` | POST | `{"tenantId":"cliente1","dayOfWeek":"SUNDAY","reason":"Folga"}` |
| List All | `GET /blocked-days` | `POST /blocked-days/list` | POST | `{"tenantId":"cliente1"}` |
| List Specific | `GET /blocked-days/specific` | `POST /blocked-days/specific/list` | POST | `{"tenantId":"cliente1"}` |
| List Recurring | `GET /blocked-days/recurring` | `POST /blocked-days/recurring/list` | POST | `{"tenantId":"cliente1"}` |
| Available Dates | `GET /blocked-days/available?startDate=X&endDate=Y` | `POST /blocked-days/available` | POST | `{"tenantId":"cliente1","startDate":"2026-01-01","endDate":"2026-01-31"}` |
| Unblock | `DELETE /blocked-days/{id}` | `POST /blocked-days/unblock` | POST | `{"tenantId":"cliente1","id":"uuid"}` |

## Key Changes

✅ **ALL endpoints now require `tenantId` in JSON body**
✅ **Most GET endpoints changed to POST**
✅ **All DELETE endpoints changed to POST**
✅ **Path parameters moved to JSON body**
✅ **Query parameters moved to JSON body**

## Common DTO Fields

### TenantRequest
```json
{
  "tenantId": "cliente1"
}
```

### TenantIdWithId
```json
{
  "tenantId": "cliente1",
  "id": "uuid-string"
}
```

### CreateAppointmentRequest
```json
{
  "tenantId": "cliente1",
  "serviceIds": ["uuid1", "uuid2"],
  "date": "2026-01-15",
  "startTime": "10:00",
  "userName": "João Silva",
  "userPhone": "5511999999999"
}
```

### CreateServiceRequest
```json
{
  "tenantId": "cliente1",
  "name": "Serviço Name",
  "duration": 90,
  "price": 150.00
}
```

### BlockSpecificDateRequest
```json
{
  "tenantId": "cliente1",
  "date": "2026-12-25",
  "reason": "Feriado"
}
```

### BlockRecurringDayRequest
```json
{
  "tenantId": "cliente1",
  "dayOfWeek": "SUNDAY",
  "reason": "Folga semanal"
}
```

Days of week: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

