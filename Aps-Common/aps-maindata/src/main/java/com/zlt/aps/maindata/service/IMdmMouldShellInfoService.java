package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MdmMouldShellInfo;
import com.zlt.bill.common.service.IDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMouldShellInfoService.java
 * 描    述：IMpMouldShellInfoService模壳台账后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
public interface IMdmMouldShellInfoService extends IDocService<MdmMouldShellInfo> {

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    AjaxResult mesCapture();
}
