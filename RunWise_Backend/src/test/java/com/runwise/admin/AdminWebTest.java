package com.runwise.admin;

import com.runwise.common.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminWebTest {
    private AdminAuthService auth;
    private AdminArticleService articles;
    private MockMvc mvc;
    @BeforeEach void setup() {
        auth=mock(AdminAuthService.class);articles=mock(AdminArticleService.class);
        mvc=MockMvcBuilders.standaloneSetup(new AdminController(auth,articles))
                .setControllerAdvice(new AdminExceptionHandler(),new GlobalExceptionHandler())
                .addInterceptors(new AdminInterceptor(auth)).build();
    }
    @Test void missingCredentialsReturnActual401BeforeArticleService() throws Exception {
        when(auth.authenticate(null)).thenThrow(new BusinessException(ResultCode.UNAUTHORIZED));
        mvc.perform(get("/api/admin/articles")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        verifyNoInteractions(articles);
    }
    @Test void editorCannotAccessAuditEndpoint() throws Exception {
        AdminAccount account=new AdminAccount();account.setRole("CONTENT_EDITOR");
        when(auth.authenticate("Bearer editor")).thenReturn(account);
        mvc.perform(get("/api/admin/audit-logs").header("Authorization","Bearer editor"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(articles);
    }
    @Test void profileNeverSerializesPasswordHash() throws Exception {
        AdminAccount account=new AdminAccount();account.setId(1L);account.setRole("SUPER_ADMIN");account.setPasswordHash("must-not-leak");
        when(auth.authenticate("Bearer admin")).thenReturn(account);
        mvc.perform(get("/api/admin/auth/me").header("Authorization","Bearer admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }
}
