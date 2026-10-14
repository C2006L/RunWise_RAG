package com.runwise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runwise.common.BusinessException;
import com.runwise.common.ResultCode;
import com.runwise.entity.InjuryArticle;
import com.runwise.mapper.InjuryArticleMapper;
import com.runwise.service.InjuryService;
import com.runwise.vo.InjuryArticleVO;
import com.runwise.vo.InjuryCategoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InjuryServiceImpl extends ServiceImpl<InjuryArticleMapper, InjuryArticle> implements InjuryService {

    private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();

    static {
        CATEGORY_LABELS.put("knee", "膝部");
        CATEGORY_LABELS.put("ankle", "足踝");
        CATEGORY_LABELS.put("shin", "小腿");
    }

    private final ObjectMapper objectMapper;

    public InjuryServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<InjuryArticleVO> listArticles(String category) {
        LambdaQueryWrapper<InjuryArticle> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty() && !"全部".equals(category)) {
            wrapper.eq(InjuryArticle::getCategory, category);
        }
        // 列表不返回正文四节，减小传输体积；详情接口返回全文
        wrapper.select(InjuryArticle::getId, InjuryArticle::getCategory, InjuryArticle::getTitle,
                InjuryArticle::getSummary, InjuryArticle::getTags, InjuryArticle::getReadMinutes,
                InjuryArticle::getSort, InjuryArticle::getCreateTime, InjuryArticle::getUpdateTime);
        wrapper.orderByAsc(InjuryArticle::getSort);
        return list(wrapper).stream()
                .map(article -> toVO(article, false))
                .collect(Collectors.toList());
    }

    @Override
    public InjuryArticleVO getArticle(Long id) {
        InjuryArticle article = getById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        return toVO(article, true);
    }

    @Override
    public List<InjuryCategoryVO> listCategories() {
        List<InjuryCategoryVO> categories = new ArrayList<>();
        CATEGORY_LABELS.forEach((key, label) -> categories.add(new InjuryCategoryVO(key, label)));
        return categories;
    }

    private InjuryArticleVO toVO(InjuryArticle article, boolean includeSections) {
        InjuryArticleVO result = new InjuryArticleVO();
        result.setId(article.getId());
        result.setCategory(article.getCategory());
        result.setCategoryLabel(CATEGORY_LABELS.getOrDefault(article.getCategory(), article.getCategory()));
        result.setTitle(article.getTitle());
        result.setSummary(article.getSummary());
        result.setTags(parseTags(article.getTags()));
        result.setSections(includeSections ? parseSections(article.getSections()) : null);
        result.setReadMinutes(article.getReadMinutes());
        result.setSort(article.getSort());
        result.setCreateTime(article.getCreateTime());
        result.setUpdateTime(article.getUpdateTime());
        return result;
    }

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    private List<InjuryArticleVO.Section> parseSections(String rawSections) {
        if (rawSections == null || rawSections.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    rawSections,
                    new TypeReference<List<InjuryArticleVO.Section>>() { });
        } catch (JsonProcessingException e) {
            log.error("伤病文章 sections JSON 解析失败, 原始内容={}", rawSections, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "文章正文格式错误");
        }
    }
}
