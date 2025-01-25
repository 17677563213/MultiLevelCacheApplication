package com.example.cache.controller;

import com.example.cache.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 提供用户相关的RESTful API接口
 * 处理用户信息的HTTP请求
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 更新用户信息
     * HTTP POST请求
     * 
     * @param userId 用户ID（路径参数）
     * @param userData 用户数据（请求体）
     * @return 更新结果消息
     */
    @PostMapping("/{userId}")
    public String updateUser(@PathVariable String userId, @RequestBody String userData) {
        userService.updateUserInfo(userId, userData);
        return "User updated successfully";
    }

    /**
     * 获取用户信息
     * HTTP GET请求
     * 
     * @param userId 用户ID（路径参数）
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    public String getUser(@PathVariable String userId) {
        return userService.getUserInfo(userId);
    }

    /**
     * 删除用户信息
     * HTTP DELETE请求
     * 
     * @param userId 用户ID（路径参数）
     * @return 删除结果消息
     */
    @DeleteMapping("/{userId}")
    public String deleteUser(@PathVariable String userId) {
        userService.deleteUserInfo(userId);
        return "User deleted successfully";
    }
}
