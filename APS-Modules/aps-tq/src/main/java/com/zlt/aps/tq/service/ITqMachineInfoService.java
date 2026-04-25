package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ITqMachineInfoService extends IDocService<TqMachineInfo> {

    String checkUnique(TqMachineInfo machineInfo);

    String checkMachineCodeUnique(TqMachineInfo machineInfo);

    List<TqMachineInfo> listMachineInfo(TqMachineInfo machineInfo);

    /**
     * 导出胎圈机台列表
     *
     * @param machineInfo 查询条件
     * @return 机台列表
     */
    List<TqMachineInfo> selectMachineInfoList(TqMachineInfo machineInfo);
}
