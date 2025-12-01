package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.bill.common.service.IDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICd15CurlLengthService.java
 * 描    述：ICd15CurlLengthService钢丝斜裁卷曲长度后端接口
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ICd15CurlLengthService  extends IDocService<Cd15CurlLength>{

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlLength 查询条件
     * @return 结果
     */
    AjaxResult selectCurlLengthByCode(Cd15CurlLength curlLength);
}
