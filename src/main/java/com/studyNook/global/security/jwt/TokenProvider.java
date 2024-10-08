package com.studyNook.global.security.jwt;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.studyNook.global.security.jwt.props.JwtProperties;
import com.studyNook.global.security.jwt.types.Role;
import com.studyNook.global.security.jwt.types.TokenType;
import com.studyNook.global.common.exception.CustomException;
import com.studyNook.global.common.exception.code.AuthResponseCode;
import com.studyNook.member.dto.MemberTokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.studyNook.global.security.jwt.types.TokenType.ACCESS_TOKEN;
import static com.studyNook.global.security.jwt.types.TokenType.REFRESH_TOKEN;

@Slf4j
@Component
public class TokenProvider {
    private static final Map<String, Object> jwtHeader = Map.of("alg", "HS512", "typ", "JWT");
    private final long tokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;
    private final RedisTemplate redisTemplate;
    private final Key key;

    @Autowired
    public TokenProvider(JwtProperties properties, RedisTemplate redisTemplate) {
        this.tokenValidityInMilliseconds = properties.tokenValidityInMilliseconds();
        this.refreshTokenValidityInMilliseconds = properties.refreshTokenValidityInMilliseconds();
        byte[] keyBytes = Decoders.BASE64.decode(properties.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.redisTemplate = redisTemplate;
    }

    public MemberTokenDto generateToken(String email, Role role) {
        String accessToken = Jwts.builder()
                .setHeader(jwtHeader)
                .setSubject(email)
                .claim("role", role)
                .setExpiration(getTokenExpiration(ACCESS_TOKEN))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        String refreshToken = Jwts.builder()
                .setHeader(jwtHeader)
                .setSubject(email)
                .claim("role", role)
                .setExpiration(getTokenExpiration(REFRESH_TOKEN))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return MemberTokenDto.of(accessToken, refreshToken);
    }

    public String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        return StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : "";
    }

    public boolean validateToken(String token){
        try{

            // 로그아웃 토큰 여부 체크
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return redisTemplate.opsForValue().get(token) != null ? false : true;

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            throw new CustomException(AuthResponseCode.UNAUTHORIZED, "잘못된 토큰 서명");
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthResponseCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(AuthResponseCode.UNAUTHORIZED, "지원되지 않는 토큰");
        }
    }

    public Authentication getAuthentication(String token) {
        String username = getString(token);
        String role = getRole(token);
        List<String> roles = Splitter.on(',').splitToList(role);

        return new UsernamePasswordAuthenticationToken(username, "", getRoles(Sets.newHashSet(roles)));
    }

    public boolean isTokenExpired(String token) {
        return parseJwt(token).getExpiration().before(new Date());
    }

    public long getExpiration(String token) {
        return parseJwt(token).getExpiration().getTime();
    }

    private List<SimpleGrantedAuthority> getRoles(Set<String> roles){
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    private String getRole(String token) {
        return parseJwt(token, "role");
    }

    private String getString(String token){
        return parseJwt(token).getSubject();
    }

    protected Claims parseJwt(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token).getBody();
    }

    protected String parseJwt(String token, String claimName) {
        Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

        return claimsJws.getBody().get(claimName, String.class);
    }

    private Date getTokenExpiration(final TokenType tokenType){
        ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
        long now = zdt.toInstant().toEpochMilli();
        return tokenType.equals(REFRESH_TOKEN) ?
                new Date(now + refreshTokenValidityInMilliseconds) :
                new Date(now + tokenValidityInMilliseconds);
    }
}
