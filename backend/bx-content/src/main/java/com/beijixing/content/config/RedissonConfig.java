package com.beijixing.content.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%d", host, port);
        
        if (password != null && !password.isEmpty()) {
            config.useSingleServer()
                    .setAddress(address)
                    .setPassword(password)
                    .setDatabase(0);
        } else {
            config.useSingleServer()
                    .setAddress(address)
                    .setDatabase(0);
        }
        
        return Redisson.create(config);
    }
}