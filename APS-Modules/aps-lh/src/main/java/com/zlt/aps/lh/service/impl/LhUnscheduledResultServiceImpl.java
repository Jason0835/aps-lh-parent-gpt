package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.service.ILhUnscheduledResultService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhUnscheduledResultServiceImpl.java
 * 描    述：LhUnscheduledResultServiceImpl硫化未排结果业务层处理
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhUnscheduledResultServiceImpl extends AbstractDocService<LhUnscheduledResult>  implements ILhUnscheduledResultService {
    @Override
    protected String getDocTypeCode() {
        return "0207";
    }



}
