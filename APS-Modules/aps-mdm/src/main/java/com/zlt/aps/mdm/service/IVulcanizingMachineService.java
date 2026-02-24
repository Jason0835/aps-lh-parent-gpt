package com.zlt.aps.mdm.service;


import com.zlt.aps.mdm.api.domain.entity.VulcanizingMachine;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IVulcanizingMachineService.java
 * 描    述：IVulcanizingMachineService基础数据-硫化机档案后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
public interface IVulcanizingMachineService extends IDocService<VulcanizingMachine> {

    /**
     * 根据字段精确查询
     */
    List<VulcanizingMachine> selectListByVulcanizingMachine(VulcanizingMachine vulcanizingMachine);
}
