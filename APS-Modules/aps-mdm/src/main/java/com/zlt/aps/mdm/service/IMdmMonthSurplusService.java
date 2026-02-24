package com.zlt.aps.mdm.service;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.aps.mdm.api.domain.entity.MdmMonthSurplus;
import com.zlt.bill.common.service.IDocService;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMonthSurplusService.java
 * 描    述：IMdmMonthSurplusService0140基础数据_月底计划余量后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
public interface IMdmMonthSurplusService extends IDocService<MdmMonthSurplus> {

    /**
     * 异步导入
     */
    void importDataAsync(List<MdmMonthSurplus> list, boolean updateSupport, Long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes);

}
