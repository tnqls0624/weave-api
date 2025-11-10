package com.weave.domain.auth.repository;

import com.weave.domain.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

  Optional<RefreshToken> findByEmail(String email);

  Optional<RefreshToken> findByRefreshToken(String refreshToken);

  void deleteByEmail(String email);
}
