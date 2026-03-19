package service;

import Entity.User;
import org.springframework.stereotype.Service;
import repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Spring יזריק לכאן את ה-Repository באופן אוטומטי
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getOrCreateUser(String username) {
        // מחפשים אם המשתמש כבר קיים ב-MySQL
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    // אם לא נמצא - יוצרים משתמש חדש לגמרי
                    User newUser = new User();
                    newUser.setUsername(username);

                    // שמירה ב-Database
                    return userRepository.save(newUser);
                });
    }
}