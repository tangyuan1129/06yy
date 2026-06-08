
package com.aichat.service;

import com.aichat.entity.User;
import com.aichat.mapper.UserMapper;
import com.aichat.util.AESUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User register(String username, String password) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setDailyQuota(20);
        user.setUsedToday(0);
        user.setLastResetDate(LocalDate.now());
        userMapper.insert(user);
        return user;
    }

    public User login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }
        System.out.println("登录调试 - 用户名: " + username);
        System.out.println("登录调试 - 输入密码: " + password);
        System.out.println("登录调试 - 数据库密码hash: " + user.getPassword());
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        System.out.println("登录调试 - 密码匹配: " + matches);
        if (!matches) {
            throw new RuntimeException("密码错误");
        }
        resetDailyQuotaIfNeeded(user);
        return userMapper.selectById(user.getId());
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
        // 加密存储 API Key
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            user.setApiKey(AESUtil.encrypt(apiKey.trim()));
            userMapper.updateById(user);
        } else {
            // 使用 UpdateWrapper 强制更新为 null（MyBatis-Plus 的 updateById 会忽略 null 值）
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User> updateWrapper = 
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            updateWrapper.eq("id", userId).set("api_key", null);
            userMapper.update(null, updateWrapper);
        }
    }

    /**
     * 获取解密后的 API Key
     */
    public String getDecryptedApiKey(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getApiKey() == null || user.getApiKey().isEmpty()) {
            return null;
        }
        String key = user.getApiKey();
        // 兼容老数据：如果未加密则直接返回
        if (!AESUtil.isEncrypted(key)) {
            return key;
        }
        return AESUtil.decrypt(key);
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
        if (user.getDailyQuota() == null || user.getDailyQuota() == 0) {
            user.setDailyQuota(20);
        }
        if (user.getUsedToday() == null) {
            user.setUsedToday(0);
        }
        if (user.getLastResetDate() == null || !user.getLastResetDate().equals(LocalDate.now())) {
            user.setUsedToday(0);
            user.setLastResetDate(LocalDate.now());
        }
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void resetPasswordByUsername(String username, String newPassword) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}