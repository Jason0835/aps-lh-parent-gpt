package com.zlt.aps.mp.mdm.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryNoProductionService.java
 * 描    述：IFactoryNoProductionService基础数据-分厂不排产后端接口
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
public interface IFactoryNoProductionService extends IService<FactoryNoProduction> {

    /**
     * 查询分厂不排产设定
     *
     * @param factoryNoProduction
     * @return
     */
    List<FactoryNoProduction> selectFactoryNoProductionList(FactoryNoProduction factoryNoProduction);

    /**
     * 校验分厂不排产品种唯一性
     *
     * @param factoryNoProduction
     * @return
     */
    String checkFactoryNotProductionUnique(FactoryNoProduction factoryNoProduction);

    /**
     * 导入分厂不排产品种数据
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    @Transactional
    AjaxResult importData(List<FactoryNoProduction> list, boolean updateSupport, Long importLogId);
}
