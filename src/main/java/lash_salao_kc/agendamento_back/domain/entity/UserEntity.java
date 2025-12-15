package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @NotNull
    private String name;

    @NotNull
    private String number;
}
