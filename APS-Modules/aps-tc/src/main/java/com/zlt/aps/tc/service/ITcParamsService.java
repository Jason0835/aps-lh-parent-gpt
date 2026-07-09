package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.bill.common.service.IDocService;

import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITcParamsService.java
 * 描    述：ITcParamsService胎侧排程参数配置后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
public interface ITcParamsService extends IDocService<TcParams> {
    TcParams selectOneByParamCode(String paramCode, String factoryCode);

    Map<String, String> listTcParams(String factoryCode);
}
