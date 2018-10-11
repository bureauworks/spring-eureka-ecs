package com.bureauworks.springeurekaecs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.regex.Pattern;

/**
 * Adjusts the application ip/hostname according to the host (ECS/fargate, docker).
 *
 * @author Marcelo Cyreno
 * @see org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration
 * https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-developing-auto-configuration.html
 */
@Configuration
@EnableConfigurationProperties
@AutoConfigureBefore({EurekaClientAutoConfiguration.class})
public class EurekaIntanceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(EurekaIntanceConfig.class);

    private final Environment env;

    public EurekaIntanceConfig(Environment env) {
        this.env = env;
    }

    @Bean
    @Primary
    @Profile("fargate")
    public EurekaInstanceConfigBean eurekaInstanceConfigBeanForEcs(final InetUtils inetUtils) {

        final var config = new EurekaInstanceConfigBean(inetUtils);
        config.setIpAddress(getEcsPrivateIp());
        config.setSecurePort(getPortNumber());
        config.setNonSecurePort(getPortNumber());

        return config;

    }

    private String getEcsPrivateIp() {

        final var hostname = env.getProperty("HOSTNAME", "");
        final var pattern = Pattern.compile("ip-(\\d+-\\d+-\\d+-\\d+)\\.ec2\\.internal");
        final var matcher = pattern.matcher(hostname);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid HOSTNAME format: " + hostname);
        }

        final var ip = matcher.group(1).replaceAll("-", ".");

        LOG.info("Container private Ip: " + hostname);

        return ip;

    }

    private int getPortNumber() {
        return Integer.parseInt(env.getProperty("server.port", "8888"));
    }

}
