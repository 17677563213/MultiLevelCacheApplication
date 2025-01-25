package com.example.cache;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableMethodCache(basePackages = "com.example.cache")
@EnableCreateCacheAnnotation
public class MultiLevelCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiLevelCacheApplication.class, args);
    }
}
