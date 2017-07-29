package ch.uzh.fabric;

import ch.uzh.fabric.config.Bootstrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FabricApplication {

	@Autowired
	private static Bootstrap bootstrap;

	public static void main(String[] args) {
		SpringApplication.run(FabricApplication.class, args);
	}

}