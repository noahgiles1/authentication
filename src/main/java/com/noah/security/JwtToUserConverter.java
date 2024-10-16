package com.noah.security;

import com.noah.db.User;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class JwtToUserConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        User user = new User();
        user.setId(UUID.fromString(jwt.getSubject())); // We are setting the JWT subject as user id, which is Long
        return new UsernamePasswordAuthenticationToken(user, jwt, Collections.emptyList());
    }
}
