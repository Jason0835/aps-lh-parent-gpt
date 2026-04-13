package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.service.ILhMachineOnlineInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 硫化在机信息Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMachineOnlineInfoServiceImpl extends AbstractDocService<LhMachineOnlineInfo> implements ILhMachineOnlineInfoService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "LH_MACHINE_ONLINE";
    }

    @Override
    public int saveOrUpdateBatch(List<LhMachineOnlineInfo> list) {
        baseDao.insertBatch(list);
        return list.size();
    }
}
