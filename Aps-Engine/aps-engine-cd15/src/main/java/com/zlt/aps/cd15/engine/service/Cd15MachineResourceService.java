package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;

import java.time.LocalDateTime;

/**
 * 当前班次机台试算基础数据加载服务。
 */
public interface Cd15MachineResourceService {

    Cd15MachineResourceSnapshot load(String factoryCode,
                                     LocalDateTime shiftStart,
                                     LocalDateTime shiftEnd);
}
