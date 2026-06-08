package com.aichat.service;

import com.aichat.entity.User;
import com.aichat.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaResetService {

    private final UserMapper userMapper;

    // 每天凌晨0点执行，重置所有用户的每日配额
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetAllQuotas() {
        log.info("[Quota Reset] 开始重置所有用户每日配额...");
        
        List<User> allUsers = userMapper.selectList(null);
        int count = 0;
        
        for (User user : allUsers) {
            user.setUsedToday(0);
            user.setLastResetDate(LocalDate.now());
            userMapper.updateById(user);
            count++;
        }
        
        log.info("[Quota Reset] 配额重置完成，共重置 {} 个用户", count);
    }
}