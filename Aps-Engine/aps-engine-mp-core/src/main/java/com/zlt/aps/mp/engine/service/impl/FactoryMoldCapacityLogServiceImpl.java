package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.mp.api.domain.entity.MpSkuMoldCapacityAllocateLog;
import com.zlt.aps.mp.engine.mapper.FactoryMoldCapacityLogMapper;
import com.zlt.aps.mp.engine.service.IFactoryMoldCapacityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMoldCapacityLogServiceImpl.java
 * 描    述：FactoryMoldCapacityLogServiceImplS2-0405.排产过程_计划模具受限日志业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-05-15
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FactoryMoldCapacityLogServiceImpl extends ServiceImpl<FactoryMoldCapacityLogMapper, MpSkuMoldCapacityAllocateLog> implements IFactoryMoldCapacityLogService {

}
