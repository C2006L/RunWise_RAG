package com.runwise;
import com.runwise.common.BusinessException;
import com.runwise.dto.CheckinDTO;
import com.runwise.entity.Checkin;
import com.runwise.mapper.CheckinMapper;
import com.runwise.service.impl.CheckinServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.math.BigDecimal;

class CheckinBoundaryTest {
    @Test void futureCheckinsAreRejectedBeforeDatabaseWrite() {
        CheckinServiceImpl service=new CheckinServiceImpl();CheckinMapper mapper=mock(CheckinMapper.class);ReflectionTestUtils.setField(service,"baseMapper",mapper);
        CheckinDTO input=new CheckinDTO();input.setCheckinDate(LocalDate.now().plusDays(1).toString());input.setDistance(BigDecimal.ONE);input.setDuration(5);
        assertThrows(BusinessException.class,()->service.createCheckin(7L,input));verifyNoInteractions(mapper);
    }
    @Test void cannotEditAnotherUsersCheckin() {
        CheckinServiceImpl service=new CheckinServiceImpl();CheckinMapper mapper=mock(CheckinMapper.class);ReflectionTestUtils.setField(service,"baseMapper",mapper);
        Checkin record=new Checkin();record.setId(1L);record.setUserId(8L);when(mapper.selectById(1L)).thenReturn(record);
        assertThrows(BusinessException.class,()->service.updateCheckin(7L,1L,new CheckinDTO()));verify(mapper,never()).updateById(any());
    }
}
