package com.zlt.aps.maindata.service;


import com.zlt.aps.monthplan.api.domain.entity.TireCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.TireCapacityConfigurationVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITireCapacityConfigurationService.java
 * 描    述：ITireCapacityConfigurationService轮胎类型产能配置(特殊情况下配置)后端接口
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
public interface ITireCapacityConfigurationService extends IDocService<TireCapacityConfiguration> {
    /**
     * 根据查询条件，获取轮胎类型产能配置列表
     *
     * @param condition
     * @return
     */
    List<TireCapacityConfigurationVo> getConfigurationList(TireCapacityConfiguration condition);

    /**
     * 根据查询条件，获取需求信息
     * 分厂、年月、需求版本、轮胎类型、寸口不可为空
     *
     * @param condition
     * @return
     */
    TireCapacityConfigurationVo getDemandInfo(TireCapacityConfiguration condition);

    /**
     * 根据ID，获取配置信息
     * 包含对应需求版本的总需求、净需求、备货需求
     *
     * @param id
     * @return
     */
    TireCapacityConfigurationVo getConfigurationById(Long id);

    /**
     * 获取分厂在指定年份、月份的轮胎类型产能分配配置
     *
     * @param factoryCode 分厂
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<TireCapacityConfiguration> getConfigurationByFactoryYearAndMonth(String factoryCode, Integer year, Integer month);
}
