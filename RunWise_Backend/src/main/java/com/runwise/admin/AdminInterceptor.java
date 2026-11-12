package com.runwise.admin;

import com.runwise.common.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.*;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    private final AdminAuthService auth;
    public AdminInterceptor(AdminAuthService auth) { this.auth = auth; }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equals(request.getMethod())) return true;
        AdminAccount account = auth.authenticate(request.getHeader("Authorization"));
        request.setAttribute("admin", account);
        if (request.getRequestURI().startsWith("/api/admin/audit-logs") && !"SUPER_ADMIN".equals(account.getRole()))
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可查看操作日志");
        return true;
    }
}
