package com.petbuddy.petbuddystore.service;

import com.nimbusds.jose.JOSEException;
import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.dto.request.*;
import com.petbuddy.petbuddystore.dto.response.AuthenticationResponse;
import com.petbuddy.petbuddystore.dto.response.IntrospectResponse;

import java.text.ParseException;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);
    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;
    void logout(LogoutRequest request) throws ParseException, JOSEException;
    AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException;
    void createUserAccount(UserCreationRequest request, Role role);
    void verifyEmail(String email, String otp);
    void resendOtp(String email);
    void changePassword(PasswordUpdateRequest request);
    void resetPassword(ResetPasswordRequest request);
    void forgotPassword(OtpRequest request);
    AuthenticationResponse outboundAuthenticate(String code);
}
