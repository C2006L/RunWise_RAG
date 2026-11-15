package com.runwise;
import com.runwise.common.TrainingStats;
import com.runwise.entity.Checkin;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WeeklyPaceTest {
    @Test void paceIsDistanceWeightedAndLimitedToSelectedWindow() {
        LocalDate today=LocalDate.now();
        List<Checkin> rows=new ArrayList<>();
        for(int i=0;i<3;i++) {
            Checkin row=new Checkin();row.setCheckinDate(today.minusDays(i==2?8:i));
            row.setDistance(new BigDecimal(i==0?"5":"10"));row.setDuration(i==0?30:50);rows.add(row);
        }
        Map<String,Object> result=TrainingStats.period(rows,today.minusDays(6),today,"weekDates");
        assertEquals(80L,result.get("totalMinutes"));
        assertEquals(0,new BigDecimal("15").compareTo((BigDecimal)result.get("totalKm")));
        assertEquals(0,new BigDecimal("5.333333").compareTo((BigDecimal)result.get("averagePaceMinPerKm")));
        assertNull(TrainingStats.period(Collections.emptyList(),today,today,"dates").get("averagePaceMinPerKm"));
    }
}
