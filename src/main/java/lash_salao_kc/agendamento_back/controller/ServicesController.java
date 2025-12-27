package lash_salao_kc.agendamento_back.controller;

import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.service.ServicesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServicesController {
    private final ServicesService servicesService;

    /**
     * POST /services
     * Cria um novo serviço
     */
    @PostMapping
    public ResponseEntity<ServicesEntity> createService(@RequestBody ServicesEntity entity) {
        ServicesEntity saved = servicesService.saveService(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /services
     * Lista todos os serviços
     */
    @GetMapping
    public ResponseEntity<List<ServicesEntity>> getAllServices() {
        List<ServicesEntity> services = servicesService.findAll();
        return ResponseEntity.ok(services);
    }

    /**
     * GET /services/{id}
     * Busca um serviço por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServicesEntity> getServiceById(@PathVariable String id) {
        ServicesEntity service = servicesService.findById(UUID.fromString(id));
        return ResponseEntity.ok(service);
    }

    /**
     * PUT /services/{id}
     * Atualiza um serviço existente
     *
     * Request body:
     * {
     *   "name": "Design de Sobrancelhas",
     *   "duration": 60,
     *   "price": 80.00
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServicesEntity> updateService(
            @PathVariable String id,
            @RequestBody ServicesEntity updatedService) {
        ServicesEntity updated = servicesService.updateService(UUID.fromString(id), updatedService);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /services/{id}
     * Deleta um serviço
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {
        servicesService.deleteService(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }
}
