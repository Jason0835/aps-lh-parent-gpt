package com.zlt.aps.lh.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhMachineInfoService.java
 * 描    述：ILhMachineInfoService硫化机台信息后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
public interface ILhMachineInfoService extends IDocService<LhMachineInfo> {


    /**
     * 查询硫化机台List
     * @param lhMachineInfo
     * @return
     */
    List<LhMachineInfo> selectList(LhMachineInfo  lhMachineInfo);

    /**
     * 根据条件式查询
     * @param queryWrapper
     * @return
     */
    List<LhMachineInfo> selectListExportData(QueryWrapper<LhMachineInfo> queryWrapper);

    /**
     * 查询机台信息
     * @param factoryCode
     * @param machineCode
     * @return
     */
    LhMachineInfo selectOneByMachineCode(String factoryCode,String machineCode);
}
