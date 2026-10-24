package com.stockwise.stockwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockwiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockwiseApplication.class, args);
	}

}
