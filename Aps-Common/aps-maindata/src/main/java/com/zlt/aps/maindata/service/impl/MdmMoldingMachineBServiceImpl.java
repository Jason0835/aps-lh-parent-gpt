package com.zlt.aps.maindata.service.impl;

import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineB;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import com.zlt.aps.maindata.service.IMdmMoldingMachineBService;
import com.zlt.bill.common.service.AbstractDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachineBServiceImpl.java
 * 描    述：MdmMoldingMachineBServiceImpl基础数据-成型机子业务层处理
 *@author zlt
 *@date 2025-02-18
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
public class MdmMoldingMachineBServiceImpl extends AbstractDocService<MdmMoldingMachineB>  implements IMdmMoldingMachineBService {
    @Override
    protected String getDocTypeCode() {
        return "0117MAIN";
    }
}


