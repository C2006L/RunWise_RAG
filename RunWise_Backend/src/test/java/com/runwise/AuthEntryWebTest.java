package com.runwise;
import com.runwise.controller.AuthController;
import com.runwise.common.GlobalExceptionHandler;
import com.runwise.service.*;
import com.runwise.dto.*;
import com.runwise.vo.LoginVO;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthEntryWebTest {
    PasswordAuthService auth;MockMvc mvc;
    @BeforeEach void setup(){auth=mock(PasswordAuthService.class);mvc=MockMvcBuilders.standaloneSetup(new AuthController(mock(UserService.class),auth)).setControllerAdvice(new GlobalExceptionHandler()).build();}
    @Test void registrationRequiresMatchingConfirmationBeforeCreatingAnything() throws Exception {
        for(String json:new String[]{"{\"username\":\"跑者小明\",\"password\":\"test-password-123\"}","{\"username\":\"跑者小明\",\"password\":\"test-password-123\",\"confirmPassword\":\"different-password\"}"})
            mvc.perform(post("/api/auth/register").contentType("application/json").content(json)).andExpect(status().isBadRequest());
        verifyNoInteractions(auth);
    }
    @Test void chineseNamesWorkForRegistrationAndLogin() throws Exception {
        when(auth.register(any())).thenReturn(new LoginVO());when(auth.login(any())).thenReturn(new LoginVO());
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"username\":\"小明\",\"password\":\"test-password-123\",\"confirmPassword\":\"test-password-123\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login-password").contentType("application/json").content("{\"username\":\"小明\",\"password\":\"test-password-123\"}")).andExpect(status().isOk());
        verify(auth).register(argThat(input->"小明".equals(input.getUsername())));verify(auth).login(argThat(input->"小明".equals(input.getUsername())));
    }
    @Test void firstPasswordBindingAlsoRequiresConfirmation() throws Exception {
        mvc.perform(post("/api/auth/bind-password").requestAttr("userId",7L).contentType("application/json").content("{\"username\":\"小明\",\"password\":\"test-password-123\"}")).andExpect(status().isBadRequest());verifyNoInteractions(auth);
    }
}
