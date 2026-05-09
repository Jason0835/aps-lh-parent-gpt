package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.mapper.CxMachineOnlineInfoMapper;
import com.zlt.aps.cx.service.ICxMachineOnlineInfoService;
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
 * 成型在机信息Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CxMachineOnlineInfoServiceImpl extends AbstractDocService<CxMachineOnlineInfo> implements ICxMachineOnlineInfoService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private CxMachineOnlineInfoMapper cxMachineOnlineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "CX_MACHINE_ONLINE";
    }

    @Override
    public int saveOrUpdateBatch(List<CxMachineOnlineInfo> list) {
        baseDao.insertBatch(list);
        return list.size();
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, String updateBy, List<CxMachineOnlineInfo> insertList) {
        log.info("成型在机同步-事务开始：逻辑删除分厂{}旧数据，待插入数量={}", factoryCode, CollectionUtils.size(insertList));
        cxMachineOnlineInfoMapper.logicDeleteByFactoryCode(factoryCode, updateBy, new Date());
        log.info("成型在机同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<CxMachineOnlineInfo> subList = insertList.subList(i, end);
                baseDao.insertBatch(subList);
                log.info("成型在机同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("成型在机同步-事务完成：分厂{}，插入数量={}", factoryCode, CollectionUtils.size(insertList));
    }
}
