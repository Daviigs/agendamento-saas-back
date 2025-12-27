package lash_salao_kc.agendamento_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgendamentoBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgendamentoBackApplication.class, args);
	}

}
