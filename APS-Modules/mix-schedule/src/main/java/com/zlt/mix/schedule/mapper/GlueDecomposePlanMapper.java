package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 分解胶料需求量Mapper接口
 *
 * @author chen
 * @date 2022-05-04
 */
public interface GlueDecomposePlanMapper extends BaseMapper<GlueDecomposePlan> {

    /**
     * 查询分解胶料需求量列表
     *
     * @param glueDecomposePlan 分解胶料需求量
     * @return 分解胶料需求量集合
     */
    List<GlueDecomposePlan> selectGlueDecomposePlanList(GlueDecomposePlan glueDecomposePlan);

    /**
     * 批量删除分解胶料需求量
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueDecomposePlanByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueDecomposePlanNotUnique(@Param("importList") List<GlueDecomposePlan> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertGlueDecomposePlanInfo(@Param("list") List<GlueDecomposePlan> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<GlueDecomposePlan> list);

    /**
     * 根据计划日期、密炼区、胶料名称获取库存和安全库存
     * @param glueType 胶料类型：终炼 0 母联 1
     * @param planDate 计划日期
     * @param mixArea 密炼区
     * @param glue 胶料名称
     * @return 库存和安全库存
     */
    GlueDecomposePlan getStockAndSafeStock(@Param("glueType") String glueType, @Param("planDate") Date planDate, @Param("mixArea") String mixArea, @Param("glue") String glue);

    /**
     * 更新安全库存
     * @param glueDecomposePlan 要更新的数据
     * @return 结果
     */
    int mergeSafeStock(GlueDecomposePlan glueDecomposePlan);

    /**
     * 校验汇总数据中机台存在多个或者为空的数据记录
     * @param planDateStr yyyy-MM-dd
     * @param mixArea 密炼区
     * @return
     */
    int countOfMachineCodeException(@Param("planDate") String planDateStr, @Param("mixArea") String mixArea);

    /**
     * 根据id更新分解计划数据
     * @param list 要更新的数据
     * @return 影响行数
     */
    int mergeById(List<GlueDecomposePlan> list);

    /**
     * 获取胶料跨区发送设置表中的记录
     * @param planDate  计划日期
     * @param mixArea   委托密炼区
     * @return
     */
    List<GlueSpanSend> listGlueSpanSetting(@Param("planDate") Date planDate, @Param("mixArea") String mixArea);

    /**
     * 删除未被接收的接收记录
     * @param planDate
     * @param mixArea
     */
    void deleteAutoNotReceive(@Param("planDate") Date planDate, @Param("mixArea") String mixArea);

    /**
     * 删除未被接收的发送记录
     * @param planDate
     * @param mixArea
     */
    void deleteAutoNotReceiveSend(@Param("planDate") Date planDate, @Param("mixArea") String mixArea);

    /**
     * 批量设置分解后跨区接收表的默认机台
     * @param planDate
     * @param sendIdList
     */
    void matchGlueReceiveMachine(@Param("planDate") Date planDate, @Param("sendIdList") List<Long> sendIdList);

    /**
     * 批量设置分解后跨区接收表的默认特殊指定机台
     * @param planDate
     * @param sendIdList
     */
    void matchGlueReceiveSpecialMachine(@Param("planDate") Date planDate, @Param("sendIdList") List<Long> sendIdList);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    List<GlueDecomposePlan> selectSpanSendNeedFieldByIds(Long[] ids);
}
