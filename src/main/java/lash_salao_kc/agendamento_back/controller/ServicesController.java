package lash_salao_kc.agendamento_back.controller;

import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.service.ServicesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServicesController {
    private final ServicesService servicesService;

    @PostMapping
    public ResponseEntity<ServicesEntity> createService(@RequestBody ServicesEntity entity) {
        ServicesEntity saved = servicesService.saveService(entity);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<ServicesEntity>> getAllServices() {
        List<ServicesEntity> services = servicesService.findAll();
        return ResponseEntity.ok(services);
    }
}
