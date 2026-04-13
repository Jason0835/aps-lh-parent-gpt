package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.cx.mapper.CxDayFinishQtyMapper;
import com.zlt.aps.cx.service.ICxDayFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型排程日完成量Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CxDayFinishQtyServiceImpl extends AbstractDocService<CxDayFinishQty> implements ICxDayFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "CX_DAY_FINISH";
    }

    @Override
    public int saveOrUpdateBatch(List<CxDayFinishQty> list) {
        baseDao.saveBatch(list);
        return list.size();
    }
}
