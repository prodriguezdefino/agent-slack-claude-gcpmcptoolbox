package com.example.gcpcloudfunction;

import java.util.Locale;
import java.util.function.Function;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class GcpCloudFunctionApplication {

  private static final Logger logger = LoggerFactory.getLogger(GcpCloudFunctionApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(GcpCloudFunctionApplication.class, args);
  }

  @Bean
  public Function<String, String> function() {
    return value -> value.toUpperCase(Locale.ROOT);
  }
}
