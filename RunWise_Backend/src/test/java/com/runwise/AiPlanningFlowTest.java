package com.runwise;
import com.runwise.service.*;
import com.runwise.dto.*;
import com.runwise.common.*;
import com.runwise.mapper.QaRecordMapper;
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
import javax.sql.DataSource;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class) @ContextConfiguration(classes=AiPlanningFlowTest.Config.class)
class AiPlanningFlowTest {
    @Configuration @EnableTransactionManagement static class Config {
        @Bean DataSource source(){return new DriverManagerDataSource("jdbc:h2:mem:ai-plan-tests;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE","sa","");}
        @Bean JdbcTemplate jdbc(DataSource source){return new JdbcTemplate(source);}
        @Bean PlatformTransactionManager tx(DataSource source){return new DataSourceTransactionManager(source);}
        @Bean RunnerContextService context(){return mock(RunnerContextService.class);}
        @Bean PlanGenerationGateway generator(){return mock(PlanGenerationGateway.class);}
        @Bean QaRecordMapper history(){return mock(QaRecordMapper.class);}
        @Bean PlanService plans(JdbcTemplate jdbc){return new PlanService(jdbc);}
        @Bean PlanDraftValidator validator(){return new PlanDraftValidator();}
        @Bean ObjectMapper json(){return new ObjectMapper().findAndRegisterModules();}
        @Bean PlanDraftService drafts(RunnerContextService c,PlanGenerationGateway g,PlanDraftValidator v,PlanService p,JdbcTemplate j,ObjectMapper json,QaRecordMapper h,PlatformTransactionManager tx){return new PlanDraftService(c,g,v,p,j,json,h,tx);}
    }
    @Autowired PlanDraftService drafts;@Autowired RunnerContextService context;@Autowired PlanGenerationGateway generator;
    @Autowired JdbcTemplate jdbc;@Autowired QaRecordMapper history;@Autowired PlanDraftValidator validator;
    Map<String,Object> snapshot;PlanDraftRequest input;GeneratedPlan generated;
    @BeforeEach void setup(){
        reset(context,generator,history);jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE t_ai_plan_draft(id VARCHAR(36) PRIMARY KEY,user_id BIGINT,status VARCHAR(16) DEFAULT 'DRAFT',request_json CLOB,context_json CLOB,plan_json CLOB,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,expires_at TIMESTAMP,applied_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE t_training_plan_item(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,training_date DATE,type VARCHAR(16),target_km DECIMAL(6,2),note VARCHAR(255),UNIQUE(user_id,training_date))");
        snapshot=new LinkedHashMap<>();snapshot.put("recordCount",8);snapshot.put("weeklyAverageKm",new BigDecimal("10.00"));snapshot.put("longestRunKm",new BigDecimal("5"));snapshot.put("recordVersion","v1");snapshot.put("lastTrainingDate",LocalDate.now().toString());
        when(context.build(7L)).thenAnswer(invocation->new LinkedHashMap<>(snapshot));
        input=new PlanDraftRequest();input.setGoal("maintain");input.setStartDate(LocalDate.now().plusDays(1));input.setSessionsPerWeek(3);input.setNoDiscomfortConfirmed(true);
        generated=new GeneratedPlan();generated.setExplanation("## 计划说明\n依据真实记录安排。");generated.setModel("test-model");generated.setSources(Collections.singletonList("test-source"));
        List<PlanItemDTO> days=new ArrayList<>();for(int i=0;i<7;i++){PlanItemDTO day=new PlanItemDTO();day.setDate(input.getStartDate().plusDays(i));day.setType(i%2==0&&i<5?"easy":"rest");day.setTargetKm(day.getType().equals("rest")?BigDecimal.ZERO:new BigDecimal("3"));day.setNote("test");days.add(day);}generated.setDays(days);
        when(generator.generate(any(),anyMap())).thenReturn(generated);
    }
    @Test void generationDoesNotWriteCourseAndConfirmationIsIdempotent(){
        Map<String,Object> draft=drafts.generate(7L,input);String id=(String)draft.get("id");
        assertEquals(0,count());assertEquals("DRAFT",draft.get("status"));
        assertEquals("APPLIED",drafts.confirm(7L,id).get("status"));assertEquals(7,count());
        drafts.confirm(7L,id);assertEquals(7,count());
    }
    @Test void baseWeekCannotAcceptAnUnexpectedQualityWorkout(){
        generated.getDays().get(0).setType("interval");
        assertThrows(BusinessException.class,()->drafts.generate(7L,input));
        assertEquals(0,count());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_ai_plan_draft",Integer.class));
    }
    @Test void draftCannotBeOpenedOrSavedByAnotherUser(){
        String id=(String)drafts.generate(7L,input).get("id");
        assertThrows(BusinessException.class,()->drafts.detail(8L,id));assertThrows(BusinessException.class,()->drafts.confirm(8L,id));assertEquals(0,count());
    }
    @Test void existingCoursesAreNotOverwrittenAndTransactionLeavesNoPartialPlan(){
        String id=(String)drafts.generate(7L,input).get("id");jdbc.update("INSERT INTO t_training_plan_item(user_id,training_date,type,target_km,note) VALUES (7,?,'easy',2,'original')",input.getStartDate());
        assertThrows(BusinessException.class,()->drafts.confirm(7L,id));assertEquals(1,count());
        assertEquals("original",jdbc.queryForObject("SELECT note FROM t_training_plan_item",String.class));assertEquals("DRAFT",drafts.detail(7L,id).get("status"));
    }
    @Test void changedRecordsAndExpiredDraftsRequireRegeneration(){
        String id=(String)drafts.generate(7L,input).get("id");snapshot.put("recordVersion","v2");assertThrows(BusinessException.class,()->drafts.confirm(7L,id));
        snapshot.put("recordVersion","v1");jdbc.update("UPDATE t_ai_plan_draft SET expires_at=? WHERE id=?",java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(1)),id);
        assertThrows(BusinessException.class,()->drafts.confirm(7L,id));assertEquals(0,count());
    }
    @Test void missingRecordsOrDiscomfortDoNotCallTheModel(){
        snapshot.put("recordCount",0);assertEquals("NEEDS_INPUT",drafts.generate(7L,input).get("status"));verifyNoInteractions(generator);assertEquals(0,count());
        snapshot.put("recordCount",8);input.setNoDiscomfortConfirmed(false);assertEquals("NEEDS_INPUT",drafts.generate(7L,input).get("status"));verifyNoInteractions(generator);
    }
    @Test void fabricatedMileageAndMalformedSchedulesAreRejectedBeforePersistence(){
        generated.getDays().get(0).setTargetKm(new BigDecimal("100"));assertThrows(BusinessException.class,()->drafts.generate(7L,input));
        generated.getDays().get(0).setTargetKm(new BigDecimal("3"));generated.getDays().get(1).setDate(input.getStartDate());assertThrows(BusinessException.class,()->drafts.generate(7L,input));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_ai_plan_draft",Integer.class));
    }
    @Test void futureWindowIsAlignedWithExistingFourWeekPlan(){
        input.setStartDate(LocalDate.now().with(DayOfWeek.MONDAY).plusDays(25));assertThrows(BusinessException.class,()->drafts.generate(7L,input));verifyNoInteractions(generator);
    }
    @Test void laterInsertFailureRollsBackEveryCourse(){
        String id=(String)drafts.generate(7L,input).get("id");
        jdbc.execute("ALTER TABLE t_training_plan_item ADD CHECK(training_date <> DATE '"+input.getStartDate().plusDays(4)+"')");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->drafts.confirm(7L,id));
        assertEquals(0,count());assertEquals("DRAFT",drafts.detail(7L,id).get("status"));
    }
    @Test void concurrentConfirmationsInsertOnlyOneSetOfCourses() throws Exception {
        String id=(String)drafts.generate(7L,input).get("id");
        java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{
            java.util.concurrent.Future<Map<String,Object>> first=workers.submit(()->drafts.confirm(7L,id));
            java.util.concurrent.Future<Map<String,Object>> second=workers.submit(()->drafts.confirm(7L,id));
            assertEquals("APPLIED",first.get(10,java.util.concurrent.TimeUnit.SECONDS).get("status"));
            assertEquals("APPLIED",second.get(10,java.util.concurrent.TimeUnit.SECONDS).get("status"));assertEquals(7,count());
        }finally{workers.shutdownNow();}
    }
    @Test void modelCanAskForSchedulingClarificationWithoutSavingDraftOrHistory(){
        GeneratedPlan needs=new GeneratedPlan();needs.setStatus("NEEDS_INPUT");needs.setQuestions(Collections.singletonList("请补充可训练日期"));
        when(generator.generate(any(),anyMap())).thenReturn(needs);
        assertEquals("NEEDS_INPUT",drafts.generate(7L,input).get("status"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_ai_plan_draft",Integer.class));verifyNoInteractions(history);assertEquals(0,count());
    }
    private int count(){return jdbc.queryForObject("SELECT COUNT(*) FROM t_training_plan_item",Integer.class);}
}
