package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineSpecifyMachine;

import java.util.List;
import java.util.Map;

/**
  * 成型工序定点机台相关信息逻辑接口
  * @ClassName CxEngineSpecifyMachineService
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/24 16:50
  * @Version 1.0
**/
public interface CxEngineSpecifyMachineService {


    /**
     * 根据条件查询定点机台列表
     * @param cxEngineSpecifyMachine
     * @return
     */
    public List<CxEngineSpecifyMachine> selectCxEngineSpecifyMachineList(CxEngineSpecifyMachine cxEngineSpecifyMachine);

    /**
     * 获取组装成型工序定点相关信息
     * @return
     */
    Map<String, List<CxEngineSpecifyMachine>> getAllCxSpecifyMachineInfo(String jobType);

    /**
     * 验证成型机台限制
     * @param sapCode
     * @param embryoCode
     * @param machineCode
     * @param sb
     */
    public void validateSpecifyMachine(String sapCode, String embryoCode, String machineCode, StringBuilder sb);
}
