package com.demo.products;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * This is the main Application class for the Spring Boot application
 * 
 * @author Ananth Kundurthi
 * 
 */
@SpringBootApplication(scanBasePackages={"com.demo.products"})
public class ProductsApp {

	public static void main(String[] args) {
		SpringApplication.run(ProductsApp.class, args);
	}
}
