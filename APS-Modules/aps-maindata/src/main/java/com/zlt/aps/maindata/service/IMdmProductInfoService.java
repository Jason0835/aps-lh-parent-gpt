package com.zlt.aps.maindata.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductInfo;
import com.zlt.aps.monthplan.api.domain.vo.ConfigConstructionVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductInfoGrossRateVo;
import com.zlt.aps.monthplan.api.domain.vo.TableProductInfoVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProductInfoService.java
 * 描    述：IMdmProductInfoService物料信息后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-19
 */
public interface IMdmProductInfoService extends IDocService<MdmProductInfo> {

    /**
     * 根据编号查询物料信息
     */
    List<MdmProductInfo> selectListByProductCode(List<String> codeList);

    /**
     * 根据查询条件，获取物料信息
     *
     * @param queryCondition
     * @return
     */
    List<TableProductInfoVo> getList(TableProductInfoVo queryCondition);

    /**
     * 查询物料信息表
     *
     * @param id 物料信息表主键
     * @return 物料信息表
     */
    MdmProductInfo selectProductInfoById(Long id);

    /**
     * 新增物料信息表
     *
     * @param productInfo 物料信息表
     * @return 结果
     */
    int insertProductInfo(MdmProductInfo productInfo);

    /**
     * 修改物料信息表
     *
     * @param productInfo 物料信息表
     * @return 结果
     */
    int updateProductInfo(MdmProductInfo productInfo);

    /**
     * 批量删除物料信息表
     *
     * @param ids 需要删除的物料信息表主键集合
     * @return 结果
     */
    int deleteProductInfoByIds(Long[] ids);

    /**
     * 删除物料信息表信息
     *
     * @param id 物料信息表主键
     * @return 结果
     */
    int deleteProductInfoById(Long id);

    /**
     * 校验物料信息表唯一性
     */
    String checkProductInfoUnique(MdmProductInfo productInfo);

    /**
     * 查询列表
     *
     * @param wrapper 查询条件
     * @return 结果
     */
    List<MdmProductInfo> selectList(QueryWrapper<MdmProductInfo> wrapper);

    /**
     * 将json字段转成前端展示字段
     *
     * @param productInfoList 要转换的物料信息
     */
    void transformJsonField(List<MdmProductInfo> productInfoList);

    /**
     * 将前端的字段转换json字段存储
     *
     * @param productInfoList 要转换的物料信息
     */
    void transformToJsonField(List<MdmProductInfo> productInfoList);

    /**
     * 根据分厂编号和物料号集合查询物料信息
     *
     * @param factoryCode  分厂编号
     * @param productCodes 物料编号集合
     * @return 对应的施工记录列表
     */
    List<MdmProductInfo> queryByFactoryCodeAndProductCodes(String factoryCode, Set<String> productCodes);

    /**
     * 根据物料编号和规格编号查询物料信息
     *
     * @param productInfo
     * @param factoryCode
     * @return
     */
    MdmProductInfo selectOneByProductCodeAndSpecCode(String productInfo, String factoryCode);

    /**
     * 导入物料信息
     *
     * @param list          要导入的列表
     * @param updateSupport 是否更新
     * @param importLogId   导入记录id
     * @return 结果
     */
    AjaxResult importGrossRate(List<ProductInfoGrossRateVo> list, boolean updateSupport, Long importLogId);

    /**
     * 查询对应物料列表+分厂列表的物料信息
     *
     * @param factoryCodeList 分厂列表（可以为空，只限制物料编号）
     * @param productCodeList 物料编号列表（不能为空）
     * @return 物料信息
     */
    List<MdmProductInfo> selectListByFactoryProductCode(List<String> factoryCodeList, List<String> productCodeList);

    /**
     * 配置施工记录校验
     *
     * @param productConstruction 物料信息ID、SAP代码(物料表的)、胎胚号、规格代码、施工代码、成型法
     * @return 结果
     */
    AjaxResult configurationConstructionCheck(MdmProductConstruction productConstruction);

    /**
     * 配置施工记录
     *
     * @param productConstruction 物料信息ID、SAP代码(物料表的)、胎胚号、规格代码、施工代码、成型法
     * @return 结果
     */
    AjaxResult configurationConstruction(MdmProductConstruction productConstruction);

    /**
     * 根据物料号查询对应的SAP与施工关系
     * @param productConstruction 物料号
     * @return 结果
     */
    AjaxResult selectConstructionCheckList(MdmProductConstruction productConstruction);

    /**
     * 配置施工关系
     * @param configConstructionVo 配置施工关系
     * @return 结果
     */
    AjaxResult configConstruction(ConfigConstructionVo configConstructionVo);
}
