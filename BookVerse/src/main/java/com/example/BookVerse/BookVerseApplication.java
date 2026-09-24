package com.example.BookVerse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableConfigurationProperties  // kích hoạt đọc @ConfigurationProperties (AppProperties)
@EnableCaching                  // kích hoạt Caffeine Cache
public class BookVerseApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookVerseApplication.class, args);
	}

}
