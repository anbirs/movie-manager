package com.example.hometask.controller.impl;

import com.example.hometask.controller.AuthController;
import com.example.hometask.data.ApiResponse;
import com.example.hometask.data.LoginRequest;
import com.example.hometask.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthControllerImpl implements AuthController {

    @Autowired
    private AuthService authService;

    @Override
    public ResponseEntity<ApiResponse<String>> login(LoginRequest loginRequest) {
       return ResponseEntity.ok(ApiResponse.success(authService.login(loginRequest)));
    }
}
