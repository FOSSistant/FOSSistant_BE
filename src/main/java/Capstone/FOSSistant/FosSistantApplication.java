package Capstone.FOSSistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FosSistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(FosSistantApplication.class, args);
		System.out.println("[FosSistantApplication started]");
	}

}
