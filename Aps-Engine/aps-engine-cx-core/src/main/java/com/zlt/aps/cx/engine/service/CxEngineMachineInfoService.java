package com.zlt.aps.cx.engine.service;


import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;

import java.util.List;
import java.util.Map;

/**
  * 成型机台信息相关逻辑处理业务接口
  * @ClassName CxEngineMachineInfoService
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/24 16:00
  * @Version 1.0
**/
public interface CxEngineMachineInfoService {
    public List<CxMachineInfo> selectCxMachineInfoList(CxMachineInfo cxMachineInfo);

    /**
     * 加载全部成型机台信息
     * @return
     */
    public Map<String,CxMachineInfo> loadCxMachineInfoMap();
}
