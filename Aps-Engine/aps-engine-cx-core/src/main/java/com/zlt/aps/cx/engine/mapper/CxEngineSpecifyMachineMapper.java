package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineSpecifyMachine;

import java.util.List;

/**
  * 加载成型工序定点机台列表信息
  * @ClassName CxEngineSpecifyMachineMapper
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/24 16:48
  * @Version 1.0
**/
public interface CxEngineSpecifyMachineMapper {

    /**
     * 加载成型工序定点机台信息
     * @param cxEngineSpecifyMachine
     * @return
     */
    public List<CxEngineSpecifyMachine> selectCxEngineSpecifyMachineList(CxEngineSpecifyMachine cxEngineSpecifyMachine);
}
