package com.example.demo.db.service;

import com.example.demo.db.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.demo.db.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean signup(String username, String password) {
        if (userRepository.findByUsername(username).isPresent())
            return false;

        User user = new User();
        user.setUsername(username);
        // Hash the password before saving — plain text never touches the database
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return true;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        // BCrypt handles the comparison — it re-hashes and compares internally
        if (user != null && passwordEncoder.matches(password, user.getPassword()))
            return user;
        return null;
    }

    public boolean signout(String username, String password) {
        return userRepository.findByUsername(username).isPresent();
    }
}