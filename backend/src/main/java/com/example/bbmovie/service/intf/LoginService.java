package com.example.bbmovie.service.intf;


import com.example.bbmovie.model.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public interface LoginService {
    @Transactional
    User authenticateUser(String usernameOrEmail, String password);
}
