package com.api.rizz.portfolio_api.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

@Configuration
/**
 * ClaudinaryConfig
 */
public class CloudinaryConfig {

  @Value("${cloudinary.cloud.name}")
  private String cloudName;
  @Value("${cloudinary.api.key}")
  private String cloudApiKey;
  @Value("${cloudinary.api.secret}")
  private String cloudApiSecret;

  @Bean
  public Cloudinary cloudinary() {
    Map<String, String> config = new HashMap<>();
    config.put("cloud_name", cloudName);
    config.put("api_key", cloudApiSecret);
    config.put("api_secret", cloudApiSecret);

    // * Pake library cloudinary-http5
    return new Cloudinary(config);
  }
}
