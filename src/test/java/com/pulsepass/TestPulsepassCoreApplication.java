package com.pulsepass;

import org.springframework.boot.SpringApplication;

public class TestPulsepassCoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(PulsepassCoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
