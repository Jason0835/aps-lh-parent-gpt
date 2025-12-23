package com.zlt.aps.monthplan.demand.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;

import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpOrderOffsetDetailService.java
 * 描    述：IDpOrderOffsetDetailServiceS1-0604订单冲减分配后端接口
 *@author yelq
 *@date 2025-12-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpOrderOffsetDetailService  extends IBaseService<DpOrderOffsetDetail>
{
    /**
     * 查询S1-0604订单冲减分配
     * 
     * @param id S1-0604订单冲减分配主键
     * @return S1-0604订单冲减分配
     */
    DpOrderOffsetDetail selectDpOrderOffsetDetailById(Long id);

    /**
     * 查询S1-0604订单冲减分配列表
     * 
     * @param dpOrderOffsetDetail S1-0604订单冲减分配
     * @return S1-0604订单冲减分配集合
     */
    List<DpOrderOffsetDetail> selectDpOrderOffsetDetailList(DpOrderOffsetDetail dpOrderOffsetDetail);

    /**
     * 批量查询S1-0604订单冲减分配列表
     *
     * @param ids 需要查询的数据主键集合
     * @return S1-0604订单冲减分配集合
     */
    List<DpOrderOffsetDetail> selectDpOrderOffsetDetailByIds(List<Long> ids);


    /**
     * 新增S1-0604订单冲减分配
     * 
     * @param dpOrderOffsetDetail S1-0604订单冲减分配
     * @return 结果
     */
    @Transactional
    int insertDpOrderOffsetDetail(DpOrderOffsetDetail dpOrderOffsetDetail);

    /**
     * 修改S1-0604订单冲减分配
     * 
     * @param dpOrderOffsetDetail S1-0604订单冲减分配
     * @return 结果
     */
    @Transactional
    int updateDpOrderOffsetDetail(DpOrderOffsetDetail dpOrderOffsetDetail);

    /**
     * 批量删除S1-0604订单冲减分配
     * 
     * @param ids 需要删除的S1-0604订单冲减分配主键集合
     * @return 结果
     */
   
    @Transactional
    int deleteDpOrderOffsetDetailByIds(Long[] ids);

    /**
     * 批量删除S1-0604订单冲减分配
     *
     * @param ids 需要删除的S1-0604订单冲减分配主键集合
     * @return 结果
     */

    @Transactional
    int deleteDpOrderOffsetDetailByIds(List<Long> ids);

    /**
     * 删除S1-0604订单冲减分配信息
     * 
     * @param id S1-0604订单冲减分配主键
     * @return 结果
     */
    @Transactional
    int deleteDpOrderOffsetDetailById(Long id);

    /**
     * 校验S1-0604订单冲减分配唯一性
     */
    String checkDpOrderOffsetDetailUnique(DpOrderOffsetDetail dpOrderOffsetDetail);

    /**
     * 导入S1-0604订单冲减分配数据
     */
    @Transactional
    AjaxResult importData(List<DpOrderOffsetDetail> list, boolean updateSupport, Long importLogId);
}
