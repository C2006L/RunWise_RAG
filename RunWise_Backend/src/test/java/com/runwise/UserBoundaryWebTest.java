package com.runwise;

import com.runwise.common.*;
import com.runwise.config.WebMvcConfig;
import com.runwise.admin.*;
import com.runwise.interceptor.JwtInterceptor;
import com.runwise.mapper.UserMapper;
import com.runwise.service.*;
import com.runwise.controller.*;
import com.runwise.entity.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class) @WebAppConfiguration
@ContextConfiguration(classes=UserBoundaryWebTest.Config.class)
class UserBoundaryWebTest {
    @Configuration @EnableWebMvc @Import({WebMvcConfig.class,GlobalExceptionHandler.class})
    static class Config {
        @Bean JwtInterceptor jwtInterceptor() {return mock(JwtInterceptor.class);}
        @Bean AdminInterceptor adminInterceptor() {return mock(AdminInterceptor.class);}
        @Bean QaService qaService() {return mock(QaService.class);}
        @Bean UserService userService() {return mock(UserService.class);}
        @Bean QaController qaController(QaService qa) {return new QaController(qa);}
        @Bean UserController userController(UserService user) {return new UserController(user);}
    }
    @Autowired WebApplicationContext context;
    @Autowired JwtInterceptor interceptor;
    @Autowired QaService qa;
    @Autowired UserService users;
    MockMvc mvc;
    @BeforeEach void setup() {reset(interceptor,qa,users);mvc=MockMvcBuilders.webAppContextSetup(context).build();}
    @Test void qaPublicIndexDoesNotRequireLogin() throws Exception {
        when(qa.hotQuestions()).thenReturn(Collections.emptyList());mvc.perform(get("/api/qa/hot")).andExpect(status().isOk());verifyNoInteractions(interceptor);
    }
    @Test void anonymousHistoryIsBlockedByActualRouteConfiguration() throws Exception {
        when(interceptor.preHandle(any(),any(),any())).thenThrow(new BusinessException(ResultCode.UNAUTHORIZED));
        mvc.perform(get("/api/qa/history")).andExpect(status().isUnauthorized());verifyNoInteractions(qa);
    }
    @Test void qaUsesAuthenticatedIdentityInsteadOfUserOne() throws Exception {
        when(interceptor.preHandle(any(),any(),any())).thenAnswer(invocation->{((javax.servlet.http.HttpServletRequest)invocation.getArgument(0)).setAttribute("userId",42L);return true;});
        when(qa.ask(eq(42L),any())).thenReturn(Collections.singletonMap("answer","test"));
        mvc.perform(post("/api/qa/ask").contentType("application/json").content("{\"question\":\"跑步怎么开始\"}"))
                .andExpect(status().isOk());verify(qa).ask(eq(42L),any());
    }
    @Test void profileCannotMassAssignIdentityOrStatus() throws Exception {
        when(interceptor.preHandle(any(),any(),any())).thenAnswer(invocation->{((javax.servlet.http.HttpServletRequest)invocation.getArgument(0)).setAttribute("userId",42L);return true;});
        mvc.perform(put("/api/user/profile").contentType("application/json").content("{\"id\":1,\"openid\":\"other\",\"status\":0,\"deleted\":1,\"nickname\":\"Runner\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<User> value=ArgumentCaptor.forClass(User.class);verify(users).updateById(value.capture());
        assertEquals(42L,value.getValue().getId());assertNull(value.getValue().getStatus());assertNull(value.getValue().getOpenid());assertNull(value.getValue().getDeleted());
    }
}
