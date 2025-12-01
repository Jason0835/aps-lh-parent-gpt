package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 汇总胶料需求计划Mapper接口
 *
 * @author chen
 * @date 2022-04-25
 */
public interface GlueCollectPlanMapper extends BaseMapper<GlueCollectPlan> {

    /**
     * 查询汇总胶料需求计划列表
     *
     * @param glueCollectPlan 汇总胶料需求计划
     * @return 汇总胶料需求计划集合
     */
    List<GlueCollectPlan> selectGlueCollectPlanList(GlueCollectPlan glueCollectPlan);

    /**
     * 批量删除汇总胶料需求计划
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueCollectPlanByIds(Long[] ids);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertGlueCollectPlanInfo(@Param("list") List<GlueCollectPlan> list);

    /**
     * 同某一天的汇总计划到日志表中
     * @param planDate
     */
    void syncCollectPlanToLog(@Param("planDate") Date planDate);

    /**
     * 删除某一天全部的汇总计划
     * @param planDate 计划日期
     */
    void deleteCollectPlan(@Param("planDate") Date planDate);

    /**
     * 汇总胶料计划
     * @param batchNo  批次号
     * @param planDate  计划日期
     * @param createBy 操作员工
     */
    void summaryBasePlan(@Param("batchNo") String batchNo, @Param("planDate") Date planDate, @Param("createBy") String createBy);

    /**
     * 匹配终炼胶的机台
     * @param planDate 计划日期
     */
    void matchMachine(@Param("planDate") Date planDate);

    /**
     * 匹配终炼胶的特殊一次法机台
     * @param planDate 计划日期
     */
    void matchSpecialMachine(@Param("planDate") Date planDate);

    /**
     * 结合计划日期进行统计密炼区数据异常记录数
     *
     * @param planDateStr yyyyMMdd
     * @return
     */
    int countOfMixAreaException(@Param("planDate") String planDateStr);

    /**
     * 更新昨日剩余和生产量
     *
     * @param planDate 计划日期
     * @param isAddLastSurplus 生产量是否需要加上昨日剩余量。0：需要
     */
    void lastSurplusPlan(@Param("planDate") Date planDate, @Param("isAddLastSurplus") String isAddLastSurplus);
    
    /**
     * 重算白班待支领量
     * @param scheduleDate	计划日期
     */
    void recaculateGlueUnclaimed(@Param("planDate") Date planDate);
}
