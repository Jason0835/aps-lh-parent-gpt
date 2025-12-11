package com.zlt.aps.maindata.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthlySaleQtyService.java
 * 描    述：IMpMonthlySaleQtyService月均销量后端接口
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpMonthlySaleQtyService  extends IBaseService<MpMonthlySaleQty>
{
    /**
     * 查询月均销量
     * 
     * @param id 月均销量主键
     * @return 月均销量
     */
    public MpMonthlySaleQty selectMpMonthlySaleQtyById(Long id);

    /**
     * 查询月均销量列表
     * 
     * @param mpMonthlySaleQty 月均销量
     * @return 月均销量集合
     */
    public List<MpMonthlySaleQty> selectMpMonthlySaleQtyList(MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 批量查询月均销量列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 月均销量集合
     */
    public List<MpMonthlySaleQty> selectMpMonthlySaleQtyByIds(List<Long> ids);


    /**
     * 新增月均销量
     * 
     * @param mpMonthlySaleQty 月均销量
     * @return 结果
     */
    @Transactional
    public int insertMpMonthlySaleQty(MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 修改月均销量
     * 
     * @param mpMonthlySaleQty 月均销量
     * @return 结果
     */
    @Transactional
    public int updateMpMonthlySaleQty(MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 批量删除月均销量
     * 
     * @param ids 需要删除的月均销量主键集合
     * @return 结果
     */
   
    @Transactional
    public int deleteMpMonthlySaleQtyByIds(Long[] ids);

    /**
     * 批量删除月均销量
     *
     * @param ids 需要删除的月均销量主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMpMonthlySaleQtyByIds(List<Long> ids);

    /**
     * 删除月均销量信息
     * 
     * @param id 月均销量主键
     * @return 结果
     */
    @Transactional
    public int deleteMpMonthlySaleQtyById(Long id);

    /**
     * 校验月均销量唯一性
     */
    public String checkMpMonthlySaleQtyUnique(MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 导入月均销量数据
     */
    @Transactional
    public AjaxResult importData(List<MpMonthlySaleQty> list, boolean updateSupport, Long importLogId);
}
