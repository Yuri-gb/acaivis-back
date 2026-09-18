package com.acaivis.controller;

import com.acaivis.dto.*;
import com.acaivis.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager auth;
    private final JwtService jwt;
    public AuthController(AuthenticationManager a,JwtService j){auth=a;jwt=j;}
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest d){
        Authentication authentication = auth.authenticate(new UsernamePasswordAuthenticationToken(d.email(),d.password()));
        String role = authentication.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("ADMIN");
        return new LoginResponse(jwt.generate(d.email()),"Bearer",role);
    }
}
