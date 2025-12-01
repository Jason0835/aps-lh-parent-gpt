package com.zlt.aps.cx.service;

import com.zlt.aps.cxlh.cx.api.domain.entity.CxMatchingSpecifyMachineList;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxMachineInfoVo;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface CxMatchingSpecifyMachineService {

    /**
     * 详情列表
     *
     * @param cxMatchingSpecifyMachineList
     * @return
     */
    public List<CxMatchingSpecifyMachineList> viewList(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);


    /**
     * title 获取可用的成型机
     * @param scheduleDate 计划日期
     * @return List<MdmMoldingMachine> 可用成型机列表
     */
    public Map<String, CxMachineInfoVo> getAvailableMoldingMachine(Date scheduleDate);
}
