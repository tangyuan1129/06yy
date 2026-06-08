package com.aichat.controller;

import com.aichat.entity.Character;
import com.aichat.service.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色列表和角色详情接口")
public class CharacterController {

	private final CharacterService characterService;

	@GetMapping
	@Operation(summary = "获取所有角色", description = "获取角色列表")
	public List<Character> getAllCharacters() {
		return characterService.getAllCharacters();
	}

	@GetMapping("/{id}")
	@Operation(summary = "获取角色详情", description = "根据ID获取单个角色信息")
	public Character getCharacterById(@Parameter(description = "角色ID") @PathVariable Long id) {
		return characterService.getCharacterById(id);
	}

	// ⚠️ 写操作已禁用 - 生产环境不应提供角色增删改接口
	// 如需管理角色，请直接操作数据库
/*
	@PostMapping
	public Character createCharacter(@RequestBody Character character) {
		return characterService.createCharacter(character);
	}

	@PutMapping("/{id}")
	public Character updateCharacter(@PathVariable Long id, @RequestBody Character character) {
		character.setId(id);
		return characterService.updateCharacter(character);
	}

	@DeleteMapping("/{id}")
	public void deleteCharacter(@PathVariable Long id) {
		characterService.deleteCharacter(id);
	}
*/
}