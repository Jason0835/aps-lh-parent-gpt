package com.zlt.aps.lh.service;


import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.bill.common.service.IDocService;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhSpecifyMachineService.java
 * 描    述：ILhSpecifyMachineService硫化定点机台信息后端接口
 *@author zlt
 *@date 2025-03-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ILhSpecifyMachineService  extends IDocService<LhSpecifyMachine>{

    /**
     * 根据工厂编号和规格代码查询
     * @param factoryCode
     * @param specCodes
     * @return
     */
    List<LhSpecifyMachine> queryByFactoryCodeAndSpecCodes(String factoryCode,Set<String> specCodes);

}
