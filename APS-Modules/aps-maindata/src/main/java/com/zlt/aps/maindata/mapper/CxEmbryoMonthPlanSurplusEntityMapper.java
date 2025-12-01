package com.zlt.aps.maindata.mapper;



import com.zlt.aps.monthplan.api.domain.entity.CxEmbryoMonthPlanSurplus;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxEmbryoMonthPlanSurplusMapper.java
 * 描    述：成型工序胎胚计划量汇总表Mapper接口
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface CxEmbryoMonthPlanSurplusEntityMapper extends CommBaseMapper<CxEmbryoMonthPlanSurplus> {

    /**
     * 根据唯一键组合批量查询
     * @param uniqueKeys 唯一键列表（格式：year_month_factoryCode_embryoCode_bomDataVersion）
     * @return 匹配的记录列表
     */
    List<CxEmbryoMonthPlanSurplus> selectByUniqueKeys(List<String> uniqueKeys);

    /**
     * 批量插入
     * @param list 待插入的记录列表
     */
    void batchInsert(List<CxEmbryoMonthPlanSurplus> list);

    /**
     * 批量更新月度计划量
     * @param list 待更新的记录列表
     */
    void batchUpdate(List<CxEmbryoMonthPlanSurplus> list);
}
