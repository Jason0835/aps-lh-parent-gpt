package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;

import java.util.List;

/**
 * 成型前日计划增补Mapper接口
 *
 * @author chen
 * @date 2022-02-09
 */
public interface CxLastDaySupplePlanMapper {
    /**
     * 查询成型前日计划增补列表
     *
     * @param cxLastDaySupplePlan 成型前日计划增补
     * @return 成型前日计划增补集合
     */
    public List<CxLastDaySupplePlanDto> selectCxLastDaySupplePlanList(CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 查询成型前日计划增补列表,用于检查同机台同胎胚不能存在多品号同时投产
     *
     * @param cxLastDaySupplePlan 成型前日计划增补
     * @return 成型前日计划增补集合
     */
    public List<CxLastDaySupplePlanDto> selectCxLastDaySupplePlanList2(CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 修改成型前日计划增补
     *
     * @param cxLastDaySupplePlan 成型前日计划增补
     * @return 结果
     */
    public int updateCxLastDaySupplePlan(CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 批量删除成型前日计划增补
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxLastDaySupplePlanByIds(Long[] ids);

    /**
     * 根据id查询对应批次号对应的状态，以字符串的形式返回，逗号分隔
     *
     * @param ids 需要查询的数据ID
     * @return 结果
     */
    public String selectStatusByIds(Long[] ids);

    /**
     * 根据id查询成型前日计划增补
     */
    public CxLastDaySupplePlanDto getInfo(Long id);

    /**
     * 新增成型前日增补计划
     *
     * @param cxScheduleResult 前日增补计划
     * @return 结果
     */
    public int insertCxLastDaySupplePlan(CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 根据排程日期查询是否已经生成前日增补计划，如果已生成，则返回增补批次号及成型批次号
     * @param scheduleDateStr 排程日期字符串，格式:yyyy-MM-dd
     * @return 增补批次号及成型批次号
     */
    public CxLastDaySupplePlanDto selectSuppleBatchNoAndCxBatchNoByScheduleDate(String scheduleDateStr);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 查询非本id的且包含该硫化机的记录
     */
    public List<CxLastDaySupplePlanDto> getListByLhMachineCode(CxLastDaySupplePlanDto lastDaySupplePlanDto);
}
