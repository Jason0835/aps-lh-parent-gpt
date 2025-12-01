package com.zlt.aps.monthplan.mdm.service;


import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.EstimateExceedShort;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IEstimateExceedShortService.java
 * 描    述：IEstimateExceedShortService预计超欠产后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-18
 */
public interface IEstimateExceedShortService {
    /**
     * 根据年份，月份获取欠产量数据
     * 欠产量 < 0的数据
     *
     * @param year
     * @param month
     * @return
     */
    List<EstimateExceedShort> getEstimateExceedShortByYearAndMonth(Integer year, Integer month);

    /**
     * 查询超欠产列表
     */
    List<EstimateExceedShort> selectEstimateExceedShortList(EstimateExceedShort query);

    /**
     * 校验唯一性
     */
    String checkUnique(EstimateExceedShort tEstimateExceedShort);

    /**
     * 保存预计超欠产管理数据
     */
    int save(EstimateExceedShort billVO);

    /**
     * 根据ID列表删除
     */
    int removeByIds(List<Long> ids);

    /**
     * 获取预计超欠产管理数据
     */
    EstimateExceedShort getInfo(Long billId);
    
    /**
     * 导入预计超欠产管理数据
     */
    AjaxResult importData(List<EstimateExceedShort> list, boolean updateSupport, Long importLogId);

    /**
     * 导入预计超欠产管理数据
     */
    void importDataAsync(List<EstimateExceedShort> list, boolean updateSupport, Long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes);

    /**
     * 修改预计超欠数
     */
    int updateExceedShortQty(EstimateExceedShort estimateExceedShort);

    /**
     * 设置品号、寸口
     */
    void setProductInfo(List<EstimateExceedShort> resultList);
}
