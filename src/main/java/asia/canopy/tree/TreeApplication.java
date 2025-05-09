package asia.canopy.tree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TreeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TreeApplication.class, args);
	}
}
