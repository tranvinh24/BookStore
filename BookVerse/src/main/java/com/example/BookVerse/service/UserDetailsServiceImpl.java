package com.example.BookVerse.service;

import com.example.BookVerse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService tách biệt để tránh circular dependency.
 * SecurityConfig inject bean này thay vì inject AuthService trực tiếp.
 *
 * Vòng tròn bị vỡ: SecurityConfig → UserDetailsServiceImpl → UserRepository
 * (không phụ thuộc lại SecurityConfig)
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUserName(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("Không tìm thấy user: " + username));
    }
}
