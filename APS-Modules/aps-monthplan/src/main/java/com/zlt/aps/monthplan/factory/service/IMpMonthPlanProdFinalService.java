package com.zlt.aps.monthplan.factory.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.common.datasource.service.IBaseService;

import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanProdFinal;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthPlanProdFinalService.java
 * 描    述：IMpMonthPlanProdFinalService工厂月生产计划-最终排产计划定稿后端接口
 *@author yelq
 *@date 2025-12-16
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpMonthPlanProdFinalService  extends IBaseService<MpMonthPlanProdFinal>
{
    /**
     * 查询工厂月生产计划-最终排产计划定稿
     * 
     * @param id 工厂月生产计划-最终排产计划定稿主键
     * @return 工厂月生产计划-最终排产计划定稿
     */
    MpMonthPlanProdFinal selectMpMonthPlanProdFinalById(Integer id);

    /**
     * 查询工厂月生产计划-最终排产计划定稿列表
     * 
     * @param mpMonthPlanProdFinal 工厂月生产计划-最终排产计划定稿
     * @return 工厂月生产计划-最终排产计划定稿集合
     */
    List<MpMonthPlanProdFinal> selectMpMonthPlanProdFinalList(MpMonthPlanProdFinal mpMonthPlanProdFinal);

    /**
     * 批量查询工厂月生产计划-最终排产计划定稿列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 工厂月生产计划-最终排产计划定稿集合
     */
    List<MpMonthPlanProdFinal> selectMpMonthPlanProdFinalByIds(List<Integer> ids);


    /**
     * 新增工厂月生产计划-最终排产计划定稿
     * 
     * @param mpMonthPlanProdFinal 工厂月生产计划-最终排产计划定稿
     * @return 结果
     */
    @Transactional
    int insertMpMonthPlanProdFinal(MpMonthPlanProdFinal mpMonthPlanProdFinal);

    /**
     * 修改工厂月生产计划-最终排产计划定稿
     * 
     * @param mpMonthPlanProdFinal 工厂月生产计划-最终排产计划定稿
     * @return 结果
     */
    @Transactional
    int updateMpMonthPlanProdFinal(MpMonthPlanProdFinal mpMonthPlanProdFinal);

    /**
     * 批量删除工厂月生产计划-最终排产计划定稿
     * 
     * @param ids 需要删除的工厂月生产计划-最终排产计划定稿主键集合
     * @return 结果
     */
   
    @Transactional
    int deleteMpMonthPlanProdFinalByIds(Integer[] ids);

    /**
     * 批量删除工厂月生产计划-最终排产计划定稿
     *
     * @param ids 需要删除的工厂月生产计划-最终排产计划定稿主键集合
     * @return 结果
     */

    @Transactional
    int deleteMpMonthPlanProdFinalByIds(List<Integer> ids);

    /**
     * 删除工厂月生产计划-最终排产计划定稿信息
     * 
     * @param id 工厂月生产计划-最终排产计划定稿主键
     * @return 结果
     */
    @Transactional
    int deleteMpMonthPlanProdFinalById(Integer id);

    /**
     * 校验工厂月生产计划-最终排产计划定稿唯一性
     */
    String checkMpMonthPlanProdFinalUnique(MpMonthPlanProdFinal mpMonthPlanProdFinal);

    /**
     * 导入工厂月生产计划-最终排产计划定稿数据
     */
    @Transactional
    AjaxResult importData(List<MpMonthPlanProdFinal> list, boolean updateSupport, Long importLogId);
    /**
     *  库存抓取日~（同月）月底的月度计划量汇总
     * @param requireVersionNumber  需求计划版本号
     * @return 月底的月度计划量汇总
     */
    Map<String,Long> calculateMonthSurplus(String requireVersionNumber);
}
