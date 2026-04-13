package com.zlt.aps.mdm.service;


import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.bill.common.service.IDocService;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmConstructionInfoService.java
 * 描    述：IMdmConstructionInfoService投产胎胚施工信息后端接口
 *@author zlt
 *@date 2025-12-10
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMdmConstructionInfoService  extends IDocService<MdmConstructionInfo>{

    void importDataAsync(List<MdmConstructionInfo> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes);

}
