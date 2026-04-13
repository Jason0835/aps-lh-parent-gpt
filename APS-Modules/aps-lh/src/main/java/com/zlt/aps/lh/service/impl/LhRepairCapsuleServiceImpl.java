package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.service.ILhRepairCapsuleService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 胶囊已使用次数Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LhRepairCapsuleServiceImpl extends AbstractDocService<LhRepairCapsule> implements ILhRepairCapsuleService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "LH_REPAIR_CAPSULE";
    }

    @Override
    public int saveOrUpdateBatch(List<LhRepairCapsule> list) {
        baseDao.insertBatch(list);
        return list.size();
    }
}
