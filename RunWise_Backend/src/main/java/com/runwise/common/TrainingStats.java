package com.runwise.common;

import com.runwise.entity.Checkin;
import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;

/** 所有统计均从有效打卡记录计算；duration/pace 的既有存储单位分别为分钟、分钟/km。 */
public final class TrainingStats {
    private TrainingStats() { }
    public static int streak(Collection<LocalDate> dates,LocalDate today) {
        Set<LocalDate> set=new HashSet<>(dates);
        LocalDate cursor=set.contains(today)?today:today.minusDays(1);
        int count=0;
        while(set.contains(cursor)) {count++;cursor=cursor.minusDays(1);}
        return count;
    }
    public static int longest(Collection<LocalDate> dates,LocalDate today) {
        SortedSet<LocalDate> sorted=new TreeSet<>();
        for(LocalDate date:dates) if(!date.isAfter(today)) sorted.add(date);
        LocalDate previous=null;int current=0,best=0;
        for(LocalDate date:sorted) {
            current=previous!=null && previous.plusDays(1).equals(date)?current+1:1;
            best=Math.max(best,current);previous=date;
        }
        return best;
    }
    public static Map<String,Object> period(List<Checkin> records,LocalDate start,LocalDate end,String datesKey) {
        Map<LocalDate,BigDecimal> byDate=new HashMap<>();int count=0;long minutes=0;
        List<LocalDate> allDates=new ArrayList<>();
        for(Checkin record:records) {
            LocalDate date=record.getCheckinDate();allDates.add(date);
            if(!date.isBefore(start)&&!date.isAfter(end)) {
                byDate.merge(date,record.getDistance(),BigDecimal::add);count++;
                if(record.getDuration()!=null) minutes+=record.getDuration();
            }
        }
        List<String> dates=new ArrayList<>();List<BigDecimal> distances=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;
        for(LocalDate date=start;!date.isAfter(end);date=date.plusDays(1)) {
            BigDecimal km=byDate.getOrDefault(date,BigDecimal.ZERO);
            dates.add(date.toString());distances.add(km);total=total.add(km);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put(datesKey,dates);result.put("distances",distances);
        result.put("totalKm",total);result.put("totalMinutes",minutes);
        result.put("averagePaceMinPerKm",total.signum()>0?BigDecimal.valueOf(minutes).divide(total,6,java.math.RoundingMode.HALF_UP):null);
        result.put("checkinCount",count);result.put("streakDays",streak(allDates,end));return result;
    }
}
