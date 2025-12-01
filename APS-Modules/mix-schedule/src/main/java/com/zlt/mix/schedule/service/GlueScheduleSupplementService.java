package com.zlt.mix.schedule.service;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleSupplement;

/**
 * 生产补量服务
 *
 */
public interface GlueScheduleSupplementService {
    /**
     * 计算生产补量计划
     *
     * @param glueScheduleSupplement 终炼/母炼日计划排程
     * @return 生产补量计划集合
     */
    List<GlueScheduleSupplement> caculateSuppliment(GlueScheduleSupplement glueScheduleSupplement);
    
    /**
     * 保存生产补量记录
     * @param supplement 待保存的生产补量记录
     * @return
     */
    AjaxResult saveSupplement(List<GlueScheduleSupplement> glueScheduleSupplementList);
    
    /**
     * 生产补量列表
     *
     * @param glueScheduleSupplement
     * @return
     */
    List<GlueScheduleSupplement> listGlueScheduleSupplement(GlueScheduleSupplement glueScheduleSupplement);
}
