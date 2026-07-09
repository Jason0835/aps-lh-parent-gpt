package com.zlt.aps.nc.service;

import java.util.List;

import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.bill.common.service.IDocService;

/**
 * 内衬机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface NcMachineInfoService extends IDocService<NcMachineInfo> {
    /**
     * 获取机台表
     * 
     * @param queryParams
     * @return
     */
    List<NcMachineInfo> selectMachineInfoList(NcMachineInfo queryParams);
}
