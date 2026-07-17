package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.MesCxScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成型排程结果下发Mapper接口
 *
 * @author APS Team
 * @since 2.0.0
 */
@DS(DataSource.MES)
@Mapper
public interface CxScheduleResultIssueMapper {

    /**
     * 批量新增成型排程结果到MES中间表
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertCxScheduleResult(@Param("list") List<MesCxScheduleResult> list);

    /**
     * 根据成型批次号和排程日期批量更新
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchUpdateCxScheduleResult(@Param("list") List<MesCxScheduleResult> list);

    /**
     * 根据排程日期范围删除数据
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param dataVersion 版本号
     * @return 影响行数
     */
    int deleteByScheduleDateRange(@Param("startDate") String startDate,
                                   @Param("endDate") String endDate,
                                   @Param("dataVersion") String dataVersion);

    /**
     * 根据排程日期删除数据
     *
     * @param scheduleDate 排程日期
     * @param dataVersion 版本号
     * @return 影响行数
     */
    int deleteByScheduleDate(@Param("scheduleDate") String scheduleDate,
                              @Param("dataVersion") String dataVersion);

    /**
     * 单条插入成型排程结果到中间表
     *
     * @param mesCxScheduleResult 数据
     * @return 影响行数
     */
    int insertCxScheduleResult(@Param("item") MesCxScheduleResult mesCxScheduleResult);

    /**
     * 根据排程日期和机台编码更新数据
     *
     * @param mesCxScheduleResult 数据
     * @return 影响行数
     */
    int updateByScheduleDateAndMachine(@Param("item") MesCxScheduleResult mesCxScheduleResult);

    /**
     * 批量根据排程日期和机台编码更新数据（匹配键不含版本号，更新时把新版本号写入 DATA_VERSION 字段）
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchUpdateByScheduleDateAndMachine(@Param("list") List<MesCxScheduleResult> list);

    /**
     * 批量查询中间表中已存在的记录（按排程日期+机台编码+胎胚编码+工单号匹配，不含版本号）
     * 说明：匹配键不含版本号，目的是让同一天的重新发布能覆盖旧版本数据，避免中间表多版本残留。
     *
     * @param list 数据列表
     * @return 已存在的记录列表（仅包含SCHEDULE_DATE, MACHINE_CODE, EMBRYO_CODE, ORDER_NO）
     */
    List<MesCxScheduleResult> selectExistingByScheduleDateAndMachine(@Param("list") List<MesCxScheduleResult> list);

    /**
     * 批量删除中间表中已存在的记录（按排程日期+机台编码+胎胚编码+工单号匹配，会删除该键的所有版本数据）
     * 说明：用于重新发布场景，先删除该 4 字段的所有历史版本数据，再插入本次发布的新版本数据，
     *      彻底避免多版本残留造成的同版本同日期同机台重复记录。
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchDeleteByScheduleDateAndMachine(@Param("list") List<MesCxScheduleResult> list);
}
