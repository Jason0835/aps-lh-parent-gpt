package com.zlt.aps.maindata.service;


import com.zlt.aps.maindata.domain.entity.CxScheduleResultSearchVo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.monthplan.api.domain.vo.BaseMoldingMachineInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineProNumVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMoldingMachineService.java
 * 描    述：IMdmMoldingMachineService基础数据-成型机档案后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-18
 */
public interface IMdmMoldingMachineService extends IDocService<MdmMoldingMachine> {


    /**
     * 根据工厂编码和机台编码获取成型机信息
     *
     * @param factoryCode
     * @param machineCode
     * @return
     */
    MdmMoldingMachine getMoldingMachineByMachineCode(String factoryCode, String machineCode);

    /**
     * 根据工厂编码取成型机寸口 + 成型机成型法 机台最大数
     *
     * @param factoryCode 工厂
     * @return List<MdmMoldingMachineProNumVo>
     */
    List<MdmMoldingMachineProNumVo> getMoldingMachineProNum(String factoryCode);

    /**
     * 根据分厂等条件，获取成型产能配置信息
     *
     * @param cxScheduleResultSearchVo
     * @return
     */
    List<BaseMoldingMachineInfoVo> getCurrencyMachineInfo(CxScheduleResultSearchVo cxScheduleResultSearchVo);
}
