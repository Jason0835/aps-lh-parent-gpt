package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.CxPrecisionPlan;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 成型精度计划服务接口
 *
 * 文档CRUD：支持标准文档操作和导入导出
 *
 * @author APS Team
 */
public interface ICxPrecisionPlanService extends IDocService<CxPrecisionPlan> {

    /**
     * 校验唯一性
     * @param entity 成型精度计划实体
     * @return UserConstants.NOT_UNIQUE 不唯一，UserConstants.UNIQUE 唯一
     */
    String checkUnique(CxPrecisionPlan entity);

    /**
     * 导入数据
     * @param list 数据列表
     * @param updateSupport 是否更新已存在数据
     * @param importLogId 导入日志ID
     * @return 导入结果
     */
    AjaxResult importData(List<CxPrecisionPlan> list, boolean updateSupport, Long importLogId);
}
