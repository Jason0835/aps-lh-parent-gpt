package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 硫化排程结果Mapper
 *
 * @author APS
 */
@Mapper
public interface LhScheduleResultMapper extends BaseMapper<LhScheduleResult> {

    /**
     * 批量插入排程结果
     *
     * @param list 排程结果列表
     * @return 插入记录数
     */
    int insertBatch(@Param("list") List<LhScheduleResult> list);

    List<LhScheduleResult> changeMachinePreCheck(@Param("ids") List<Long> ids,
                                                 @Param("newMachineCode") String newMachineCode);

    /**
     * 批量查询命中机台近7天最近一次收尾的前规格物料。
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 当前导出排程日期
     * @param machineCloseOutBeforeMap key=硫化机台编号，value=该机台当前早班开始时间
     * @return 每台机最近一次收尾的前规格物料
     */
    List<LhScheduleResult> selectLatestCloseOutBeforeMaterial(
            @Param("factoryCode") String factoryCode,
            @Param("scheduleDate") Date scheduleDate,
            @Param("machineCloseOutBeforeMap") Map<String, Date> machineCloseOutBeforeMap);

    /**
     * 查询指定日期区间内有计划量的物料编码。
     *
     * @param factoryCode  分厂编号
     * @param startDate    开始日期（包含）
     * @param endDate      结束日期（不包含）
     * @param materialCodes 候选物料编码
     * @return 区间内存在计划量的物料编码
     */
    List<String> selectProducedMaterialCodes(
            @Param("factoryCode") String factoryCode,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("materialCodes") List<String> materialCodes);
}
