package com.noah.jwt.config;

import com.noah.dotrecipe.authentication.dto.JwtTokenDto;
import com.noah.jwt.entities.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TokenGenerator {

  private final JwtEncoder accessTokenEncoder;
  private final JwtEncoder refreshTokenEncoder;

  public TokenGenerator(JwtEncoder accessTokenEncoder,
                        @Qualifier("jwtRefreshTokenEncoder") JwtEncoder refreshTokenEncoder) {
    this.accessTokenEncoder = accessTokenEncoder;
    this.refreshTokenEncoder = refreshTokenEncoder;
  }

  private String createAccessToken(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    Instant now = Instant.now();

    JwtClaimsSet claimsSet = JwtClaimsSet.builder()
            .issuer("myApp")
            .issuedAt(now)
            .expiresAt(now.plus(5, ChronoUnit.MINUTES))
            .subject(String.valueOf(user.getId()))
            .build();

    return accessTokenEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
  }

  private String createRefreshToken(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    Instant now = Instant.now();

    JwtClaimsSet claimsSet = JwtClaimsSet.builder()
            .issuer("myApp")
            .issuedAt(now)
            .expiresAt(now.plus(30, ChronoUnit.DAYS))
            .subject(String.valueOf(user.getId()))
            .build();

    return refreshTokenEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
  }

  public JwtTokenDto createToken(Authentication authentication) {
    if (!(authentication.getPrincipal() instanceof User user)) {
      throw new BadCredentialsException(
              MessageFormat.format(
                      "principal {0} is not of User type",
                      authentication.getPrincipal().getClass())
      );
    }

    JwtTokenDto tokenDto = new JwtTokenDto();
    tokenDto.setUserId(String.valueOf(user.getId()));
    tokenDto.setAccessToken(createAccessToken(authentication));

    String refreshToken;
    if (authentication.getCredentials() instanceof Jwt jwt) {
      Instant now = Instant.now();
      Instant expiresAt = jwt.getExpiresAt();
      Duration duration = Duration.between(now, expiresAt);
      long daysUntilExpired = duration.toDays();
      if (daysUntilExpired < 7) {
        refreshToken = createRefreshToken(authentication);
      } else {
        refreshToken = jwt.getTokenValue();
      }
    } else {
      refreshToken = createRefreshToken(authentication);
    }
    tokenDto.setRefreshToken(refreshToken);

    return tokenDto;
  }
}
