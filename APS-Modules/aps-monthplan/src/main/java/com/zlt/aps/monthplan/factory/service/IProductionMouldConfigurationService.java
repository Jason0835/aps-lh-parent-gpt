package com.zlt.aps.monthplan.factory.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMouldingProductParamDto;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMouldConfiguration;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductionMouldConfigurationService.java
 * 描    述：IProductionMouldConfigurationService模具正在生产的品种后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-28
 */
public interface IProductionMouldConfigurationService extends IService<ProductionMouldConfiguration> {

    /**
     * 列表查询
     *
     * @param queryVO
     * @return
     */
    List<ProductionMouldConfiguration> selectList(ProductionMouldConfiguration queryVO);

    /**
     * 保存配置
     *
     * @param billVO
     * @return
     */
    AjaxResult saveConfiguration(ProductionMouldConfiguration billVO);

    /**
     * 根据ID列表删除
     *
     * @param ids
     * @return
     */
    int removeByIds(List<Long> ids);

    /**
     * 导入
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    AjaxResult doImportData(List<ProductionMouldConfiguration> list, boolean updateSupport, long importLogId);

    /**
     * 校验唯一性
     *
     * @param billVO
     * @return
     */
    String checkUnique(ProductionMouldConfiguration billVO);

    /**
     * 根据硫化排程，生成模具正在生产的物料续作信息
     *
     * @param param
     * @return
     */
    AjaxResult buildMouldingProduct(FactoryMouldingProductParamDto param);
}
