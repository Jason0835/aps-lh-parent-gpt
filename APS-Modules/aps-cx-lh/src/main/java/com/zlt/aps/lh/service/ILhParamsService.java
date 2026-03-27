package com.zlt.aps.lh.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhParamsService.java
 * 描    述：ILhParamsService硫化参数信息后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
public interface ILhParamsService extends IDocService<LhParams> {

    /**
     * 查询List
     * @param queryWrapper
     * @return
     */
    List<LhParams> selectList(QueryWrapper<LhParams> queryWrapper);


    /**
     * 校验唯一性
     */
    String checkUnique(LhParams query);

    /**
     * 根据参数编码查询参数信息
     * @param paramCode
     * @return
     */
    LhParams selectOneByParamCode(String paramCode,String factoryCode);

    /**
     * 根据参数编码查询参数信息
     * @return
     */
    Map<String,String> listLhParams(String factoryCode);
}
