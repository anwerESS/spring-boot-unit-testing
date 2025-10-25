package org.example.testwebmvccontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

	private final UserRepository userRepository;

	@Autowired
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	public User getUserById(Long id) {
		return userRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
	}

	public User createUser(User user) {
		if (user.getAge() < 18) {
			throw new IllegalArgumentException("User must be 18 or older");
		}
		return userRepository.save(user);
	}

	public User updateUser(Long id, User userDetails) {
		User user = getUserById(id);
		user.setName(userDetails.getName());
		user.setEmail(userDetails.getEmail());
		user.setAge(userDetails.getAge());
		return userRepository.save(user);
	}

	public void deleteUser(Long id) {
		User user = getUserById(id);
		userRepository.delete(user);
	}
}
