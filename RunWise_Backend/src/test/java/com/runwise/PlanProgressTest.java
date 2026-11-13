package com.runwise;
import com.runwise.service.PlanService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlanProgressTest {
    JdbcTemplate jdbc;PlanService plans;LocalDate today=LocalDate.now();
    @BeforeEach void setup(){
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:progress;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE","sa",""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE t_training_plan_item(id BIGINT PRIMARY KEY,user_id BIGINT,training_date DATE,type VARCHAR(16),target_km DECIMAL(6,2),note VARCHAR(255))");
        jdbc.execute("CREATE TABLE t_checkin(id BIGINT PRIMARY KEY,user_id BIGINT,checkin_date DATE,distance DECIMAL(6,2),deleted INT)");
        plans=new PlanService(jdbc);
        jdbc.update("INSERT INTO t_training_plan_item VALUES (1,7,?,'interval',5,'test')",today);
    }
    Map<String,Object> day(LocalDate date){
        for(Map<String,Object> week:plans.weeks(7L)) for(Object item:(List<?>)week.get("days")) {
            Map<String,Object> day=(Map<String,Object>)item;if(date.toString().equals(day.get("date"))) return day;
        }
        throw new AssertionError("date missing");
    }
    @Test void todayAloneDoesNotMeanTrainingHasStarted(){assertEquals("planned",day(today).get("status"));}
    @Test void progressUsesOwnedActiveRecordsAndReactsToEditAndDelete(){
        jdbc.update("INSERT INTO t_checkin VALUES (1,8,?,20,0),(2,7,?,20,1)",today,today);
        assertEquals("planned",day(today).get("status"));
        jdbc.update("INSERT INTO t_checkin VALUES (3,7,?,3,0)",today);
        assertEquals("in_progress",day(today).get("status"));
        jdbc.update("UPDATE t_checkin SET distance=5 WHERE id=3");
        assertEquals("distance_met",day(today).get("status"));
        assertTrue(day(today).get("statusNote").toString().contains("不等同于任务完成"));
        jdbc.update("UPDATE t_checkin SET deleted=1 WHERE id=3");
        assertEquals("planned",day(today).get("status"));
    }
    @Test void futureRecordsAndRestDaysNeverBecomeCompletedTasks(){
        LocalDate future=today.plusDays(1);
        jdbc.update("INSERT INTO t_training_plan_item VALUES (2,7,?,'easy',5,'')",future);
        jdbc.update("INSERT INTO t_checkin VALUES (1,7,?,10,0)",future);
        assertEquals("planned",day(future).get("status"));
        jdbc.update("UPDATE t_training_plan_item SET type='rest',target_km=0 WHERE id=1");
        jdbc.update("INSERT INTO t_checkin VALUES (2,7,?,10,0)",today);
        assertEquals("rest",day(today).get("status"));
    }
}
