package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import com.zlt.aps.lh.service.ILhMoldAlterPlanFinishService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模具交替计划完成回报Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMoldAlterPlanFinishServiceImpl extends AbstractDocService<LhMoldAlterPlanFinish> implements ILhMoldAlterPlanFinishService {

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "LH_MOLD_ALTER_PLAN_FINISH";
    }

    @Override
    public int saveOrUpdateBatch(List<LhMoldAlterPlanFinish> list) {
        baseDao.saveBatch(list);
        return list.size();
    }
}
