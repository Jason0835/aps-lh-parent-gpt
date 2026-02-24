package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanProductionFinalResultVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductModelRelationMapper.java
 * 描    述：SKU与模具关系Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-18
 */
@Mapper
public interface MdmProductModelRelationEntityMapper extends CommBaseMapper<MdmSkuMouldRel> {

    /**
     * 查询规格对应的模具关系
     *
     * @param specCodes
     * @return
     */
    List<MdmSkuMouldRel> queryBySpecCodes(@Param("factoryCode") String factoryCode, @Param("specCodes") List<String> specCodes);

    /**
     * 根据物料、模具、年份、月份，获取其最大可用模具编号
     *
     * @param productCode 物料编码
     * @param mouldNo     模具号
     * @param factoryCode 分厂
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<String> getMouldMaxList(@Param("productCode") String productCode, @Param("mouldNo") String mouldNo, @Param("factoryCode") String factoryCode, Integer year, Integer month);

    /**
     * 查询共用模具的情况
     * @return 结果
     */
    List<MdmSkuMouldRel> selectSameMouldNo();

    /**
     * 根据模具编号查询
     * @param mouldCodeList 模具编号
     * @return 结果
     */
    List<MdmSkuMouldRel> selectByMouldCode(@Param("list") List<String> mouldCodeList);

    /**
     * 查询SKU与模具关系，关联模具信息
     * @param resultVos 查询条件
     * @return 结果
     */
    List<MdmSkuMouldRel> select4ImportAdjustData(@Param("list") List<MonthPlanProductionFinalResultVo> resultVos);

    /**
     * 根据模具编号查询
     *
     * @param mouldCodeList 模具编号
     * @return 结果
     */
    List<MdmSkuMouldRel> selectByUniqueKeyList(@Param("list") List<MdmSkuMouldRel> mouldCodeList);

    /**
     * 根据模具台账更新SKU与模具关系的主花纹
     * @param modelInfo 创建人、创建时间
     * @return 结果
     */
    int updateMainPatternByModelInfo(MdmModelInfo modelInfo);
}
