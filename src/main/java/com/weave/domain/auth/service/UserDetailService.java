package com.weave.domain.auth.service;

import com.google.common.collect.ImmutableList;
import com.weave.domain.auth.repository.AuthRepository;
import com.weave.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {

  private final AuthRepository authRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = authRepository.findByEmailAndDeletedFalse(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword() == null ? "" : user.getPassword(),
        ImmutableList.of(new SimpleGrantedAuthority("ROLE_USER"))
    );
  }
}
