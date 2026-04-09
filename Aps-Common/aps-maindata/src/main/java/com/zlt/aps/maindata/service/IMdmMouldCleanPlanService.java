package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanPlan;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * APS模具清洗计划Service接口
 *
 * @author APS Team
 */
public interface IMdmMouldCleanPlanService extends IDocService<MdmMouldCleanPlan> {

    /**
     * 从模具清洗预警同步生成模具清洗计划
     *
     * @return 生成数量
     */
    int syncFromMouldCleanWarn();

    /**
     * 导入数据
     *
     * @param list 数据列表
     * @param updateSupport 是否更新已存在数据
     * @param importLogId 导入日志ID
     * @return 结果
     */
    AjaxResult importData(List<MdmMouldCleanPlan> list, boolean updateSupport, Long importLogId);

    /**
     * 校验唯一性
     *
     * @param entity 实体
     * @return 结果
     */
    String checkUnique(MdmMouldCleanPlan entity);
}
