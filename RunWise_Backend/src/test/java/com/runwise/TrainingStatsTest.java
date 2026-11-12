package com.runwise;
import com.runwise.common.TrainingStats;
import com.runwise.entity.Checkin;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TrainingStatsTest {
    private final LocalDate today=LocalDate.of(2026,10,25);
    @Test void gapsMustStopTheStreak() {
        assertEquals(1,TrainingStats.streak(Arrays.asList(today,today.minusDays(2),today.minusDays(4)),today));
    }
    @Test void yesterdayIsOnlyAnAllowedStartingPoint() {
        assertEquals(2,TrainingStats.streak(Arrays.asList(today.minusDays(1),today.minusDays(2),today.minusDays(4)),today));
        assertEquals(0,TrainingStats.streak(Collections.singletonList(today.minusDays(2)),today));
    }
    @Test void crossYearDuplicatesAndFutureDates() {
        LocalDate jan=LocalDate.of(2026,1,1);
        List<LocalDate> dates=Arrays.asList(jan,jan,jan.minusDays(1),jan.minusDays(2),jan.plusDays(1));
        assertEquals(3,TrainingStats.streak(dates,jan));assertEquals(3,TrainingStats.longest(dates,jan));
    }
    @Test void leapDayIsContinuous() {
        LocalDate march=LocalDate.of(2024,3,1);
        assertEquals(3,TrainingStats.streak(Arrays.asList(march,march.minusDays(1),march.minusDays(2)),march));
    }
    @Test void chartTotalExactlyMatchesTheSeries() {
        Checkin a=new Checkin();a.setCheckinDate(today);a.setDistance(new BigDecimal("5.25"));
        Checkin b=new Checkin();b.setCheckinDate(today.minusDays(6));b.setDistance(new BigDecimal("4.12"));
        Checkin outside=new Checkin();outside.setCheckinDate(today.minusDays(7));outside.setDistance(new BigDecimal("99"));
        Map<String,Object> result=TrainingStats.period(Arrays.asList(a,b,outside),today.minusDays(6),today,"weekDates");
        assertEquals(new BigDecimal("9.37"),result.get("totalKm"));assertEquals(2,result.get("checkinCount"));
        assertEquals(7,((List<?>)result.get("distances")).size());
    }
}
