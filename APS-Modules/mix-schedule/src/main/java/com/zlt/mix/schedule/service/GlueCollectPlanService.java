package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.schedule.api.domain.dto.GlueCollectPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;

import java.util.List;

/**
 * 汇总胶料需求计划Service接口
 *
 * @author chen
 * @date 2022-04-25
 */
public interface GlueCollectPlanService extends IService<GlueCollectPlan> {
    /**
     * 查询汇总胶料需求计划列表
     *
     * @param glueCollectPlan 汇总胶料需求计划
     * @return 汇总胶料需求计划集合
     */
    List<GlueCollectPlan> selectGlueCollectPlanList(GlueCollectPlan glueCollectPlan);

    /**
     * 保存汇总胶料需求计划信息（id为空则新增，id不为空则修改）
     *
     * @param glueCollectPlan
     */
    void saveGlueCollectPlan(GlueCollectPlan glueCollectPlan);

    /**
     * 批量删除汇总胶料需求计划
     *
     * @param ids 需要删除的汇总胶料需求计划ID
     * @return 结果
     */
    int deleteGlueCollectPlanByIds(Long[] ids);

    /**
     * 汇总计划(先备份数据，在删掉当前数据，最后在重新汇总最新的数据到表中)
     * @param glueCollectPlan
     */
    void summaryPlan(GlueCollectPlan glueCollectPlan);

    /**
     * 验证分厂计划中密炼区域数据
     *
     * @param glueCollectPlan
     * @return
     */
    String validateMixAreaData(GlueCollectPlan glueCollectPlan);

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    byte[] exportData(GlueCollectPlanExportDictDto dto);

    /**
     * 检测对应日期的数据是否存在
     *
     * @param glueCollectPlan 日期
     * @return 是否唯一的常量值
     */
    String checkPlanDateExist(GlueCollectPlan glueCollectPlan);
}
