package com.example.cache.service;

import com.example.cache.manager.CacheConsistencyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务类
 * 提供用户信息的增删改查操作
 * 演示了缓存在实际业务中的应用
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private CacheConsistencyManager cacheManager;

    /**
     * 用户缓存键的前缀
     * 用于构造缓存键
     */
    private static final String USER_KEY_PREFIX = "user:";

    /**
     * 获取用户信息
     * 模拟从数据库获取数据并更新缓存
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    public String getUserInfo(String userId) {
        String key = USER_KEY_PREFIX + userId;
        // 模拟从数据库获取用户信息
        String userInfo = "User info for " + userId + " at " + System.currentTimeMillis();
        // 更新缓存
        cacheManager.updateCache(key, userInfo);
        return userInfo;
    }

    /**
     * 更新用户信息
     * 模拟更新数据库并同步更新缓存
     * 
     * @param userId 用户ID
     * @param userInfo 新的用户信息
     */
    public void updateUserInfo(String userId, String userInfo) {
        String key = USER_KEY_PREFIX + userId;
        // 模拟更新数据库
        // 更新缓存
        cacheManager.updateCache(key, userInfo);
        log.info("User info updated and cache updated for userId: {}", userId);
    }

    /**
     * 删除用户信息
     * 模拟从数据库删除并清除缓存
     * 
     * @param userId 用户ID
     */
    public void deleteUserInfo(String userId) {
        String key = USER_KEY_PREFIX + userId;
        // 模拟从数据库删除
        // 删除缓存
        cacheManager.deleteCache(key);
        log.info("User info deleted and cache evicted for userId: {}", userId);
    }
}
