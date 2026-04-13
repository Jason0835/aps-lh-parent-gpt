package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxMesStock;
import com.zlt.aps.cx.mapper.CxMesStockMapper;
import com.zlt.aps.cx.service.ICxMesStockService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 生胎库存Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CxMesStockServiceImpl extends AbstractDocService<CxMesStock> implements ICxMesStockService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "CX_MES_STOCK";
    }

    @Override
    public int saveOrUpdateBatch(List<CxMesStock> list) {
        baseDao.insertBatch(list);
        return list.size();
    }
}
