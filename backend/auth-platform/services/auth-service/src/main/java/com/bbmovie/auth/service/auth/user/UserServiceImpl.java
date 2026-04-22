package com.bbmovie.auth.service.auth.user;

import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.dto.response.UserResponse;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.utils.DeviceInfoUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.bbmovie.auth.constant.UserErrorMessages.USER_NOT_FOUND_BY_EMAIL;
import static com.bbmovie.auth.service.auth.session.SessionServiceImpl.createUserResponseFromUser;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DeviceInfoUtils deviceInfoUtils;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, DeviceInfoUtils deviceInfoUtils) {
        this.userRepository = userRepository;
        this.deviceInfoUtils = deviceInfoUtils;
    }

    /**
     * Loads and retrieves the authenticated user information based on the provided email.
     *
     * @param email the email address of the user whose information is to be retrieved
     * @return the user response object containing the details of the authenticated user
     * @throws UserNotFoundException if no user is found with the given email address
     */
    @Override
    public UserResponse loadAuthenticatedUserInformation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(String.format(USER_NOT_FOUND_BY_EMAIL, email)));
        return createUserResponseFromUser(user);
    }

    /**
     * Retrieves information about the user's device based on the user agent data
     * present in the HTTP request.
     *
     * @param request the HttpServletRequest object containing the user agent and
     *                other request-related data
     * @return a UserAgentResponse object that contains the device information
     *         extracted from the user agent string
     */
    @Override
    public UserAgentResponse getUserDeviceInformation(HttpServletRequest request) {
        return deviceInfoUtils.extractUserAgentInfo(request);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User createUserFromOAuth2(User user) {
        userRepository.save(user);
        return user;
    }
}
