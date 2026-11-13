package com.runwise;
import com.runwise.common.*;
import com.runwise.entity.User;
import com.runwise.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.baomidou.mybatisplus.core.conditions.Wrapper;

class WechatBindingTest {
    static class VerifiedWechat extends UserServiceImpl {
        VerifiedWechat(){super(mock(JwtUtils.class),mock(StringRedisTemplate.class));}
        @Override protected String getOpenid(String code){return "verified-openid";}
    }
    User user(String openid){User u=new User();u.setId(7L);u.setStatus(1);u.setOpenid(openid);return u;}
    @Test void bindingExistingWebUserUpdatesIdentityWithoutCreatingOrMovingData(){
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"binding-test"),User.class);
        VerifiedWechat service=spy(new VerifiedWechat());
        doReturn(user("web:placeholder")).when(service).getById(7L);
        doReturn(null).when(service).getByOpenid("verified-openid");
        doReturn(true).when(service).update(any(Wrapper.class));
        service.bindWechat(7L,"one-time-test-code");
        verify(service).update(any(Wrapper.class));verify(service,never()).save(any(User.class));
    }
    @Test void conflictingOrAlreadyBoundIdentityCannotBeReplaced(){
        VerifiedWechat service=spy(new VerifiedWechat());
        doReturn(user("web:placeholder")).when(service).getById(7L);
        doReturn(user("verified-openid")).when(service).getByOpenid("verified-openid");
        assertThrows(BusinessException.class,()->service.bindWechat(7L,"test"));
        doReturn(user("other-wechat")).when(service).getById(7L);
        assertThrows(BusinessException.class,()->service.bindWechat(7L,"test"));
        verify(service,never()).update(any(Wrapper.class));
    }
}
