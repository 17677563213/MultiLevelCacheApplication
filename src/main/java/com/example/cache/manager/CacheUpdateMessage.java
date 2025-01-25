package com.example.cache.manager;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;

/**
 * 缓存更新消息类
 * 用于在分布式环境中传递缓存更新的信息
 * 实现了Serializable接口以支持序列化
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheUpdateMessage implements Serializable {
    
    /**
     * 序列化版本ID
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 缓存的键
     * 用于标识需要更新或删除的缓存项
     */
    private String key;
    
    /**
     * 操作类型
     * 可能的值：
     * - "update": 表示更新缓存
     * - "delete": 表示删除缓存
     */
    private String operation;  // "update" or "delete"
}
