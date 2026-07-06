package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.MesTmScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎面排程结果下发Mapper接口
 * 中间表MES_TM_SCHEDULE_RESULT建在MES分库，通过@DS(DataSource.MES)指定数据源
 *
 * @author APS
 */
@DS(DataSource.MES)
@Mapper
public interface TmScheduleResultIssueMapper {

    /**
     * 批量新增胎面排程结果到MES中间表
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertTmScheduleResult(@Param("list") List<MesTmScheduleResult> list);

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
     * 批量根据排程日期+机台+胎面编码更新数据
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchUpdateByScheduleDateAndMachine(@Param("list") List<MesTmScheduleResult> list);

    /**
     * 批量查询中间表中已存在的记录（按排程日期+机台编码+胎面编码+版本号匹配）
     *
     * @param list 数据列表
     * @return 已存在的记录列表
     */
    List<MesTmScheduleResult> selectExistingByScheduleDateAndMachine(@Param("list") List<MesTmScheduleResult> list);
}
