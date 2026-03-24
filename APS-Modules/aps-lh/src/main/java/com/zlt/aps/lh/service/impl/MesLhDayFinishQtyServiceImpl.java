package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.MesLhDayFinishQty;
import com.zlt.aps.lh.service.IMesLhDayFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MesLhDayFinishQtyServiceImpl.java
 * 描    述：MesLhDayFinishQtyServiceImpl硫化排程日完成量回报接口业务层处理
 *@author zlt
 *@date 2025-02-21
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
public class MesLhDayFinishQtyServiceImpl extends AbstractDocService<MesLhDayFinishQty>  implements IMesLhDayFinishQtyService {
    @Override
    protected String getDocTypeCode() {
        return "0108";
    }



}
