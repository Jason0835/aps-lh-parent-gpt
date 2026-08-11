package com.zlt.aps.gsq.service.impl;

import com.zlt.aps.gsq.api.domain.entity.GsqDayFinishQty;
import com.zlt.aps.gsq.mapper.GsqDayFinishQtyMapper;
import com.zlt.aps.gsq.service.IGsqDayFinishQtyService;
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
 * 钢丝圈排程日完成量回报Service实现
 *
 * @author APS Team
 * @since 2026/08/11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GsqDayFinishQtyServiceImpl extends AbstractDocService<GsqDayFinishQty> implements IGsqDayFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private GsqDayFinishQtyMapper gsqDayFinishQtyMapper;

    @Override
    protected String getDocTypeCode() {
        return "GSQ_DAY_FINISH";
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy, List<GsqDayFinishQty> insertList) {
        log.info("钢丝圈排程日完成量同步-事务开始：逻辑删除分厂{}排程日期为{}的旧数据，待插入数量={}", factoryCode, scheduleDate, CollectionUtils.size(insertList));
        gsqDayFinishQtyMapper.logicDeleteByFactoryCodeAndScheduleDate(factoryCode, scheduleDate, updateBy, new Date());
        log.info("钢丝圈排程日完成量同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<GsqDayFinishQty> subList = insertList.subList(i, end);
                baseDao.saveBatch(subList);
                log.info("钢丝圈排程日完成量同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("钢丝圈排程日完成量同步-事务完成：分厂{}，排程日期={}，插入数量={}", factoryCode, scheduleDate, CollectionUtils.size(insertList));
    }
}
