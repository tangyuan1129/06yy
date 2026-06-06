package com.aichat.service;

import com.aichat.entity.Character;
import com.aichat.mapper.CharacterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterService {

	private final CharacterMapper characterMapper;

	public List<Character> getAllCharacters() {
		return characterMapper.selectList(
				new LambdaQueryWrapper<Character>()
						.orderByDesc(Character::getCreatedAt)
		);
	}

	public Character getCharacterById(Long id) {
		return characterMapper.selectById(id);
	}

	public Character getOne(LambdaQueryWrapper<Character> queryWrapper) {
		return characterMapper.selectOne(queryWrapper);
	}

	public Character createCharacter(Character character) {
		characterMapper.insert(character);
		// 确保ID被正确回填
		if (character.getId() == null) {
			// 如果ID为空，重新查询一下
			return characterMapper.selectOne(
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Character>()
					.orderByDesc(Character::getCreatedAt)
					.last("LIMIT 1")
			);
		}
		return character;
	}

	public Character updateCharacter(Character character) {
		characterMapper.updateById(character);
		return character;
	}

	public void deleteCharacter(Long id) {
		characterMapper.deleteById(id);
	}
}