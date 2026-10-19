package com.findmyfaculty.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("preview")
public class PreviewFilterRegistration {
    @Bean public FilterRegistrationBean<PreviewBearerAuthenticationFilter> previewBearerFilterRegistration(PreviewBearerAuthenticationFilter filter){
        FilterRegistrationBean<PreviewBearerAuthenticationFilter> registration=new FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;
    }
}
