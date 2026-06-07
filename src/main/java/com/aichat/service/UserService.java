package com.aichat.service;

import com.aichat.entity.User;
import com.aichat.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public User register(String username, String password) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setDailyQuota(20);
        user.setUsedToday(0);
        user.setLastResetDate(LocalDate.now());
        userMapper.insert(user);
        return user;
    }

    public User login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getPassword, password));
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        resetDailyQuotaIfNeeded(user);
        return user;
    }

    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            resetDailyQuotaIfNeeded(user);
        }
        return user;
    }

    public void updateApiKey(Long userId, String apiKey) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setApiKey(apiKey);
        userMapper.updateById(user);
    }

    public boolean canChat(Long userId) {
        User user = getUserById(userId);
        if (user == null) return false;
        return user.getUsedToday() < user.getDailyQuota();
    }

    public void incrementUsage(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setUsedToday(user.getUsedToday() + 1);
            userMapper.updateById(user);
        }
    }

    public int getRemainingQuota(Long userId) {
        User user = getUserById(userId);
        if (user == null) return 0;
        return user.getDailyQuota() - user.getUsedToday();
    }

    private void resetDailyQuotaIfNeeded(User user) {
        if (user.getLastResetDate() == null || !user.getLastResetDate().equals(LocalDate.now())) {
            user.setUsedToday(0);
            user.setLastResetDate(LocalDate.now());
            userMapper.updateById(user);
        }
    }
}