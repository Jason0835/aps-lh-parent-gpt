package com.zlt.aps.lh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanVo;

import java.util.List;

/**
 * 硫化精度计划Service接口
 *
 * @author APS Team
 */
public interface ILhPrecisionPlanService extends IService<LhPrecisionPlan> {

    /**
     * 查询硫化精度计划列表
     *
     * @param vo 查询条件
     * @return 计划列表
     */
    List<LhPrecisionPlan> selectLhPrecisionPlanList(LhPrecisionPlanVo vo);

    /**
     * 从MES同步数据生成硫化精度初版计划
     *
     * @return 生成数量
     */
    int generatePlansFromMes();

    /**
     * 自动生成年度硫化精度计划
     *
     * @param year 年份
     * @return 生成数量
     */
    int autoGenerateYearlyPlans(Integer year);

    /**
     * 执行30天预警检查
     *
     * @return 预警数量
     */
    int checkWarning();

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
     * @param actualDate 实际日期
     * @return 是否成功
     */
    boolean updateActualDate(Long mesSourceId, String actualDate);
}
