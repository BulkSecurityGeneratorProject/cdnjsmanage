package com.huyduc.manage.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huyduc.manage.repository.UserRepository;
import com.huyduc.manage.security.jwt.JWTFilter;
import com.huyduc.manage.security.jwt.TokenProvider;
import com.huyduc.manage.web.rest.vm.LoginVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collections;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class UserJWTController {

    private final Logger log = LoggerFactory.getLogger(UserJWTController.class);
    private final TokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public UserJWTController(TokenProvider tokenProvider, AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
            String jwt = tokenProvider.createToken(authentication, rememberMe);
            response.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
            return ResponseEntity.ok(new JWTToken(jwt));
        } catch (AuthenticationException ae) {
            log.trace("Authentication exception trace: {}", ae);
            return new ResponseEntity(Collections.singletonMap("AuthenticationException",
                    ae.getLocalizedMessage()), HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
