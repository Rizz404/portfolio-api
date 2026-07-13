package com.api.rizz.portfolio_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class PortfolioApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortfolioApiApplication.class, args);
	}

}
