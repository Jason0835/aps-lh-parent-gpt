package com.zlt.aps.dj.service;

import java.util.List;

import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.bill.common.service.IDocService;

/**
 * 垫胶机台信息Service接口
 *
 * @author zlt
 * @date 2026-05-28
 */
public interface DjMachineInfoService extends IDocService<DjMachineInfo> {
    /**
     * 获取机台表
     * @param queryParams
     * @return
     */
    List<DjMachineInfo> selectMachineInfoList(DjMachineInfo queryParams);
}
