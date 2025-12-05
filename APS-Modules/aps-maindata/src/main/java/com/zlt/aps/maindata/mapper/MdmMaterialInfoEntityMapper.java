package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.vo.TableProductInfoVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMaterialInfoMapper.java
 * 描    述：物料信息Mapper接口
 *@author zlt
 *@date 2025-02-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MdmMaterialInfoEntityMapper extends CommBaseMapper<MdmMaterialInfo> {

    MdmMaterialInfo selectByProductCode(@Param("productCode") String productCode);

    List<MdmMaterialInfo> selectByPlanVersion(@Param("year") int year, @Param("month") int month,
                                          @Param("planVersion") String planVersion,
                                          @Param("productName") String productName);

    /**
     * 查询物料基础信息
     *
     * @param query
     * @return
     */
    List<TableProductInfoVo> getMaterialInfoList(TableProductInfoVo query);
    /**
     * 获取可以替换的物料
     */
    List<MdmMaterialInfo> selectReplaceMaterialInfoList(MdmMaterialInfo productInfo);

    /**
     * 根据唯一键查询
     * @param subList 唯一键列表，根据"|"分隔
     * @return 查询结果
     */
    List<MdmMaterialInfo> selectByUniqueKeyList(@Param("list") List<String> subList);

    /**
     * 根据分厂编号和物料号集合查询物料信息
     * @param factoryCode 分厂编号
     * @param productCodes 物料编号集合
     * @return 对应的施工记录列表
     */
    List<MdmMaterialInfo> queryByFactoryCodeAndProductCodes(@Param("factoryCode") String factoryCode,
                                                           @Param("productCodes") List<String> productCodes);
}
