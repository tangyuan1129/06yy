package com.aichat.service;

import com.aichat.mapper.SensitiveWordMapper;
import com.aichat.entity.SensitiveWord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    public boolean containsSensitiveWord(String text) {
        List<SensitiveWord> words = sensitiveWordMapper.selectList(null);
        for (SensitiveWord word : words) {
            if (text.contains(word.getWord())) {
                return true;
            }
        }
        return false;
    }

    public void addWord(String word) {
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(word);
        sensitiveWordMapper.insert(sw);
    }
}