package com.runwise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.runwise.common.BusinessException;
import com.runwise.common.ResultCode;
import com.runwise.entity.InjuryArticle;
import com.runwise.mapper.InjuryArticleMapper;
import com.runwise.service.InjuryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class InjuryServiceImpl extends ServiceImpl<InjuryArticleMapper, InjuryArticle> implements InjuryService {

    @Override
    public List<InjuryArticle> listArticles(String category) {
        LambdaQueryWrapper<InjuryArticle> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty() && !"全部".equals(category)) {
            wrapper.eq(InjuryArticle::getCategory, category);
        }
        // 列表不返回正文四节，减小传输体积；详情接口返回全文
        wrapper.select(InjuryArticle::getId, InjuryArticle::getCategory, InjuryArticle::getTitle,
                InjuryArticle::getSummary, InjuryArticle::getReadMinutes, InjuryArticle::getSort,
                InjuryArticle::getCreateTime);
        wrapper.orderByAsc(InjuryArticle::getSort);
        return list(wrapper);
    }

    @Override
    public InjuryArticle getArticle(Long id) {
        InjuryArticle article = getById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        return article;
    }
}