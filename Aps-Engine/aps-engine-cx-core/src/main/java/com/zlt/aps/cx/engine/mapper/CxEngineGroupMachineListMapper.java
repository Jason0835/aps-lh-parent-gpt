package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineMachineGroupList;

import java.util.List;

/**
 * 成型机台组投产班次接口
 */
public interface CxEngineGroupMachineListMapper {

    /**
     * 根据条件进行机台投产班次查询
     * @param cxEngineMachineGroupList
     * @return
     */
    List<CxEngineMachineGroupList> selectCxEngineGroupMachineListList(CxEngineMachineGroupList cxEngineMachineGroupList);

    CxEngineMachineGroupList selectCxEngineGroupMachineListById(Long id);
}
