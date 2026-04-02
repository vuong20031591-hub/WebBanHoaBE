package com.florastore.web_ban_hoa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
public class WebBanHoaApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebBanHoaApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer(@Value("${security.cors.allowed-origins}") String allowedOrigins) {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
						.allowedOrigins(allowedOrigins.split(","))
						.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
						.allowCredentials(true)
						.allowedHeaders("*");
			}
		};
	}
}
