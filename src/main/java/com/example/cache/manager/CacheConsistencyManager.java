package com.example.cache.manager;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存一致性管理器
 * 负责管理本地缓存和远程缓存的一致性
 * 实现了缓存的更新、删除和同步功能
 */
@Slf4j
@Component
public class CacheConsistencyManager {

    /**
     * Redis模板
     * 用于发布缓存更新消息
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Redisson客户端
     * 用于分布式锁
     */
    @Autowired
    private RedissonClient redissonClient;

    /**
     * Caffeine缓存管理器
     * 用于管理Caffeine缓存
     */
    @Autowired
    private CacheManager caffeineCacheManager;

    /**
     * 本地缓存实例
     * 使用JetCache的LOCAL类型，用于快速访问
     */
    @CreateCache(name = "localCache", cacheType = CacheType.LOCAL)
    private Cache<String, Object> jetcacheLocal;

    /**
     * 远程缓存实例
     * 使用JetCache的REMOTE类型，用于分布式缓存
     */
    @CreateCache(name = "remoteCache", cacheType = CacheType.REMOTE)
    private Cache<String, Object> jetcacheRemote;

    /**
     * 锁前缀
     * 用于构造分布式锁的键
     */
    private static final String LOCK_PREFIX = "cache:lock:";

    /**
     * 缓存更新主题
     * 用于发布缓存更新消息
     */
    private static final String UPDATE_TOPIC = "cache:update:topic";

    /**
     * 更新缓存并发布更新消息
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    public void updateCache(String key, Object value) {
        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            // 尝试获取锁
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 更新各级缓存
                    updateAllCaches(key, value);
                    // 发布缓存更新消息
                    publishCacheUpdateMessage(key, "update");
                } finally {
                    // 释放锁
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            // 获取锁失败，记录日志并中断线程
            log.error("获取锁失败", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 删除缓存并发布删除消息
     * 
     * @param key 要删除的缓存键
     */
    public void deleteCache(String key) {
        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            // 尝试获取锁
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 清除各级缓存
                    clearAllCaches(key);
                    // 发布缓存删除消息
                    publishCacheUpdateMessage(key, "delete");
                } finally {
                    // 释放锁
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            // 获取锁失败，记录日志并中断线程
            log.error("获取锁失败", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 清除本地缓存
     * 用于其他实例收到缓存更新消息时清除本地缓存
     * 
     * @param key 要清除的缓存键
     */
    public void clearLocalCache(String key) {
        // 清除本地缓存
        jetcacheLocal.remove(key);
    }

    /**
     * 更新所有缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    private void updateAllCaches(String key, Object value) {
        // 更新Redis
        redisTemplate.opsForValue().set(key, value);
        
        // 更新Caffeine缓存
        org.springframework.cache.Cache caffeineCache = caffeineCacheManager.getCache("default");
        if (caffeineCache != null) {
            caffeineCache.put(key, value);
        }
        
        // 更新JetCache本地缓存
        jetcacheLocal.put(key, value);
        // 更新JetCache远程缓存
        jetcacheRemote.put(key, value);
    }

    /**
     * 清除所有缓存
     * 
     * @param key 缓存键
     */
    private void clearAllCaches(String key) {
        // 清除Redis
        redisTemplate.delete(key);
        
        // 清除Caffeine缓存
        org.springframework.cache.Cache caffeineCache = caffeineCacheManager.getCache("default");
        if (caffeineCache != null) {
            caffeineCache.evict(key);
        }
        
        // 清除JetCache本地缓存
        jetcacheLocal.remove(key);
        // 清除JetCache远程缓存
        jetcacheRemote.remove(key);
    }

    /**
     * 发布缓存更新消息到Redis
     * 
     * @param key 缓存键
     * @param operation 操作类型（update或delete）
     */
    private void publishCacheUpdateMessage(String key, String operation) {
        CacheUpdateMessage message = new CacheUpdateMessage(key, operation);
        redisTemplate.convertAndSend(UPDATE_TOPIC, message);
    }

    /**
     * 获取缓存值
     * 
     * @param key 缓存键
     * @param cacheType 缓存类型
     * @return 缓存值
     */
    public Object get(String key, String cacheType) {
        // 先从本地缓存获取
        Object value = jetcacheLocal.get(key);
        if (value != null) {
            return value;
        }

        // 从Caffeine缓存获取
        org.springframework.cache.Cache caffeineCache = caffeineCacheManager.getCache(cacheType);
        if (caffeineCache != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = caffeineCache.get(key);
            if (wrapper != null) {
                return wrapper.get();
            }
        }

        // 从Redis获取
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            // 回填本地缓存
            updateAllCaches(key, value);
        }

        return value;
    }

    /**
     * 清除缓存
     * 
     * @param key 缓存键
     * @param cacheType 缓存类型
     */
    public void evict(String key, String cacheType) {
        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            // 尝试获取锁
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 清除各级缓存
                    clearAllCaches(key);
                    // 发布缓存删除消息
                    publishCacheUpdateMessage(key, "delete");
                } finally {
                    // 释放锁
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            // 获取锁失败，记录日志并中断线程
            log.error("获取锁失败", e);
            Thread.currentThread().interrupt();
        }
    }
}
