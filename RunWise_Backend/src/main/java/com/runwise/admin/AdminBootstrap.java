package com.runwise.admin;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 显式开启的一次性初始化。没有默认账号密码，不重置既有账户。 */
@Component
@ConditionalOnProperty(name="runwise.admin.bootstrap", havingValue="true")
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final Environment env;
    public AdminBootstrap(JdbcTemplate jdbc, Environment env) { this.jdbc = jdbc; this.env = env; }
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = env.getRequiredProperty("ADMIN_USERNAME").toLowerCase(java.util.Locale.ROOT);
        String password = env.getRequiredProperty("ADMIN_PASSWORD");
        if (!username.matches("[A-Za-z0-9_.-]{3,64}") || password.length() < 12 || password.length() > 128)
            throw new IllegalArgumentException("管理员账号应为3-64位字母数字，密码应为12-128位");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_admin_account", Long.class) != 0)
            throw new IllegalStateException("已有管理员，初始化已拒绝；请关闭 bootstrap 配置");
        jdbc.update("INSERT INTO t_admin_account(username,password_hash,role) VALUES (?,?,?)",
                username, AdminPassword.hash(password), "SUPER_ADMIN");
        jdbc.update("INSERT INTO t_admin_audit_log(admin_id,username,action,detail) "
                + "SELECT id,username,'BOOTSTRAP','创建初始管理员' FROM t_admin_account WHERE username=?", username);
    }
}
