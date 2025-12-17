package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.I18nChange;
import com.zlt.aps.monthplan.api.domain.vo.I18nJsonVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 国际化变更记录Service
 */
public interface I18nChangeService {

    /**
     * 加载页面需要的国际化
     */
    void loadPageBundle();

    /**
     * 查询国际化列表
     */
    List<I18nChange> selectList(I18nChange query);

    /**
     * 保存更新
     */
    AjaxResult save(I18nChange change);

    /**
     * 根据id查询
     */
    I18nChange getInfo(Long id);

    /**
     * 查询页面JSON
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult pageJson(I18nJsonVo jsonVo);

    /**
     * 下载国际化
     */
    byte[] download();

    /**
     * 加载通用的国际化到数据库
     */
    void loadCommonBundle();

    /**
     * 加载系统的国际化到数据库
     */
    void loadSystemBundle();

    /**
     * 加载ui加载的框架国际化到数据库
     */
    void loadUiBaseBundle();

    /**
     * 初始化页面多语言的第一级结构
     */
    void initPageJsonFirstElement();
}
