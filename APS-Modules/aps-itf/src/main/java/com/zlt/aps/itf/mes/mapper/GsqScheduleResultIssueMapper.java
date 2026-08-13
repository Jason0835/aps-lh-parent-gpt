package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.MesGsqScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钢丝圈排程结果下发Mapper接口
 * 中间表MES_GSQ_SCHEDULE_RESULT建在MES分库，通过@DS(DataSource.MES)指定数据源
 *
 * @author APS
 */
@DS(DataSource.MES)
@Mapper
public interface GsqScheduleResultIssueMapper {

    /**
     * 批量新增钢丝圈排程结果到MES中间表
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertGsqScheduleResult(@Param("list") List<MesGsqScheduleResult> list);

    /**
     * 根据排程日期删除数据
     *
     * @param scheduleDate 排程日期
     * @param dataVersion  版本号
     * @return 影响行数
     */
    int deleteByScheduleDate(@Param("scheduleDate") String scheduleDate,
                              @Param("dataVersion") String dataVersion);

    /**
     * 批量查询中间表中已存在的记录（按排程日期+机台编码+钢丝圈编码匹配，不含版本号）
     * 说明：匹配键不含版本号，目的是让同一天的重新发布能覆盖旧版本数据，避免中间表多版本残留。
     *
     * @param list 数据列表
     * @return 已存在的记录列表（仅包含SCHEDULE_DATE, MACHINE_CODE, STEEL_RING_CODE）
     */
    List<MesGsqScheduleResult> selectExistingByScheduleDateAndMachine(@Param("list") List<MesGsqScheduleResult> list);

    /**
     * 批量删除中间表中已存在的记录（按排程日期+机台编码+钢丝圈编码匹配，会删除该键的所有版本数据）
     * 说明：用于重新发布场景，先删除该键的所有历史版本数据，再插入本次发布的新版本数据，
     *      彻底避免多版本残留造成的同版本同日期同机台重复记录。
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchDeleteByScheduleDateAndMachine(@Param("list") List<MesGsqScheduleResult> list);
}
