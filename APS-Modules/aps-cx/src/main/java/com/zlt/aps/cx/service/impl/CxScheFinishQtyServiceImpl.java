package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.aps.cx.mapper.CxScheFinishQtyMapper;
import com.zlt.aps.cx.service.ICxScheFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型排程完成量回报Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CxScheFinishQtyServiceImpl extends AbstractDocService<CxScheFinishQty> implements ICxScheFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "CX_SCHE_FINISH";
    }

    @Override
    public int saveOrUpdateBatch(List<CxScheFinishQty> list) {
        baseDao.saveBatch(list);
        return list.size();
    }
}
