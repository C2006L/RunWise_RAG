package com.runwise;

import com.runwise.entity.InjuryArticle;
import com.runwise.service.impl.InjuryServiceImpl;
import com.runwise.common.BusinessException;
import com.runwise.controller.InjuryController;
import com.runwise.common.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Collections;

class InjuryReadTest {
    private InjuryArticle article() {
        InjuryArticle a=new InjuryArticle();a.setId(1L);a.setCategory("knee");a.setTitle("test");
        a.setStatus("PUBLISHED");a.setTags("跑步, 恢复, ,");a.setSections("[{\"heading\":\"标题\",\"body\":\"正文\"}]");return a;
    }
    @Test void threePublicReadRoutesReturnFrontendArraysAndCategoryLabels() throws Exception {
        InjuryServiceImpl service=spy(new InjuryServiceImpl(new ObjectMapper()));
        doReturn(article()).when(service).getById(1L);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"injury-test"),InjuryArticle.class);
        doReturn(Collections.singletonList(article())).when(service).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new InjuryController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/injury/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].key").value("knee"));
        mvc.perform(get("/api/injury/articles?category=knee")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].categoryLabel").value("膝部"));
        mvc.perform(get("/api/injury/articles/1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tags[1]").value("恢复")).andExpect(jsonPath("$.data.sections[0].body").value("正文"));
    }
    @Test void unpublishedAndOfflineDetailsAreNotReadable() {
        InjuryServiceImpl service=spy(new InjuryServiceImpl(new ObjectMapper()));
        for(String state:new String[]{"DRAFT","OFFLINE"}) {
            InjuryArticle a=article();a.setStatus(state);doReturn(a).when(service).getById(1L);
            assertThrows(BusinessException.class,()->service.getArticle(1L));
        }
    }
    @Test void corruptSectionsAreRejectedInsteadOfReturningFakeContent() {
        InjuryServiceImpl service=spy(new InjuryServiceImpl(new ObjectMapper()));
        InjuryArticle a=article();a.setSections("invalid json");doReturn(a).when(service).getById(1L);
        assertThrows(BusinessException.class,()->service.getArticle(1L));
    }
}
