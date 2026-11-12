package com.runwise.admin;

import com.runwise.common.BusinessException;
import com.runwise.entity.InjuryArticle;
import com.runwise.mapper.InjuryArticleMapper;
import com.runwise.service.impl.InjuryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminArticleTest {
    @Test void unpublishedArticleCannotBeReadThroughPublicDetail() {
        InjuryArticleMapper mapper=mock(InjuryArticleMapper.class);
        InjuryServiceImpl service=new InjuryServiceImpl(new ObjectMapper());
        ReflectionTestUtils.setField(service,"baseMapper",mapper);
        InjuryArticle article=new InjuryArticle();article.setId(1L);article.setStatus("DRAFT");
        when(mapper.selectById(1L)).thenReturn(article);
        assertThrows(BusinessException.class,()->service.getArticle(1L));
        article.setStatus("OFFLINE");assertThrows(BusinessException.class,()->service.getArticle(1L));
        article.setStatus("PUBLISHED");assertEquals(1L,service.getArticle(1L).getId());
    }
    @Test void staleRevisionDoesNotWriteAuditOrReturnSuccess() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        AdminArticleService service=new AdminArticleService(jdbc,new ObjectMapper());
        AdminRequests.Status request=new AdminRequests.Status();request.setStatus("PUBLISHED");request.setRevision(1);
        AdminAccount account=new AdminAccount();account.setId(1L);
        assertThrows(BusinessException.class,()->service.status(1L,request,account));
        verify(jdbc,times(1)).update(contains("AND revision=?"),eq("PUBLISHED"),eq(1L),eq(1L),eq(1));
        verifyNoMoreInteractions(jdbc);
    }
}
