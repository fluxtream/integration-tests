package org.fluxtream.test.integration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by candide on 13/04/15.
 */
@Configuration
@PropertySource("classpath:application.properties")
public class CommonConfiguration {

    @Value("${adminUser.username}")
    public String flxAdminUserUsername;

    @Value("${adminUser.password}")
    public String flxAdminUserPassword;

    @Value("${couchAdminUser.username}")
    public String couchAdminUserUsername;

    @Value("${couchAdminUser.password}")
    public String couchAdminUserPassword;

    @Value("${targetHomeBaseUrl}")
    public Object targetHomeBaseUrl;
}
