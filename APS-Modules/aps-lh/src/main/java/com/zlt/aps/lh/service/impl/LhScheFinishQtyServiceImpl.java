package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.aps.lh.service.ILhScheFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 硫化排程完成量回报Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LhScheFinishQtyServiceImpl extends AbstractDocService<LhScheFinishQty> implements ILhScheFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "LH_SCHE_FINISH";
    }

    @Override
    public int saveOrUpdateBatch(List<LhScheFinishQty> list) {
        baseDao.saveBatch(list);
        return list.size();
    }
}
