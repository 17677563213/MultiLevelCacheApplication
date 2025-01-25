# 多级缓存示例项目

这是一个使用 Spring Boot 实现的多级缓存系统示例项目，展示了如何在分布式环境中实现本地缓存和分布式缓存的一致性。

## 技术栈

- Spring Boot 2.7.0
- Redis (分布式缓存)
- JetCache (本地缓存)
- Caffeine (本地缓存)
- Lombok

## 系统架构

### 缓存层次
1. 本地缓存（一级缓存）
   - JetCache LOCAL
   - Caffeine
2. 分布式缓存（二级缓存）
   - Redis
   - JetCache REMOTE

### 缓存一致性
- 使用 Redis 的发布/订阅机制实现多实例间的缓存同步
- 采用先更新远程缓存，再删除本地缓存的策略
- 使用分布式锁保证并发安全

## 主要组件

### 1. 缓存管理器 (CacheConsistencyManager)
- 负责管理本地缓存和远程缓存的一致性
- 提供缓存的增删改查操作
- 实现缓存更新的发布/订阅机制

### 2. 缓存监听器 (CacheUpdateListener)
- 监听缓存更新消息
- 处理本地缓存的更新和删除操作
- 确保多实例间的缓存一致性

### 3. Redis 配置 (RedisConfig)
- 配置 Redis 连接
- 设置序列化方式
- 配置消息监听容器

### 4. 用户服务示例 (UserService)
- 演示缓存在实际业务中的应用
- 包含用户信息的增删改查操作

## API 接口

### 用户管理接口
```
1. 获取用户信息
GET http://localhost:8081/api/users/{userId}

2. 更新用户信息
POST http://localhost:8081/api/users/{userId}
Body: "Updated user info"

3. 删除用户信息
DELETE http://localhost:8081/api/users/{userId}
```

### 缓存测试接口
```
1. 测试本地缓存
GET http://localhost:8081/api/cache/local/{key}

2. 测试分布式缓存
GET http://localhost:8081/api/cache/remote/{key}

3. 更新缓存
POST http://localhost:8081/api/cache/update/{key}
Body: "New cache value"

4. 删除缓存
DELETE http://localhost:8081/api/cache/{key}
```

## 配置说明

### application.yml
```yaml
server:
  port: 8081  # 服务端口

spring:
  redis:
    host: 127.0.0.1  # Redis服务器地址
    port: 6379       # Redis端口
    timeout: 30000   # 连接超时时间
```

## 运行说明

1. 确保 Redis 服务已启动
```bash
# 检查 Redis 状态
redis-cli ping
```

2. 编译并运行项目
```bash
mvn clean package
mvn spring-boot:run
```

3. 测试多实例场景
- 修改 `application.yml` 中的端口号
- 启动多个实例来测试缓存同步

## 缓存更新流程

1. 更新缓存时：
   - 更新本地缓存
   - 更新远程缓存
   - 发布缓存更新消息

2. 删除缓存时：
   - 删除本地缓存
   - 删除远程缓存
   - 发布缓存删除消息

3. 收到缓存更新消息时：
   - 如果是更新操作，清除本地缓存，后续访问时从远程缓存加载
   - 如果是删除操作，直接清除本地缓存

## 注意事项

1. 缓存一致性
   - 采用最终一致性方案
   - 通过 Redis 发布/订阅机制保证多实例间的缓存同步
   - 使用分布式锁避免并发问题

2. 性能优化
   - 本地缓存提供快速访问
   - 分布式缓存确保数据一致性
   - 采用异步方式处理缓存更新通知

3. 错误处理
   - 提供了完善的异常处理机制
   - 日志记录关键操作和错误信息

## 扩展建议

1. 监控与告警
   - 添加缓存命中率监控
   - 实现缓存容量监控
   - 设置关键指标告警

2. 安全性
   - 添加缓存访问权限控制
   - 实现数据加密存储
   - 防止缓存穿透和雪崩

3. 可维护性
   - 完善日志记录
   - 添加性能指标统计
   - 实现缓存预热机制

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交变更
4. 发起 Pull Request

## 许可证

MIT License
