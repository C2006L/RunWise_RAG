package com.runwise;

import com.runwise.controller.InternalHealthController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class InternalHealthTest {
    @Test void healthyOnlyWhenDependenciesAndSchemaAreReady() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(jdbc.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(startsWith("SELECT COUNT"), eq(Integer.class))).thenReturn(11);
        when(redis.execute(any(RedisCallback.class))).thenReturn("PONG");
        InternalHealthController health = new InternalHealthController(jdbc, redis);
        assertEquals(200, health.health().getStatusCodeValue());
        when(jdbc.queryForObject(startsWith("SELECT COUNT"), eq(Integer.class))).thenReturn(0);
        assertEquals(503, health.health().getStatusCodeValue());
    }

    @Test void redisFailureIsReportedWithoutLeakingExceptionDetails() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisCallback.class))).thenThrow(new RuntimeException("private-host-password"));
        org.springframework.http.ResponseEntity<?> result = new InternalHealthController(jdbc, redis).health();
        assertEquals(503, result.getStatusCodeValue());
        assertFalse(result.getBody().toString().contains("private-host-password"));
    }
}
