package com.example.cache.listener;

import com.example.cache.manager.CacheConsistencyManager;
import com.example.cache.manager.CacheUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 缓存更新监听器
 * 负责监听Redis的缓存更新消息，并处理本地缓存的更新
 * 实现了MessageListener接口以接收Redis的消息
 */
@Slf4j
@Component
public class CacheUpdateListener implements MessageListener {
    
    @Autowired
    private CacheConsistencyManager cacheConsistencyManager;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 处理接收到的缓存更新消息
     * 
     * @param message Redis消息对象，包含消息内容
     * @param pattern 消息匹配模式（未使用）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 将消息转换为CacheUpdateMessage对象
            String messageBody = new String(message.getBody());
            CacheUpdateMessage cacheMessage = objectMapper.readValue(messageBody, CacheUpdateMessage.class);
            
            log.info("Received cache update message: {}", messageBody);
            
            // 根据操作类型处理缓存
            String key = cacheMessage.getKey();
            String operation = cacheMessage.getOperation();
            
            if ("delete".equals(operation)) {
                // 删除本地缓存
                cacheConsistencyManager.clearLocalCache(key);
                log.info("Local cache cleared for key: {}", key);
            } else if ("update".equals(operation)) {
                // 删除本地缓存，让它在下次访问时从远程缓存重新加载
                cacheConsistencyManager.clearLocalCache(key);
                log.info("Local cache invalidated for key: {} to reload from remote", key);
            }
            
        } catch (Exception e) {
            log.error("Error processing cache update message", e);
        }
    }
}
