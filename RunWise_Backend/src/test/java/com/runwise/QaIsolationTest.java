package com.runwise;
import com.runwise.service.impl.QaServiceImpl;
import com.runwise.entity.QaRecord;
import com.runwise.dto.AskDTO;
import com.runwise.common.BusinessException;
import com.runwise.mapper.QaRecordMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class QaIsolationTest {
    static class RecordingService extends QaServiceImpl {
        LambdaQueryWrapper<QaRecord> query;QaRecord saved;List<QaRecord> records=new ArrayList<>();
        @Override public List<QaRecord> list(Wrapper<QaRecord> query) {this.query=(LambdaQueryWrapper<QaRecord>)query;return new ArrayList<>(records);}
        @Override public boolean save(QaRecord record) {record.setId(99L);saved=record;return true;}
    }
    @Test void contextQueriesAndStoredRecordsAreScopedToUserAndConversation() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"test"),QaRecord.class);
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        java.util.concurrent.atomic.AtomicReference<String> posted=new java.util.concurrent.atomic.AtomicReference<>();
        server.createContext("/api/rag/ask",exchange->{
            posted.set(cn.hutool.core.io.IoUtil.read(exchange.getRequestBody(),StandardCharsets.UTF_8));
            byte[] answer="{\"answer\":\"test answer\",\"sources\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,answer.length);exchange.getResponseBody().write(answer);exchange.close();
        });server.start();
        try {
            RecordingService service=new RecordingService();ReflectionTestUtils.setField(service,"ragEnabled",true);
            com.runwise.service.RunnerContextService context=mock(com.runwise.service.RunnerContextService.class);
            when(context.build(42L)).thenReturn(Collections.singletonMap("recordCount",8));
            ReflectionTestUtils.setField(service,"runnerContext",context);
            ReflectionTestUtils.setField(service,"ragBaseUrl","http://127.0.0.1:"+server.getAddress().getPort());
            AskDTO input=new AskDTO();input.setQuestion("test question");input.setConversationId("session-a");
            Map<String,Object> answer=service.ask(42L,input);
            assertEquals(8,cn.hutool.json.JSONUtil.parseObj(posted.get()).getJSONObject("trainingContext").getInt("recordCount"));
            String sql=service.query.getSqlSegment();Collection<Object> params=service.query.getParamNameValuePairs().values();
            assertTrue(sql.contains("user_id"));assertTrue(sql.contains("conversation_id"));assertTrue(params.contains(42L));assertTrue(params.contains("session-a"));
            assertEquals(42L,service.saved.getUserId());assertEquals("session-a",service.saved.getConversationId());assertEquals(99L,answer.get("id"));
            input.setConversationId(null);String first=(String)service.ask(42L,input).get("conversationId");String second=(String)service.ask(42L,input).get("conversationId");
            assertNotEquals(first,second);
        } finally {server.stop(0);}
    }
    @Test void cannotChangeFeedbackOnAnotherUsersRecord() {
        QaServiceImpl service=new QaServiceImpl();QaRecordMapper mapper=mock(QaRecordMapper.class);ReflectionTestUtils.setField(service,"baseMapper",mapper);
        QaRecord record=new QaRecord();record.setUserId(8L);when(mapper.selectById(1L)).thenReturn(record);
        assertThrows(BusinessException.class,()->service.feedback(7L,1L,1));verify(mapper,never()).updateById(any());
    }

    private RecordingService service(int port) {
        RecordingService service=new RecordingService();
        ReflectionTestUtils.setField(service,"ragEnabled",true);
        ReflectionTestUtils.setField(service,"ragBaseUrl","http://127.0.0.1:"+port);
        com.runwise.service.RunnerContextService context=mock(com.runwise.service.RunnerContextService.class);
        when(context.build(42L)).thenReturn(Collections.singletonMap("recordCount",8));
        ReflectionTestUtils.setField(service,"runnerContext",context);
        return service;
    }

    private AskDTO question() {
        AskDTO input=new AskDTO();input.setQuestion("继续分析");input.setConversationId("session-a");return input;
    }

    @Test void longHistoricalAnswersAreExcerptedOnlyForTransportAndRemainStoredInFull() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        java.util.concurrent.atomic.AtomicReference<String> posted=new java.util.concurrent.atomic.AtomicReference<>();
        server.createContext("/api/rag/ask",exchange->{
            posted.set(cn.hutool.core.io.IoUtil.read(exchange.getRequestBody(),StandardCharsets.UTF_8));
            byte[] answer="{\"answer\":\"complete\",\"sources\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,answer.length);exchange.getResponseBody().write(answer);exchange.close();
        });server.start();
        try {
            RecordingService service=service(server.getAddress().getPort());
            String original=String.join("",Collections.nCopies(5000,"跑\uD83C\uDFC3"));
            QaRecord old=new QaRecord();old.setQuestion("上一问");old.setAnswer(original);service.records.add(old);
            service.ask(42L,question());
            String sent=cn.hutool.json.JSONUtil.parseObj(posted.get()).getJSONArray("history").getJSONObject(0).getStr("assistant");
            assertTrue(sent.codePointCount(0,sent.length())<=4000);assertTrue(sent.endsWith("[历史上下文已节选]"));
            assertEquals(original,old.getAnswer());
        } finally {server.stop(0);}
    }

    @Test void upstreamModelErrorDoesNotFallBackOrWriteHistoryEvenWhenLocalFallbackIsEnabled() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        java.util.concurrent.atomic.AtomicInteger localCalls=new java.util.concurrent.atomic.AtomicInteger();
        server.createContext("/api/rag/ask",exchange->{
            byte[] answer="{\"detail\":{\"code\":\"MODEL_TRUNCATED\",\"message\":\"secret API key\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503,answer.length);exchange.getResponseBody().write(answer);exchange.close();
        });
        server.createContext("/api/generate",exchange->{localCalls.incrementAndGet();exchange.sendResponseHeaders(500,-1);exchange.close();});server.start();
        try {
            RecordingService service=service(server.getAddress().getPort());
            ReflectionTestUtils.setField(service,"ollamaFallbackEnabled",true);
            ReflectionTestUtils.setField(service,"ollamaBaseUrl","http://127.0.0.1:"+server.getAddress().getPort());
            BusinessException error=assertThrows(BusinessException.class,()->service.ask(42L,question()));
            assertTrue(error.getMessage().contains("输出上限"));assertFalse(error.getMessage().contains("secret"));
            assertNull(service.saved);assertEquals(0,localCalls.get());
        } finally {server.stop(0);}
    }

    @Test void explicitLocalModeKeepsConversationContextAndRejectsTruncation() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        java.util.concurrent.atomic.AtomicReference<String> posted=new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean truncate=new java.util.concurrent.atomic.AtomicBoolean();
        server.createContext("/api/generate",exchange->{
            posted.set(cn.hutool.core.io.IoUtil.read(exchange.getRequestBody(),StandardCharsets.UTF_8));
            byte[] answer=("{\"done\":true,\"done_reason\":\""+(truncate.get()?"length":"stop")+"\",\"response\":\"## 建议\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,answer.length);exchange.getResponseBody().write(answer);exchange.close();
        });server.start();
        try {
            RecordingService service=service(server.getAddress().getPort());
            ReflectionTestUtils.setField(service,"ragEnabled",false);
            ReflectionTestUtils.setField(service,"ollamaBaseUrl","http://127.0.0.1:"+server.getAddress().getPort());
            ReflectionTestUtils.setField(service,"ollamaModel","test-local");
            QaRecord old=new QaRecord();old.setQuestion("周三不能跑步");old.setAnswer("建议周四训练");service.records.add(old);
            service.ask(42L,question());assertTrue(posted.get().contains("周三不能跑步"));
            service.saved=null;truncate.set(true);
            assertThrows(BusinessException.class,()->service.ask(42L,question()));assertNull(service.saved);
        } finally {server.stop(0);}
    }
}
