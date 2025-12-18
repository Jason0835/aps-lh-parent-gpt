package com.zlt.aps.monthplan.demand.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.common.datasource.service.IBaseService;

import com.zlt.aps.monthplan.api.domain.entity.MpSkuProductionType;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpSkuProductionTypeService.java
 * 描    述：IMpSkuProductionTypeServiceSKU排产分类后端接口
 *@author yelq
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpSkuProductionTypeService  extends IBaseService<MpSkuProductionType>
{
    /**
     * 查询SKU排产分类
     * 
     * @param id SKU排产分类主键
     * @return SKU排产分类
     */
    MpSkuProductionType selectMpSkuProductionTypeById(Long id);

    /**
     * 查询SKU排产分类列表
     * 
     * @param mpSkuProductionType SKU排产分类
     * @return SKU排产分类集合
     */
    List<MpSkuProductionType> selectMpSkuProductionTypeList(MpSkuProductionType mpSkuProductionType);

    /**
     * 批量查询SKU排产分类列表
     *
     * @param ids 需要查询的数据主键集合
     * @return SKU排产分类集合
     */
    List<MpSkuProductionType> selectMpSkuProductionTypeByIds(List<Long> ids);


    /**
     * 新增SKU排产分类
     * 
     * @param mpSkuProductionType SKU排产分类
     * @return 结果
     */
    @Transactional
    int insertMpSkuProductionType(MpSkuProductionType mpSkuProductionType);

    /**
     * 修改SKU排产分类
     * 
     * @param mpSkuProductionType SKU排产分类
     * @return 结果
     */
    @Transactional
    int updateMpSkuProductionType(MpSkuProductionType mpSkuProductionType);

    /**
     * 批量删除SKU排产分类
     * 
     * @param ids 需要删除的SKU排产分类主键集合
     * @return 结果
     */
   
    @Transactional
    int deleteMpSkuProductionTypeByIds(Long[] ids);

    /**
     * 批量删除SKU排产分类
     *
     * @param ids 需要删除的SKU排产分类主键集合
     * @return 结果
     */

    @Transactional
    int deleteMpSkuProductionTypeByIds(List<Long> ids);

    /**
     * 删除SKU排产分类信息
     * 
     * @param id SKU排产分类主键
     * @return 结果
     */
    @Transactional
    int deleteMpSkuProductionTypeById(Long id);

    /**
     * 校验SKU排产分类唯一性
     */
    String checkMpSkuProductionTypeUnique(MpSkuProductionType mpSkuProductionType);

    /**
     * 导入SKU排产分类数据
     */
    @Transactional
    AjaxResult importData(List<MpSkuProductionType> list, boolean updateSupport, Long importLogId);
    /**
     *  获取SKU对应的排产分类
     * @return SKU对应的排产分类
     */
    Map<String,String> skuToProductionType();
}
