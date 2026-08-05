package com.api.rizz.portfolio_api.config;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * WebConfig
 *
 * <p>Resolve bahasa response dari header HTTP {@code Accept-Language}. Kalau header tidak ada atau
 * bahasanya tidak didukung, fallback ke {@link Locale#ENGLISH}.
 *
 * <p>Bean HARUS bernama persis "localeResolver" karena DispatcherServlet mencarinya berdasarkan
 * nama bean tersebut (lihat DispatcherServlet.initLocaleResolver()).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.ENGLISH);
    resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.forLanguageTag("id")));
    return resolver;
  }
}
