package com.zlt.aps.maindata.service.impl;

import com.zlt.aps.maindata.mapper.MdmLhRepairCapsuleEntityMapper;
import com.zlt.aps.maindata.service.IMdmLhRepairCapsuleService;
import com.zlt.aps.mdm.api.domain.entity.MdmLhRepairCapsule;
import com.zlt.bill.common.service.AbstractDocService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 胶囊已使用次数Service实现
 *
 * @author zlt
 * @since 2025/12/25
 */
@Service
public class MdmLhRepairCapsuleServiceImpl extends AbstractDocService<MdmLhRepairCapsule> implements IMdmLhRepairCapsuleService {

    @Resource
    private MdmLhRepairCapsuleEntityMapper mdmLhRepairCapsuleEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0";
    }

}
