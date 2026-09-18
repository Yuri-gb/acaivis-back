package com.acaivis.service;

import com.acaivis.repository.AdminRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {
    private final AdminRepository repo;
    public AdminUserDetailsService(AdminRepository r){repo=r;}
    public UserDetails loadUserByUsername(String email)throws UsernameNotFoundException{
        return repo.findByEmail(email).map(a -> User.withUsername(a.getEmail())
                .password(a.getPasswordHash())
                .roles(a.getRole())
                .disabled(!a.isActive())
                .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
