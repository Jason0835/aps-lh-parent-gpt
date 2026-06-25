package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90MachineResourceSnapshot;

import java.time.LocalDateTime;

/**
 * 当前班次机台试算基础数据加载服务。
 */
public interface Cd90MachineResourceService {

    Cd90MachineResourceSnapshot load(String factoryCode,
                                     LocalDateTime shiftStart,
                                     LocalDateTime shiftEnd);
}
