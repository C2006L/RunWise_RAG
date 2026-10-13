package com.runwise.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.runwise.entity.InjuryArticle;

import java.util.List;

public interface InjuryService extends IService<InjuryArticle> {

    /**
     * 文章列表（category 为空或"全部"时返回全部）
     */
    List<InjuryArticle> listArticles(String category);

    /**
     * 文章详情
     */
    InjuryArticle getArticle(Long id);
}