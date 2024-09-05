package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineMachineGroupList;

import java.util.List;
import java.util.Map;


/**
 * 成型损耗率设定Service接口
 * 
 * @author Joran.zhang
 * @date 2021-06-29
 */
public interface CxEngineGroupMachineListService
{
    /**
     * 根据条件进行机台投产班次查询
     * @param cxEngineMachineGroupList
     * @return
     */
    List<CxEngineMachineGroupList> selectCxEngineGroupMachineListList(CxEngineMachineGroupList cxEngineMachineGroupList);

    CxEngineMachineGroupList selectCxEngineGroupMachineListById(Long id);

    /**
     * 加载全部机台设置的投产班次信息
     * @return
     */
    Map<String,Double> getCxMachineProudctShift();
}
