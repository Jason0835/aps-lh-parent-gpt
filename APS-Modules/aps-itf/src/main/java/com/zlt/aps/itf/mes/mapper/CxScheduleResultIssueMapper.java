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
     * 批量新增成型排程结果到中间表
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
}
