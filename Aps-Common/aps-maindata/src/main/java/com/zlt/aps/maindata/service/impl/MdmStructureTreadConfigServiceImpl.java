package com.zlt.aps.maindata.service.impl;

import com.zlt.aps.maindata.mapper.MdmStructureTreadConfigEntityMapper;
import com.zlt.aps.maindata.service.IMdmStructureTreadConfigService;
import com.zlt.aps.mp.api.domain.entity.MdmStructureTreadConfig;
import com.zlt.bill.common.service.AbstractDocService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * APS结构整车胎面配置Service实现
 *
 * @author zlt
 * @since 2025/12/25
 */
@Service
public class MdmStructureTreadConfigServiceImpl extends AbstractDocService<MdmStructureTreadConfig> implements IMdmStructureTreadConfigService {

    @Resource
    private MdmStructureTreadConfigEntityMapper mdmStructureTreadConfigEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0";
    }

}
