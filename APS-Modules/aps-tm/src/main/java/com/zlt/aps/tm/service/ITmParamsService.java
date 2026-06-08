package com.zlt.aps.tm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITmParamsService.java
 * 描    述：ITmParamsService胎面排程参数配置后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
public interface ITmParamsService extends IDocService<TmParams> {

    List<TmParams> selectList(QueryWrapper<TmParams> queryWrapper);

    String checkUnique(TmParams query);

    TmParams selectOneByParamCode(String paramCode, String factoryCode);

    Map<String, String> listTmParams(String factoryCode);
}
