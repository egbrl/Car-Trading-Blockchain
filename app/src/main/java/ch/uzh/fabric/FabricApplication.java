package ch.uzh.fabric;

import ch.uzh.fabric.config.Bootstrap;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class FabricApplication {

	@Autowired
	private static Bootstrap bootstrap;

	public static void main(String[] args) {
		SpringApplication.run(FabricApplication.class, args);

		try {
			File lock = new File("app.lock");
			FileUtils.touch(lock);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

}