package com.huyduc.manage.web.rest;

import com.huyduc.manage.bean.User;
import com.huyduc.manage.repository.UserRepository;
import com.huyduc.manage.security.SecurityUtils;
import com.huyduc.manage.service.UserService;
import com.huyduc.manage.service.dto.PasswordChangeDTO;
import com.huyduc.manage.service.dto.UserDTO;
import com.huyduc.manage.web.rest.errors.EmailAlreadyUsedException;
import com.huyduc.manage.web.rest.errors.EmailNotFoundException;
import com.huyduc.manage.web.rest.errors.InternalServerErrorException;
import com.huyduc.manage.web.rest.errors.InvalidPasswordException;
import com.huyduc.manage.web.rest.vm.KeyAndPasswordVM;
import com.huyduc.manage.web.rest.vm.ManagedUserVM;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private static final Logger log = LoggerFactory.getLogger(AccountResource.class);
    private final UserRepository userRepository;
    private final UserService userService;

    public AccountResource(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

//    /**
//     * POST  /register : register the user.
//     *
//     * @param registerUserAccountVM the managed user View Model
//     * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
//     * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
//     * @throws LoginAlreadyUsedException 400 (Bad Request) if the login is already used
//     */
//    @PostMapping(value = "/register")
//    public ResponseEntity<User> registerAccount(@Valid @RequestBody RegisterUserAccountVM registerUserAccountVM) {
//        if (!checkPasswordLength(registerUserAccountVM.getPassword())) {
//            throw new InvalidPasswordException();
//        }
//        if (!isIdenticalPassword(registerUserAccountVM.getPassword(), registerUserAccountVM.getRe_password())) {
//            throw new PasswordNotMatchException();
//        }
//        try {
//            User user = userService.registerUser(registerUserAccountVM, registerUserAccountVM.getPassword());
//            return new ResponseEntity(user, HttpStatus.CREATED);
//        } catch (Exception e) {
//            return new ResponseEntity(Collections.singletonMap("createFailed",
//                    e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
//        }
//    }

    /**
     * GET  /activate : activate the registered user.
     *
     * @param key the activation key
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be activated
     */
    @GetMapping("/activate")
    public void activateAccount(@RequestParam(value = "key") String key) {
        Optional<User> user = userService.activateRegistration(key);
        if (!user.isPresent()) {
            throw new InternalServerErrorException("No user was found for this activation key");
        }
    }

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        return request.getRemoteUser();
    }

    /**
     * GET  /account : get the current user.
     *
     * @return the current user
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    public UserDTO getAccount() {
        return userService.getUserWithAuthorities()
                .map(UserDTO::new)
                .orElseThrow(() -> new InternalServerErrorException("User could not be found"));
    }

    /**
     * POST  /account : update the current user information.
     *
     * @param userDTO the current user information
     * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
     * @throws RuntimeException          500 (Internal Server Error) if the user login wasn't found
     */
    @PostMapping("/account")
    public void saveAccount(@Valid @RequestBody UserDTO userDTO) {
        final String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new InternalServerErrorException("Current user login not found"));
        Optional<User> user = userRepository.findOneByLogin(userLogin);
        if (!user.isPresent()) {
            throw new InternalServerErrorException("User could not be found");
        }
        userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(),
                userDTO.getLangKey(), userDTO.getImageUrl(), userDTO.getAddress(), userDTO.getPhoneNumber(), userDTO.getIdentityCardNumber());
    }

    /**
     * POST  /account/change-password : changes the current user's password
     *
     * @param passwordChangeDto current and new password
     * @throws InvalidPasswordException 400 (Bad Request) if the new password is incorrect
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (!checkPasswordLength(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * POST   /account/reset-password/init : Send an email to reset the password of the user
     *
     * @param mail the mail of the user
     * @throws EmailNotFoundException 400 (Bad Request) if the email address is not registered
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        //    mailService.sendPasswordResetMail(
        //        userService.requestPasswordReset(mail)
        //            .orElseThrow(EmailNotFoundException::new)
        //    );
    }

    /**
     * POST   /account/reset-password/finish : Finish to reset the password of the user
     *
     * @param keyAndPassword the generated key and the new password
     * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
     * @throws RuntimeException         500 (Internal Server Error) if the password could not be reset
     */
    @PostMapping(path = "/account/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user =
                userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new InternalServerErrorException("No user was found for this reset key");
        }
    }

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
                password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
                password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }

    private static boolean isIdenticalPassword(String password, String re_password) {
        return password.equals(re_password);
    }
}
