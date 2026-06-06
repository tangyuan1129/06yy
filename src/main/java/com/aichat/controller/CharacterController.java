package com.aichat.controller;

import com.aichat.entity.Character;
import com.aichat.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CharacterController {

	private final CharacterService characterService;

	@GetMapping
	public List<Character> getAllCharacters() {
		return characterService.getAllCharacters();
	}

	@GetMapping("/{id}")
	public Character getCharacterById(@PathVariable Long id) {
		return characterService.getCharacterById(id);
	}

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
}