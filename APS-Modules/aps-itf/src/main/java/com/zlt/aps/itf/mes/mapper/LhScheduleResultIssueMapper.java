package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.MesLhScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化排程结果下发Mapper接口
 *
 * @author APS Team
 * @since 2.0.0
 */
@DS(DataSource.MES)
@Mapper
public interface LhScheduleResultIssueMapper {

    /**
     * 批量新增硫化排程结果到MES中间表
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertLhScheduleResult(@Param("list") List<MesLhScheduleResult> list);

    /**
     * 根据排程日期和机台更新数据
     *
     * @param mesItem 数据项
     * @return 影响行数
     */
    int updateByScheduleDateAndMachine(MesLhScheduleResult mesItem);

    /**
     * 根据排程日期和机台更新数据（仅更新早中班，不覆盖夜班）
     * 用于T-2日（窗口首日）下发，T-2日无夜班数据，避免将MES已有的夜班数据覆盖为空
     *
     * @param mesItem 数据项
     * @return 影响行数
     */
    int updateDay1ByScheduleDateAndMachine(MesLhScheduleResult mesItem);

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
     * 批量查询中间表中已存在的记录（按排程日期+硫化机台编码+物料编码+工单号匹配，不含版本号）
     * 返回完整记录（含各班次数据），供Day1场景合并class1数据使用
     * 说明：匹配键不含版本号，目的是让同一天的重新发布能覆盖旧版本数据，避免中间表多版本残留。
     *
     * @param list 数据列表
     * @return 已存在的记录列表
     */
    List<MesLhScheduleResult> selectExistingByScheduleDateAndMachine(@Param("list") List<MesLhScheduleResult> list);

    /**
     * 批量删除中间表中已存在的记录（按排程日期+硫化机台编码+物料编码+工单号匹配，会删除该键的所有版本数据）
     * 说明：用于重新发布场景，先删除该键的所有历史版本数据，再插入本次发布的新版本数据，
     *      彻底避免多版本残留造成的同版本同日期同机台重复记录。
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchDeleteByScheduleDateAndMachine(@Param("list") List<MesLhScheduleResult> list);
}
