package com.adityachandel.booklore.config.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageCacheConfig {

    @Bean
    public FilterRegistrationBean<ImageCachingFilter> imageCachingFilterRegistration() {
        FilterRegistrationBean<ImageCachingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ImageCachingFilter());
        registrationBean.addUrlPatterns("/api/v1/books/*/cover");
        return registrationBean;
    }
}
