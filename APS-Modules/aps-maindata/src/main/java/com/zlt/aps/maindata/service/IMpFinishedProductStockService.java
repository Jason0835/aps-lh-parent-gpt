package com.zlt.aps.maindata.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpFinishedProductStockService.java
 * 描    述：IMpFinishedProductStockService成品库存后端接口
 *@author yelq
 *@date 2025-12-15
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpFinishedProductStockService  extends IBaseService<MpFinishedProductStock>
{
    /**
     * 查询成品库存
     * 
     * @param id 成品库存主键
     * @return 成品库存
     */
    MpFinishedProductStock selectMpFinishedProductStockById(Long id);

    /**
     * 查询成品库存列表
     * 
     * @param mpFinishedProductStock 成品库存
     * @return 成品库存集合
     */
    List<MpFinishedProductStock> selectMpFinishedProductStockList(MpFinishedProductStock mpFinishedProductStock);

    /**
     * 批量查询成品库存列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 成品库存集合
     */
    List<MpFinishedProductStock> selectMpFinishedProductStockByIds(List<Long> ids);


    /**
     * 新增成品库存
     * 
     * @param mpFinishedProductStock 成品库存
     * @return 结果
     */
    @Transactional
    int insertMpFinishedProductStock(MpFinishedProductStock mpFinishedProductStock);

    /**
     * 修改成品库存
     * 
     * @param mpFinishedProductStock 成品库存
     * @return 结果
     */
    @Transactional
    int updateMpFinishedProductStock(MpFinishedProductStock mpFinishedProductStock);

    /**
     * 批量删除成品库存
     * 
     * @param ids 需要删除的成品库存主键集合
     * @return 结果
     */
   
    @Transactional
    int deleteMpFinishedProductStockByIds(Long[] ids);

    /**
     * 批量删除成品库存
     *
     * @param ids 需要删除的成品库存主键集合
     * @return 结果
     */

    @Transactional
    int deleteMpFinishedProductStockByIds(List<Long> ids);

    /**
     * 删除成品库存信息
     * 
     * @param id 成品库存主键
     * @return 结果
     */
    @Transactional
    int deleteMpFinishedProductStockById(Long id);

    /**
     * 校验成品库存唯一性
     */
    String checkMpFinishedProductStockUnique(MpFinishedProductStock mpFinishedProductStock);

    /**
     * 导入成品库存数据
     */
    @Transactional
    AjaxResult importData(List<MpFinishedProductStock> list, boolean updateSupport, Long importLogId);
    /**
     * 查询成品库存表，汇总计算超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
     * @return
     */
    List<MpFinishedProductStock> findCurrentFinishStock();
    /**
     *  根据物料编号获取成品库存
     * @param materialCode 自定义工具栏…
     * @return 成品库存
     */
    List<MpFinishedProductStock> getMpFinishedProductStockByMaterialCode(String materialCode);
}
