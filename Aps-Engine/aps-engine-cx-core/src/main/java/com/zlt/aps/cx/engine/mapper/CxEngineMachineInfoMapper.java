package com.zlt.aps.cx.engine.mapper;


import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;

import java.util.List;

/**
 * 成型机台信息mapper接口
 */
public interface CxEngineMachineInfoMapper {

    /**
     * 加载成型机台列表信息
     * 过滤掉删除标识和禁用标识的机台
     * @param cxMachineInfo
     * @return
     */
    public List<CxMachineInfo> selectCxMachineInfoList(CxMachineInfo cxMachineInfo);
}
