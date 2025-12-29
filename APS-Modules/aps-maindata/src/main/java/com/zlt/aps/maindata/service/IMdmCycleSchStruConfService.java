package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmCycleSchStruConfService.java
 * 描    述：IMdmCycleSchStruConfService周期排产结构配置后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-09
 */
public interface IMdmCycleSchStruConfService extends IDocService<MdmCycleSchStruConf> {

    /**
     * 生成月周期排产结构配置
     *
     * @param mdmCycleSchStruConf 参数
     * @return 结果
     */
    AjaxResult genMonthCycleSchStruConf(MdmCycleSchStruConf mdmCycleSchStruConf);
    /**
     *  查询当前周期性排产结构配置
     * @return 周期性排产结构配置
     */
    List<MdmCycleSchStruConf> findCycleSchStruConf();
}
