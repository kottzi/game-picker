package io.github.kottzi.gamepicker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GamePickerApplication {

	static void main(String... args) {
		SpringApplication.run(GamePickerApplication.class, args);
	}

}
