package com.example.cache.manager;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
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
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 更新各级缓存
                    updateAllCaches(key, value);
                    // 发布缓存更新消息
                    publishCacheUpdateMessage(key, "update");
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
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
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 清除各级缓存
                    clearAllCaches(key);
                    // 发布缓存删除消息
                    publishCacheUpdateMessage(key, "delete");
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
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
        jetcacheLocal.remove(key);
        log.info("Local cache cleared for key: {}", key);
    }

    /**
     * 更新所有缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    private void updateAllCaches(String key, Object value) {
        // 更新本地缓存
        jetcacheLocal.put(key, value);
        // 更新远程缓存
        jetcacheRemote.put(key, value);
        log.info("All caches updated for key: {}", key);
    }

    /**
     * 清除所有缓存
     * 
     * @param key 缓存键
     */
    private void clearAllCaches(String key) {
        // 清除本地缓存
        jetcacheLocal.remove(key);
        // 清除远程缓存
        jetcacheRemote.remove(key);
        log.info("All caches cleared for key: {}", key);
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
        log.info("Published cache {} message for key: {}", operation, key);
    }

    /**
     * 获取缓存值
     * 先从本地缓存获取，如果没有则从远程缓存获取
     * 
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        // 先从本地缓存获取
        Object value = jetcacheLocal.get(key);
        if (value == null) {
            // 本地缓存未命中，从远程缓存获取
            value = jetcacheRemote.get(key);
            if (value != null) {
                // 将远程缓存的值放入本地缓存
                jetcacheLocal.put(key, value);
                log.info("Cache value fetched from remote and stored in local cache for key: {}", key);
            }
        }
        return value;
    }
}
