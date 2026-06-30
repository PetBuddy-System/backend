package com.petbuddy.petbuddystore.controller;

import com.nimbusds.jose.JOSEException;
import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.*;
import com.petbuddy.petbuddystore.dto.response.AuthenticationResponse;
import com.petbuddy.petbuddystore.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.text.ParseException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication API", description = "Bảo mật user")
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/signup")
    @Operation(description = "Đăng ký user (Tạo mới tài khoản) -> OTP mail")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody UserCreationRequest request){
        authenticationService.createUserAccount(request, Role.CUSTOMER);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("OTP verification has been sent to your email"));
    }

    @PostMapping("/login")
    @Operation(description = "Login user")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(@RequestBody AuthenticationRequest request){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Login successfully", authenticationService.authenticate(request)));
    }

    @GetMapping("/outbound/authentication")
    @Operation(description = "Login with Google")
    public void outboundAuthenticate(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        AuthenticationResponse authResponse = authenticationService.outboundAuthenticate(code);
        String redirectUrl = UriComponentsBuilder
                .fromUriString("http://localhost:5173/oauth2/success")
                .queryParam("accessToken", authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/logout")
    @Operation(description = "Logout user")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Logout successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(@RequestBody RefreshTokenRequest request)
            throws ParseException, JOSEException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(authenticationService.refreshToken(request)));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request){
        authenticationService.verifyEmail(request.getEmail(), request.getOtp());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody OtpRequest request){
        authenticationService.resendOtp(request.getEmail());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("OTP has been resent to your email"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordUpdateRequest request){
        authenticationService.changePassword(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody OtpRequest request){
        authenticationService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Otp forgot password has been sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authenticationService.resetPassword(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Password reset successfully"));
    }

}
