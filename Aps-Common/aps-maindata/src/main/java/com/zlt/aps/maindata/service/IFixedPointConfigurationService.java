package com.zlt.aps.maindata.service;


import com.zlt.aps.mp.api.domain.entity.FixedPointConfiguration;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFixedPointConfigurationService.java
 * 描    述：IFixedPointConfigurationService基础数据-定点机台主后端接口
 * 定点生产配置业务放置在改接口业务中，子表无需专门的service
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-21
 */
public interface IFixedPointConfigurationService {


    /**
     * 查询基础数据-定点机台主列表
     *
     * @param fixedPointConfiguration 基础数据-定点机台主
     * @return 基础数据-定点机台主集合
     */
    public List<FixedPointConfiguration> selectFixedPointConfigurationList(FixedPointConfiguration fixedPointConfiguration);
}
