package com.api.rizz.portfolio_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * CacheConfig
 *
 * <p>Redis di project ini dipakai sebagai cache layer buat endpoint publik yang sering di-GET tapi
 * jarang berubah (skills/uses/projects/experiences/blogs) -- lihat anotasi @Cacheable di
 * masing-masing Service. Sifatnya tetap OPTIONAL: kalau Redis lagi down/unreachable, request harus
 * tetap jalan normal (cache miss -> langsung query DB) alih-alih ikut gagal cuma gara-gara operasi
 * cache-nya error. Dua hal di bawah ini yang bikin itu kejadian:
 */
@Slf4j
@EnableCaching
@Configuration
public class CacheConfig implements CachingConfigurer {

  // * 1. Default Boot buat RedisCacheManager itu JdkSerializationRedisSerializer, yang mewajibkan
  // * semua value implements Serializable -- DTO response kita (record biasa) gak implements itu,
  // * jadi bakal gagal serialize kalau dipaksa default. Ganti value serializer-nya ke JSON
  // * (GenericJacksonJsonRedisSerializer, versi Jackson 3 -- Spring Boot 4 udah pindah dari
  // * com.fasterxml.jackson ke tools.jackson) biar record/Page/List apa adanya bisa di-cache.
  // * Default typing (embed nama class ke JSON-nya) dibatasi lewat PolymorphicTypeValidator cuma
  // * ke package DTO response kita + tipe koleksi/paging bawaan, biar gak buka celah deserialize
  // * class sembarangan kalau ada yang bisa nulis ke Redis-nya.
  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    PolymorphicTypeValidator typeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.api.rizz.portfolio_api.dto.response")
            .allowIfSubType("java.util")
            .allowIfSubType("org.springframework.data.domain")
            .build();

    GenericJacksonJsonRedisSerializer jsonSerializer =
        GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(typeValidator).build();

    // * builder.cacheDefaults() (getter) ngambil config yang udah kebentuk dari properties
    // * (spring.cache.redis.time-to-live, dst di application.properties), terus cuma serializer
    // * value-nya yang di-override -- biar TTL & setting lain tetap dikontrol dari properties.
    return builder ->
        builder.cacheDefaults(
            builder
                .cacheDefaults()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));
  }

  // * 2. CacheErrorHandler default Spring (SimpleCacheErrorHandler) RETHROW exception dari
  // * get/put/evict/clear -- kalau dibiarin default, Redis down = semua endpoint yang kena
  // * @Cacheable ikut error 500. Di-override supaya error-nya cukup di-log (warning) dan method
  // * aslinya tetap lanjut jalan (fallback ke query DB biasa).
  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
            "Cache GET gagal di '{}' (key={}), fallback ke DB: {}",
            cache.getName(),
            key,
            exception.getMessage());
      }

      @Override
      public void handleCachePutError(
          RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn(
            "Cache PUT gagal di '{}' (key={}): {}", cache.getName(), key, exception.getMessage());
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
            "Cache EVICT gagal di '{}' (key={}): {}", cache.getName(), key, exception.getMessage());
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR gagal di '{}': {}", cache.getName(), exception.getMessage());
      }
    };
  }
}
