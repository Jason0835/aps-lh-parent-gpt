package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.engine.mapper.CxEngineMachineInfoMapper;
import com.zlt.aps.cx.engine.service.CxEngineMachineInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成型工序引擎获取机台相关逻辑层实现
 */
@Service("cxEngineMachineInfoService")
@Slf4j
public class CxEngineMachineInfoServiceImpl implements CxEngineMachineInfoService {

    @Autowired
    private CxEngineMachineInfoMapper cxEngineMachineInfoMapper;

    /**
     * 根据条件获取成型机台列表信息
     * @param cxMachineInfo
     * @return
     */
    @Override
    public List<CxMachineInfo> selectCxMachineInfoList(CxMachineInfo cxMachineInfo) {
        return cxEngineMachineInfoMapper.selectCxMachineInfoList(cxMachineInfo);
    }

    /**
     * 获取全部成型机台数据组装为集合
     * @return
     */
    @Override
    public Map<String, CxMachineInfo> loadCxMachineInfoMap() {
        Map<String, CxMachineInfo> cxMachineInfoMap =null;
        List<CxMachineInfo> cxMachineInfoList=this.selectCxMachineInfoList(new CxMachineInfo());
        if(StringUtils.isNotEmpty(cxMachineInfoList)){
            cxMachineInfoMap=new HashMap<>();
            for(CxMachineInfo cxMachineInfo:cxMachineInfoList){
                cxMachineInfoMap.put(cxMachineInfo.getMachineCode(),cxMachineInfo);
            }
        }
        return cxMachineInfoMap;
    }
}
