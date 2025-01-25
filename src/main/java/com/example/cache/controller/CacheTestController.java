package com.example.cache.controller;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.example.cache.manager.CacheConsistencyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheTestController {

    @Autowired
    private CacheConsistencyManager cacheConsistencyManager;

    // 测试本地缓存
    @Cached(name = "localCache", cacheType = CacheType.LOCAL, expire = 60)
    @GetMapping("/local/{key}")
    public Map<String, Object> getFromLocalCache(@PathVariable String key) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", "Local cache value for " + key);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    // 测试分布式缓存
    @Cached(name = "remoteCache", cacheType = CacheType.REMOTE, expire = 60)
    @GetMapping("/remote/{key}")
    public Map<String, Object> getFromRemoteCache(@PathVariable String key) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", "Remote cache value for " + key);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    // 测试缓存更新
    @PostMapping("/update/{key}")
    public Map<String, Object> updateCache(@PathVariable String key, @RequestBody(required = false) String value) {
        // 更新缓存
        cacheConsistencyManager.updateCache(key, value != null ? value : "Updated value at " + System.currentTimeMillis());
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Cache updated for key: " + key);
        return result;
    }

    // 测试缓存删除
    @DeleteMapping("/{key}")
    public Map<String, Object> deleteCache(@PathVariable String key) {
        // 删除缓存
        cacheConsistencyManager.deleteCache(key);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Cache deleted for key: " + key);
        return result;
    }
}
