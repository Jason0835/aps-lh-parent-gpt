package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.HalfCdImportBak;
import com.zlt.aps.cd15.api.domain.vo.HalfCdImportBakExportVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfCdImportBakMapper.java
 * 描    述：裁断线下计划导入导出Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-29
 */
@Mapper
public interface HalfCdImportBakEntityMapper extends CommBaseMapper<HalfCdImportBak> {

    /**
     * 删除库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteTqStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTqStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 删除库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteGsqStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertGsqStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 删除库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteNcStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertNcStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 删除库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteCd90Stock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertCd90Stock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 删除库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteCd15Stock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertCd15Stock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateTqScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTqScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateGsqScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertGsqScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateNcScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertNcScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateCd90ScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertCd90ScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateCd15ScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertCd15ScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 查询裁断排程结果列表
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public List<HalfCdImportBakExportVo> selectScheduleResult(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询成型计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public List<HalfCdImportBakExportVo> selectCxScheduleResult(@Param("scheduleDate") String scheduleDate);

    /**
     * 新增胎圈月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTqMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新胎圈月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateTqMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增钢丝圈月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertGsqMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新钢丝圈月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateGsqMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增内衬月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertNcMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新内衬月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateNcMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增钢丝斜裁月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertCd15MonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新钢丝斜裁月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateCd15MonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增纤维直裁月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertCd90MonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新纤维直裁月剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateCd90MonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 查询胎圈排程结果列表
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public List<HalfCdImportBakExportVo> selectTqScheduleResult(@Param("scheduleDate") String scheduleDate);

    /**
     * 调用裁断计划导入存储过程
     *
     * @param scheduleDate 排程日期
     */
    public void importCdData(@Param("scheduleDate") Date scheduleDate);
}
