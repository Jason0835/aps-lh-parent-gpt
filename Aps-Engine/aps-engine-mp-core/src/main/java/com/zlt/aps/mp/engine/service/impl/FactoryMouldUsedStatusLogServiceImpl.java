package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.mp.engine.mapper.FactoryMouldUsedStatusLogMapper;
import com.zlt.aps.mp.engine.service.IFactoryMouldUsedStatusLogService;
import com.zlt.aps.monthplan.api.domain.entity.MpMouldUsedStatusLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMouldUseStatusLogServiceImpl.java
 * 描    述：MpMouldUseStatusLogServiceImplS2-0406.排产过程_模具可用状态日志业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-01-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FactoryMouldUsedStatusLogServiceImpl extends ServiceImpl<FactoryMouldUsedStatusLogMapper, MpMouldUsedStatusLog> implements IFactoryMouldUsedStatusLogService {

}
