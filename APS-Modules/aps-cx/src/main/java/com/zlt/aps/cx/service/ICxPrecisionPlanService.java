package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
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

    /**
     * 从MES同步数据生成成型精度初版计划
     *
     * @param year 年份（为空时使用当前年）
     * @return 生成数量
     */
    int generatePlansFromMes(Integer year);

    /**
     * 自动生成年度成型精度计划
     *
     * @param year 年份
     * @return 生成数量
     */
    int autoGenerateYearlyPlans(Integer year);

    /**
     * 批量更新到期天数
     *
     * @return 更新数量
     */
    int batchUpdateDaysToDue();

    /**
     * MES回传实际完成时间
     *
     * @param mesSourceId MES来源ID
     * @param actualDate  实际日期
     * @return 是否成功
     */
    boolean updateActualDate(Long mesSourceId, String actualDate);

    /**
     * 根据设备保养计划生成成型精度计划
     *
     * @param maintenancePlans 设备保养计划列表
     * @param cycleDays 周期天数（15/60）
     * @return 生成数量
     */
    int generateFromMaintenancePlan(List<MdmDevMaintenancePlan> maintenancePlans, Integer cycleDays);

    /**
     * 按周期自动生成成型精度计划
     *
     * @param year 年度
     * @param cycleDays 周期天数（15/60）
     * @return 生成数量
     */
    int autoGenerateByCycle(Integer year, Integer cycleDays);
}
