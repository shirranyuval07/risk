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
    public boolean signup(String username, String password)
    {
        if(userRepository.findByUsername(username).isPresent())
            return false;
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        userRepository.save(user);
        return true;
    }
    public User login(String username, String password)
    {
        User user =  userRepository.findByUsername(username).orElse(null);
        if(user != null && user.getPassword().equals(password))
            return user;
        return null;
    }
    public boolean signout(String username,String password)
    {
        if(userRepository.findByUsername(username).isEmpty())
            return false;
        return true;
    }
}