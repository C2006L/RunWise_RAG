package com.runwise;

import com.runwise.service.*;
import com.runwise.admin.*;
import com.runwise.common.*;
import com.runwise.dto.*;
import com.runwise.entity.User;
import com.runwise.entity.InjuryArticle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.redis.core.*;
import javax.sql.DataSource;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真正执行 JDBC 与事务，但使用独立内存数据库，不读取开发环境数据库。 */
@ExtendWith(SpringExtension.class) @ContextConfiguration(classes=PersistenceFlowTest.Config.class)
@org.springframework.test.context.TestPropertySource(properties={"runwise.jwt.secret=test-only-secret-at-least-32-bytes-long","runwise.jwt.access-token-expire=7200000","runwise.jwt.refresh-token-expire=604800000"})
class PersistenceFlowTest {
    @Configuration @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {return new DriverManagerDataSource("jdbc:h2:mem:runwise-test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE","sa","");}
        @Bean JdbcTemplate jdbc(DataSource source) {return new JdbcTemplate(source);}
        @Bean PlatformTransactionManager tx(DataSource source) {return new DataSourceTransactionManager(source);}
        @Bean PlanService plans(JdbcTemplate jdbc) {return new PlanService(jdbc);}
        @Bean AdminArticleService articles(JdbcTemplate jdbc) {return new AdminArticleService(jdbc,new ObjectMapper());}
        @Bean UserService users(JdbcTemplate jdbc) {
            UserService users=mock(UserService.class);
            when(users.save(any(User.class))).thenAnswer(invocation->{
                User user=invocation.getArgument(0);
                jdbc.update("INSERT INTO t_user(openid,nickname,status) VALUES (?,?,?)",user.getOpenid(),user.getNickname(),user.getStatus());
                user.setId(jdbc.queryForObject("SELECT id FROM t_user WHERE openid=?",Long.class,user.getOpenid()));return true;
            });
            when(users.getById(any(java.io.Serializable.class))).thenAnswer(invocation->{
                List<User> rows=jdbc.query("SELECT * FROM t_user WHERE id=?",new org.springframework.jdbc.core.BeanPropertyRowMapper<>(User.class),new Object[]{invocation.getArgument(0)});
                return rows.isEmpty()?null:rows.get(0);
            });return users;
        }
        @Bean JwtUtils jwt() {
            JwtUtils jwt=new JwtUtils();org.springframework.test.util.ReflectionTestUtils.setField(jwt,"secret","test-only-secret-at-least-32-bytes-long");
            org.springframework.test.util.ReflectionTestUtils.setField(jwt,"accessTokenExpire",7200000L);
            org.springframework.test.util.ReflectionTestUtils.setField(jwt,"refreshTokenExpire",604800000L);jwt.init();return jwt;
        }
        @SuppressWarnings("unchecked") @Bean StringRedisTemplate redis() {
            StringRedisTemplate redis=mock(StringRedisTemplate.class);when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));return redis;
        }
        @Bean PasswordAuthService passwordAuth(JdbcTemplate jdbc,UserService users,JwtUtils jwt,StringRedisTemplate redis) {return new PasswordAuthService(jdbc,users,jwt,redis);}
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired PlanService plans;
    @Autowired AdminArticleService articles;
    @Autowired PasswordAuthService auth;
    @BeforeEach void schema() {
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE t_user(id BIGINT AUTO_INCREMENT PRIMARY KEY,openid VARCHAR(64),nickname VARCHAR(64),status INT)");
        jdbc.execute("CREATE TABLE t_web_credential(user_id BIGINT PRIMARY KEY,username VARCHAR(32) UNIQUE,password_hash VARCHAR(255))");
        jdbc.execute("CREATE TABLE t_training_plan_item(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,training_date DATE,type VARCHAR(16),target_km DECIMAL(6,2),note VARCHAR(255),UNIQUE(user_id,training_date))");
        jdbc.execute("CREATE TABLE t_injury_article(id BIGINT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(128),category VARCHAR(32),summary VARCHAR(255),tags VARCHAR(255),sections TEXT,read_minutes INT,sort INT,status VARCHAR(16),revision INT,updated_by BIGINT,deleted INT DEFAULT 0,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE t_admin_audit_log(id BIGINT AUTO_INCREMENT PRIMARY KEY,admin_id BIGINT,username VARCHAR(64),action VARCHAR(64),target_id BIGINT,detail VARCHAR(1000),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }
    private PlanItemDTO input() {
        PlanItemDTO input=new PlanItemDTO();input.setDate(LocalDate.now());input.setType("easy");input.setTargetKm(new BigDecimal("5.25"));input.setNote("test");return input;
    }
    @Test void plansPersistAndAreIsolatedAcrossUsers() {
        plans.add(7L,input());
        Long id=jdbc.queryForObject("SELECT id FROM t_training_plan_item",Long.class);
        assertThrows(BusinessException.class,()->plans.update(8L,id,input()));
        assertThrows(BusinessException.class,()->plans.remove(8L,id));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM t_training_plan_item",Integer.class));
        assertFalse(plans.weeks(7L).isEmpty());
        plans.remove(7L,id);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_training_plan_item",Integer.class));
    }
    @Test void duplicatePlanDateCannotOverwriteExistingPlan() {
        plans.add(7L,input());assertThrows(org.springframework.dao.DuplicateKeyException.class,()->plans.add(7L,input()));
        assertEquals(new BigDecimal("5.25"),jdbc.queryForObject("SELECT target_km FROM t_training_plan_item",BigDecimal.class));
    }
    private AdminRequests.Article article() {
        AdminRequests.Article input=new AdminRequests.Article();input.setTitle("test");input.setCategory("knee");input.setSummary("summary");input.setTags("test");input.setReadMinutes(5);input.setSort(0);input.setRevision(0);
        AdminRequests.Section section=new AdminRequests.Section();section.setHeading("heading");section.setBody("body");input.setSections(Collections.singletonList(section));return input;
    }
    private AdminAccount admin() {AdminAccount admin=new AdminAccount();admin.setId(1L);admin.setUsername("editor");return admin;}
    @Test void articlePublicationAndVersionConflictArePersisted() {
        InjuryArticle created=articles.create(article(),admin());assertEquals("DRAFT",created.getStatus());
        AdminRequests.Status status=new AdminRequests.Status();status.setStatus("PUBLISHED");status.setRevision(1);
        InjuryArticle published=articles.status(created.getId(),status,admin());assertEquals(2,published.getRevision());assertEquals("PUBLISHED",published.getStatus());
        assertThrows(BusinessException.class,()->articles.status(created.getId(),status,admin()));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM t_admin_audit_log",Integer.class));
    }
    @Test void auditFailureRollsBackArticleWrite() {
        jdbc.execute("ALTER TABLE t_admin_audit_log ADD CHECK(action='IMPOSSIBLE')");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->articles.create(article(),admin()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_injury_article",Integer.class));
    }
    private PasswordLoginDTO credential(String username) {PasswordLoginDTO dto=new PasswordLoginDTO();dto.setUsername(username);dto.setPassword("test-password-123");return dto;}
    @Test void registerLoginAndDuplicateRegistrationRollback() {
        com.runwise.vo.LoginVO registered=auth.register(credential("Runner"));
        assertEquals(registered.getUserId(),auth.login(credential("runner")).getUserId());
        assertNotEquals("test-password-123",jdbc.queryForObject("SELECT password_hash FROM t_web_credential",String.class));
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->auth.register(credential("runner")));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM t_user",Integer.class));
    }
    @Test void bindingReusesUserAndDisabledAccountCannotLogin() {
        jdbc.update("INSERT INTO t_user(id,openid,nickname,status) VALUES (99,'wechat-test','Runner',1)");
        auth.bind(99L,credential("bound"));assertEquals(99L,auth.login(credential("bound")).getUserId());
        jdbc.update("UPDATE t_user SET status=0 WHERE id=99");
        assertThrows(BusinessException.class,()->auth.login(credential("bound")));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM t_user",Integer.class));
    }
}
