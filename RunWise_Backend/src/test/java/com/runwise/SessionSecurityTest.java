package com.runwise;
import com.runwise.common.*;
import com.runwise.entity.User;
import com.runwise.mapper.UserMapper;
import com.runwise.interceptor.JwtInterceptor;
import com.runwise.service.impl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultClaims;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionSecurityTest {
    JwtUtils jwt;StringRedisTemplate redis;ValueOperations<String,String> values;UserMapper mapper;
    UserServiceImpl service;Claims claims;User user;
    @SuppressWarnings("unchecked") @BeforeEach void setup() {
        jwt=mock(JwtUtils.class);redis=mock(StringRedisTemplate.class);values=mock(ValueOperations.class);mapper=mock(UserMapper.class);
        when(redis.opsForValue()).thenReturn(values);when(jwt.isValid(anyString())).thenReturn(true);
        claims=new DefaultClaims();claims.put("type","refresh");claims.put("userId",7L);
        when(jwt.parseToken(anyString())).thenReturn(claims);when(jwt.getUserId(anyString())).thenReturn(7L);
        when(jwt.getAccessTokenExpire()).thenReturn(7200000L);when(jwt.getRefreshTokenExpire()).thenReturn(604800000L);
        when(jwt.generateAccessToken(anyLong(),anyString())).thenReturn("new-access");
        when(jwt.generateRefreshToken(anyLong(),anyString())).thenReturn("new-refresh");
        user=new User();user.setId(7L);user.setOpenid("test-openid");user.setStatus(1);when(mapper.selectById(7L)).thenReturn(user);
        service=new UserServiceImpl(jwt,redis);ReflectionTestUtils.setField(service,"baseMapper",mapper);
    }
    @Test void accessTokenCannotRefresh() {claims.put("type","access");assertThrows(BusinessException.class,()->service.refreshToken("access"));verifyNoInteractions(mapper);}
    @Test void disabledUserCannotRefresh() {user.setStatus(0);assertThrows(BusinessException.class,()->service.refreshToken("refresh"));}
    @SuppressWarnings("unchecked") @Test void replacedRefreshCannotRotate() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(0L);
        assertThrows(BusinessException.class,()->service.refreshToken("old-refresh"));
    }
    @SuppressWarnings("unchecked") @Test void activeRefreshRotatesPair() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(1L);
        // Mockito 4 展开 varargs，因此按完整参数模拟 Redis CAS 成功。
        when(redis.execute(any(RedisScript.class),anyList(),eq("refresh"),eq("new-access"),eq("new-refresh"),eq("7200000"),eq("604800000"))).thenReturn(1L);
        assertEquals("new-refresh",service.refreshToken("refresh").getRefreshToken());
    }
    @Test void refreshAndAdminCredentialsCannotActAsAccess() {
        JwtInterceptor interceptor=new JwtInterceptor(jwt,redis,mapper);
        MockHttpServletRequest request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer token");
        assertThrows(BusinessException.class,()->interceptor.preHandle(request,new MockHttpServletResponse(),new Object()));
        claims.put("type","access");claims.put("scope","admin");
        assertThrows(BusinessException.class,()->interceptor.preHandle(request,new MockHttpServletResponse(),new Object()));
    }
    @Test void accessChecksCurrentAccountStatus() {
        claims.put("type","access");when(values.get("runwise:token:7:access")).thenReturn("token");user.setStatus(0);
        MockHttpServletRequest request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer token");
        assertThrows(BusinessException.class,()->new JwtInterceptor(jwt,redis,mapper).preHandle(request,new MockHttpServletResponse(),new Object()));
    }
    @Test void mintedTokensAreUniqueEvenInTheSameSecond() {
        JwtUtils actual=new JwtUtils();ReflectionTestUtils.setField(actual,"secret","test-only-secret-with-at-least-32-bytes");
        ReflectionTestUtils.setField(actual,"accessTokenExpire",7200000L);actual.init();
        assertNotEquals(actual.generateAccessToken(7L,"test"),actual.generateAccessToken(7L,"test"));
    }
}
