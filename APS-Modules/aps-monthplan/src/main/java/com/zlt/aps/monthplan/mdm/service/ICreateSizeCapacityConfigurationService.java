package com.zlt.aps.monthplan.mdm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.vo.BuildSizeCapacityParamVo;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CreateSizeCapacityConfigurationService.java
 * 描    述：寸口产能配置生产业务接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20250617
 */
public interface ICreateSizeCapacityConfigurationService {
    /**
     * 根据分厂、年、月、需求版本生成寸口产能配置信息
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult buildSizeCapacityConfiguration(BuildSizeCapacityParamVo factoryProductionParam);
}
