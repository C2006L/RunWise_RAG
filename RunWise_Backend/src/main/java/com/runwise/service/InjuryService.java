package com.runwise.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.runwise.entity.InjuryArticle;
import com.runwise.vo.InjuryArticleVO;
import com.runwise.vo.InjuryCategoryVO;

import java.util.List;

public interface InjuryService extends IService<InjuryArticle> {

    /**
     * 文章列表（category 为空或"全部"时返回全部）
     */
    List<InjuryArticleVO> listArticles(String category);

    /**
     * 文章详情
     */
    InjuryArticleVO getArticle(Long id);

    /**
     * 前端筛选项。分类属于固定产品配置，不依赖文章表聚合。
     */
    List<InjuryCategoryVO> listCategories();
}
