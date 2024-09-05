package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型前日计划增补Service接口
 *
 * @author chen
 * @date 2022-02-09
 */
public interface CxLastDaySupplePlanService {
    /**
     * 查询成型前日计划增补列表
     *
     * @param cxLastDaySupplePlan 成型前日计划增补
     * @return 成型前日计划增补集合
     */
    public List<CxLastDaySupplePlanDto> selectCxLastDaySupplePlanList(CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 批量删除成型前日计划增补
     *
     * @param ids 需要删除的成型前日计划增补ID
     * @return 结果
     */
    @Transactional
    public int deleteCxLastDaySupplePlanByIds(Long[] ids);

    /**
     * 修改成型前日计划增补
     *
     * @param cxLastDaySupplePlan 成型前日计划增补
     * @return 结果
     */
    @Transactional
    public int updateCxLastDaySupplePlan(CxLastDaySupplePlanDto cxLastDaySupplePlan);

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
    public AjaxResult insertCxLastDaySupplePlan(CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 校验-使用模数
     */
    public AjaxResult modifyMoldsValidate(CxLastDaySupplePlanDto lastDaySupplePlanDto);

    /**
     * 修改-使用模数
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult modifyMolds(CxLastDaySupplePlanDto lastDaySupplePlanDto);
}
