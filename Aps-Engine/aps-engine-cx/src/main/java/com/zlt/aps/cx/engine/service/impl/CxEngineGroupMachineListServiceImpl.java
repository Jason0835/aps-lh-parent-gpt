package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.domain.CxEngineMachineGroupList;
import com.zlt.aps.cx.engine.mapper.CxEngineGroupMachineListMapper;
import com.zlt.aps.cx.engine.service.CxEngineGroupMachineListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成型机台设置的可投产班次逻辑层
 */
@Service("cxEngineGroupMachineListService")
public class CxEngineGroupMachineListServiceImpl implements CxEngineGroupMachineListService {

    @Autowired
    private CxEngineGroupMachineListMapper cxEngineGroupMachineListMapper;

    /**
     * 根据条件查询列表
     * @param cxEngineMachineGroupList
     * @return
     */
    @Override
    public List<CxEngineMachineGroupList> selectCxEngineGroupMachineListList(CxEngineMachineGroupList cxEngineMachineGroupList) {
        return cxEngineGroupMachineListMapper.selectCxEngineGroupMachineListList(cxEngineMachineGroupList);
    }

    @Override
    public CxEngineMachineGroupList selectCxEngineGroupMachineListById(Long id) {
        return cxEngineGroupMachineListMapper.selectCxEngineGroupMachineListById(id);
    }

    /**
     * 获取设定的机台投产班次表
     * @return
     */
    @Override
    public Map<String, Double> getCxMachineProudctShift() {
        Map<String, Double> machineProductShiftMap=new HashMap<>();
        List<CxEngineMachineGroupList> cxEngineMachineGroupLists=selectCxEngineGroupMachineListList(new CxEngineMachineGroupList());
        if(StringUtils.isNotEmpty(cxEngineMachineGroupLists)){
            machineProductShiftMap = cxEngineMachineGroupLists.stream().collect(Collectors.toMap(CxEngineMachineGroupList::getCxMachineCode, CxEngineMachineGroupList::getProductShift));
        }
        return machineProductShiftMap;
    }
}
