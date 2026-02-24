package com.zlt.aps.maindata.service;


import com.zlt.aps.monthplan.api.domain.entity.ProductMinConfiguration;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductMinConfigurationService.java
 * 描    述：IProductMinConfigurationService最小批量后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */
public interface IProductMinConfigurationService extends IDocService<ProductMinConfiguration> {
    /**
     * 获取最小批量设置
     *
     * @return
     */
    List<ProductMinConfiguration> getConfigurationList();
}
