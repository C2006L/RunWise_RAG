package com.runwise;
import com.runwise.service.RunnerContextService;
import com.runwise.entity.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RunnerContextTest {
    private Checkin record(long id,LocalDate date,String km,int minutes){Checkin row=new Checkin();row.setId(id);row.setCheckinDate(date);row.setDistance(new BigDecimal(km));row.setDuration(minutes);return row;}
    @Test void usesWeightedPaceAndFourRealWindowsWithoutInventingMissingTraining(){
        LocalDate today=LocalDate.of(2026,10,27);User user=new User();user.setRunningLevel("beginner");
        List<Checkin> rows=Arrays.asList(record(1,today,"2",20),record(2,today.minusDays(8),"8",40),record(3,today.plusDays(1),"100",100));
        Map<String,Object> result=RunnerContextService.summarize(rows,user,today,Collections.emptyList());
        assertEquals(2,result.get("recordCount"));assertEquals(new BigDecimal("10"),result.get("totalKm"));
        assertEquals(new BigDecimal("6.00"),result.get("averagePaceMinPerKm"));assertEquals(new BigDecimal("2.50"),result.get("weeklyAverageKm"));
        assertEquals(4,((List<?>)result.get("weeks")).size());assertNull(result.get("weeklyFrequencyGoal"));
        rows.get(0).setDuration(21);
        assertNotEquals(result.get("recordVersion"),RunnerContextService.summarize(rows,user,today,Collections.emptyList()).get("recordVersion"));
    }
    @Test void emptyRecordsProduceMissingDataRatherThanExampleValues(){
        Map<String,Object> result=RunnerContextService.summarize(Collections.emptyList(),new User(),LocalDate.now(),Collections.emptyList());
        assertEquals(0,result.get("recordCount"));assertNull(result.get("averagePaceMinPerKm"));assertNull(result.get("lastTrainingDate"));
    }
}
