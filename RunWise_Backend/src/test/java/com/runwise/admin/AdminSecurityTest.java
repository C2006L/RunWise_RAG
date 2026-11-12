package com.runwise.admin;

import com.runwise.common.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminSecurityTest {
    @Test void passwordIsSaltedAndVerified() {
        String hash = AdminPassword.hash("a-test-password-123");
        assertTrue(AdminPassword.matches("a-test-password-123",hash));
        assertFalse(AdminPassword.matches("wrong",hash));
        assertNotEquals(hash,AdminPassword.hash("a-test-password-123"));
    }
    @Test void ordinaryUserTokenCannotAccessAdminEvenIfSigned() {
        JwtUtils jwt=mock(JwtUtils.class);
        Claims claims=new DefaultClaims();claims.put("userId",1L);claims.put("type","access");
        when(jwt.parseToken("user-token")).thenReturn(claims);
        AdminAuthService auth=new AdminAuthService(mock(JdbcTemplate.class),mock(StringRedisTemplate.class),jwt);
        assertThrows(BusinessException.class,()->auth.authenticate("Bearer user-token"));
    }
    @SuppressWarnings("unchecked")
    @Test void logoutRevokesTokenAndDisabledAccountIsRejected() {
        JwtUtils jwt=mock(JwtUtils.class); JdbcTemplate jdbc=mock(JdbcTemplate.class);
        StringRedisTemplate redis=mock(StringRedisTemplate.class);ValueOperations<String,String> values=mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        Claims claims=new DefaultClaims();claims.setSubject("7");claims.put("scope","admin");
        when(jwt.parseToken("admin-token")).thenReturn(claims);
        when(values.get("runwise:admin:session:7")).thenReturn(null);
        AdminAuthService auth=new AdminAuthService(jdbc,redis,jwt);
        assertThrows(BusinessException.class,()->auth.authenticate("Bearer admin-token"));
        verifyNoInteractions(jdbc);
        when(values.get("runwise:admin:session:7")).thenReturn("admin-token");
        when(jdbc.query(anyString(),any(RowMapper.class),eq(7L))).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class,()->auth.authenticate("Bearer admin-token"));
        AdminAccount account=new AdminAccount();account.setId(7L);auth.logout(account);
        verify(redis).delete("runwise:admin:session:7");
    }
    @Test void editorCannotReadAuditLogs() {
        AdminAuthService auth=mock(AdminAuthService.class);AdminAccount editor=new AdminAccount();editor.setRole("CONTENT_EDITOR");
        when(auth.authenticate(any())).thenReturn(editor);
        MockHttpServletRequest request=new MockHttpServletRequest("GET","/api/admin/audit-logs");
        assertThrows(BusinessException.class,()->new AdminInterceptor(auth).preHandle(request,new MockHttpServletResponse(),new Object()));
    }
}
