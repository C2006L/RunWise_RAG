package com.runwise;
import com.runwise.common.*;
import com.runwise.service.RunnerSessions;
import com.runwise.entity.User;
import com.runwise.vo.LoginVO;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Optional real Redis contract. Only creates isolated, expiring test session keys. */
@EnabledIfEnvironmentVariable(named="RUNWISE_REDIS_TEST",matches="true")
class RunnerSessionsRedisTest {
    @Test void independentSessionsRotateAtomicallyAndLogoutDoesNotKickOtherClient(){
        RedisStandaloneConfiguration config=new RedisStandaloneConfiguration(System.getenv().getOrDefault("REDIS_HOST","127.0.0.1"),Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT","6380")));
        config.setDatabase(Integer.parseInt(System.getenv().getOrDefault("REDIS_DATABASE","0")));
        String password=System.getenv("REDIS_PASSWORD");if(password!=null&&!password.isEmpty())config.setPassword(password);
        LettuceConnectionFactory factory=new LettuceConnectionFactory(config);factory.afterPropertiesSet();
        StringRedisTemplate redis=new StringRedisTemplate(factory);
        JwtUtils jwt=new JwtUtils();ReflectionTestUtils.setField(jwt,"secret","test-session-secret-not-real-at-least-32-bytes");
        ReflectionTestUtils.setField(jwt,"accessTokenExpire",60000L);ReflectionTestUtils.setField(jwt,"refreshTokenExpire",120000L);jwt.init();
        User user=new User();user.setId(-Math.abs(UUID.randomUUID().getMostSignificantBits()));user.setOpenid("test-only");
        RunnerSessions sessions=new RunnerSessions(jwt,redis);List<String> keys=new ArrayList<>();
        try{
            LoginVO web=sessions.issue(user,false),mini=sessions.issue(user,false);
            String a=RunnerSessions.key(user.getId(),jwt.parseToken(web.getAccessToken())),b=RunnerSessions.key(user.getId(),jwt.parseToken(mini.getAccessToken()));
            keys.addAll(Arrays.asList(a+":access",a+":refresh",b+":access",b+":refresh"));assertNotEquals(a,b);
            com.runwise.mapper.UserMapper mapper=org.mockito.Mockito.mock(com.runwise.mapper.UserMapper.class);
            user.setStatus(1);org.mockito.Mockito.when(mapper.selectById(user.getId())).thenReturn(user);
            com.runwise.interceptor.JwtInterceptor interceptor=new com.runwise.interceptor.JwtInterceptor(jwt,redis,mapper);
            org.springframework.mock.web.MockHttpServletRequest webRequest=new org.springframework.mock.web.MockHttpServletRequest();webRequest.addHeader("Authorization","Bearer "+web.getAccessToken());
            org.springframework.mock.web.MockHttpServletRequest miniRequest=new org.springframework.mock.web.MockHttpServletRequest();miniRequest.addHeader("Authorization","Bearer "+mini.getAccessToken());
            assertTrue(interceptor.preHandle(webRequest,new org.springframework.mock.web.MockHttpServletResponse(),new Object()));
            assertTrue(interceptor.preHandle(miniRequest,new org.springframework.mock.web.MockHttpServletResponse(),new Object()));
            LoginVO fresh=sessions.rotate(user,web.getRefreshToken());
            assertThrows(BusinessException.class,()->interceptor.preHandle(webRequest,new org.springframework.mock.web.MockHttpServletResponse(),new Object()));
            assertTrue(interceptor.preHandle(miniRequest,new org.springframework.mock.web.MockHttpServletResponse(),new Object()));
            assertThrows(BusinessException.class,()->sessions.rotate(user,web.getRefreshToken()));
            assertEquals(mini.getAccessToken(),redis.opsForValue().get(b+":access"));
            sessions.logout(user.getId(),web.getAccessToken());
            assertEquals(fresh.getAccessToken(),redis.opsForValue().get(a+":access"));
            sessions.logout(user.getId(),mini.getAccessToken());assertNull(redis.opsForValue().get(b+":refresh"));
            assertThrows(BusinessException.class,()->sessions.rotate(user,mini.getRefreshToken()));
            assertEquals(fresh.getAccessToken(),redis.opsForValue().get(a+":access"));
            assertTrue(redis.getExpire(a+":refresh")>0);
        }finally{if(!keys.isEmpty())redis.delete(keys);factory.destroy();}
    }
}
