package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    // Langsung return entity User karena udah implements UserDetails
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new UsernameNotFoundException("User with the email " + email + " not found"));
  }
}
