package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.mapper.CxStructureTreadConfigMapper;
import com.zlt.aps.cx.service.ICxStructureTreadConfigService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 结构整车胎面配置Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CxStructureTreadConfigServiceImpl extends AbstractDocService<CxStructureTreadConfig> implements ICxStructureTreadConfigService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "CX_STRUCTURE_TREAD";
    }

    @Override
    public int saveOrUpdateBatch(List<CxStructureTreadConfig> list) {
        baseDao.saveBatch(list);
        return list.size();
    }
}
