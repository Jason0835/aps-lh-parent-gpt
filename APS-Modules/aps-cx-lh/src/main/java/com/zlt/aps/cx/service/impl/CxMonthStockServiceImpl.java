package com.zlt.aps.cx.service.impl;

import com.zlt.aps.monthplan.api.domain.entity.CxMonthStock;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import com.zlt.aps.cx.service.ICxMonthStockService;
import com.zlt.bill.common.service.AbstractDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxMonthStockServiceImpl.java
 * 描    述：CxMonthStockServiceImpl成型工序胎胚月结库存业务层处理
 *@author zlt
 *@date 2025-02-17
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
public class CxMonthStockServiceImpl extends AbstractDocService<CxMonthStock>  implements ICxMonthStockService {
    @Override
    protected String getDocTypeCode() {
        return "9007CX";
    }
}


