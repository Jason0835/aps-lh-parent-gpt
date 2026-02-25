package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.dto.ProductMouldConfigurationParam;
import com.zlt.aps.mp.api.domain.dto.ProductMouldRelationConfigurationParam;
import com.zlt.aps.mp.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.vo.ProductMouldInfoVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProductModelRelationService.java
 * 描    述：IMdmProductModelRelationServiceSKU与模具关系后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-24
 */
public interface IMdmProductModelRelationService extends IDocService<MdmSkuMouldRel> {

    /**
     * 查询规格对应的模具关系
     *
     * @param specCodes
     * @return
     */
    List<MdmSkuMouldRel> queryBySpecCodes(Set<String> specCodes, String factoryCode);

    /**
     * 根据物料编码及分厂、年月信息，得到物料匹配的模具信息
     *
     * @param queryParam 查询条件
     * @return
     */
    ProductMouldInfoVo getProductMatchMould(ProductMouldConfigurationParam queryParam);

    /**
     * 配置物料的模具关系
     * 1、验证物料是否存在
     * 2、验证模具信息及数量是否匹配
     * 3、删除原有的关系，重新配置物料与模具关系
     *
     * @param configuration
     * @return
     */
    AjaxResult configurationMouldRelation(ProductMouldRelationConfigurationParam configuration);

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    AjaxResult mesCapture();

    /**
     * 更新主花纹到物料
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    AjaxResult updateMainPatternToMaterial(MdmSkuMouldRel queryVO);
}
