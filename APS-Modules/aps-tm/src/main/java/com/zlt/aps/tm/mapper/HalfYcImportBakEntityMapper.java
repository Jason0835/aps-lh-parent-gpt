package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.entity.HalfYcImportBak;
import com.zlt.aps.tm.api.domain.vo.HalfYcImportBakExportVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfYcImportBakMapper.java
 * 描    述：线下计划导入Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-26
 */
@Mapper
public interface HalfYcImportBakEntityMapper extends BaseMapper<HalfYcImportBak> {

    /**
     * 删除库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteTmStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTmStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 删除库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteTcStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增库存数据
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTcStock(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateTmScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTmScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateTcScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增昨日早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTcScheduleResult(@Param("scheduleDate") Date scheduleDate);

    /**
     * 查询压出排程结果列表
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public List<HalfYcImportBakExportVo> selectScheduleResult(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询成型计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public List<HalfYcImportBakExportVo> selectCxScheduleResult(@Param("scheduleDate") String scheduleDate);

    /**
     * 新增胎面月度剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTmMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新胎面月度剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateTmMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 新增胎侧月度剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int insertTcMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 更新胎侧月度剩余量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int updateTcMonthPlanSurplus(@Param("scheduleDate") Date scheduleDate);

    /**
     * 执行导入压出数据存储过程
     *
     * @param scheduleDate 排程日期
     */
    public void importYcData(@Param("scheduleDate") Date scheduleDate);
}
