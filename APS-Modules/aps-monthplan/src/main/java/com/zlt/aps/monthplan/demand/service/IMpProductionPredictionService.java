package com.zlt.aps.monthplan.demand.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;

import com.zlt.aps.monthplan.api.domain.entity.MpProductionPrediction;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpProductionPredictionService.java
 * 描    述：IMpProductionPredictionServiceS2-1002.未来产量预测后端接口
 *@author yelq
 *@date 2025-12-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpProductionPredictionService  extends IBaseService<MpProductionPrediction>
{
    /**
     * 查询S2-1002.未来产量预测
     * 
     * @param id S2-1002.未来产量预测主键
     * @return S2-1002.未来产量预测
     */
    MpProductionPrediction selectMpProductionPredictionById(Long id);

    /**
     * 查询S2-1002.未来产量预测列表
     * 
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return S2-1002.未来产量预测集合
     */
    List<MpProductionPrediction> selectMpProductionPredictionList(MpProductionPrediction mpProductionPrediction);

    /**
     * 批量查询S2-1002.未来产量预测列表
     *
     * @param ids 需要查询的数据主键集合
     * @return S2-1002.未来产量预测集合
     */
    List<MpProductionPrediction> selectMpProductionPredictionByIds(List<Long> ids);


    /**
     * 新增S2-1002.未来产量预测
     * 
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return 结果
     */
    @Transactional
    int insertMpProductionPrediction(MpProductionPrediction mpProductionPrediction);

    /**
     * 修改S2-1002.未来产量预测
     * 
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return 结果
     */
    @Transactional
    int updateMpProductionPrediction(MpProductionPrediction mpProductionPrediction);

    /**
     * 批量删除S2-1002.未来产量预测
     * 
     * @param ids 需要删除的S2-1002.未来产量预测主键集合
     * @return 结果
     */
   
    @Transactional
    int deleteMpProductionPredictionByIds(Long[] ids);

    /**
     * 批量删除S2-1002.未来产量预测
     *
     * @param ids 需要删除的S2-1002.未来产量预测主键集合
     * @return 结果
     */

    @Transactional
    int deleteMpProductionPredictionByIds(List<Long> ids);

    /**
     * 删除S2-1002.未来产量预测信息
     * 
     * @param id S2-1002.未来产量预测主键
     * @return 结果
     */
    @Transactional
    int deleteMpProductionPredictionById(Long id);

    /**
     * 校验S2-1002.未来产量预测唯一性
     */
    String checkMpProductionPredictionUnique(MpProductionPrediction mpProductionPrediction);

    /**
     * 导入S2-1002.未来产量预测数据
     */
    @Transactional
    AjaxResult importData(List<MpProductionPrediction> list, boolean updateSupport, Long importLogId);
    /**
     *  生成订单预测
     * @param createCondition 参数
     * @return 结果
     */
    AjaxResult createMonthPrediction(MpProductionPrediction createCondition);
}
