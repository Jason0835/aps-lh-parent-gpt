package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.cx.mapper.CxDayFinishQtyMapper;
import com.zlt.aps.cx.service.ICxDayFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 成型排程日完成量Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CxDayFinishQtyServiceImpl extends AbstractDocService<CxDayFinishQty> implements ICxDayFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private CxDayFinishQtyMapper cxDayFinishQtyMapper;

    @Override
    protected String getDocTypeCode() {
        return "CX_DAY_FINISH";
    }

    @Override
    public int saveOrUpdateBatch(List<CxDayFinishQty> list) {
        baseDao.saveBatch(list);
        return list.size();
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date finishDate, String updateBy, List<CxDayFinishQty> insertList) {
        log.info("成型排程日完成量同步-事务开始：逻辑删除分厂{}完成日期为{}的旧数据，待插入数量={}", factoryCode, finishDate, CollectionUtils.size(insertList));
        cxDayFinishQtyMapper.logicDeleteByFactoryCodeAndFinishDate(factoryCode, finishDate, updateBy, new Date());
        log.info("成型排程日完成量同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<CxDayFinishQty> subList = insertList.subList(i, end);
                baseDao.saveBatch(subList);
                log.info("成型排程日完成量同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("成型排程日完成量同步-事务完成：分厂{}，完成日期={}，插入数量={}", factoryCode, finishDate, CollectionUtils.size(insertList));
    }
}
