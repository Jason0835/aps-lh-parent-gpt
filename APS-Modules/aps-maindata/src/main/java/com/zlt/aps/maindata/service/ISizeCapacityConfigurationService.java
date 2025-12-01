package com.zlt.aps.maindata.service;


import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.SizeCapacityConfigurationVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISizeCapacityConfigurationService.java
 * 描    述：ISizeCapacityConfigurationService寸口产能配置后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-06-04
 */
public interface ISizeCapacityConfigurationService extends IDocService<SizeCapacityConfiguration> {
    /**
     * 根据查询条件，获取配置
     * 分厂、年月、需求版本不可为空
     *
     * @param queryCondition
     * @return
     */
    List<SizeCapacityConfiguration> getConfigurationList(SizeCapacityConfiguration queryCondition);

    /**
     * 根据查询条件，获取需求信息
     * 分厂、年月、需求版本、寸口不可为空
     *
     * @param condition
     * @return
     */
    SizeCapacityConfigurationVo getDemandInfo(SizeCapacityConfiguration condition);

    /**
     * 根据ID，获取配置信息
     * 包含对应需求版本的总需求、净需求、备货需求
     *
     * @param id
     * @return
     */
    SizeCapacityConfigurationVo getConfigurationById(Long id);

    /**
     * 获取分厂在指定年份、月份的寸口产能分配配置
     *
     * @param factoryCode 分厂
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<SizeCapacityConfiguration> getConfigurationByFactoryYearAndMonth(String factoryCode, Integer year, Integer month);
}
