package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.mapper.CxMachineOnlineInfoMapper;
import com.zlt.aps.cx.service.ICxMachineOnlineInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型在机信息Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CxMachineOnlineInfoServiceImpl extends AbstractDocService<CxMachineOnlineInfo> implements ICxMachineOnlineInfoService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "CX_MACHINE_ONLINE";
    }

    @Override
    public int saveOrUpdateBatch(List<CxMachineOnlineInfo> list) {
        baseDao.insertBatch(list);
        return list.size();
    }
}
