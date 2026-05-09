package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.mapper.LhMachineOnlineInfoMapper;
import com.zlt.aps.lh.service.ILhMachineOnlineInfoService;
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
 * 硫化在机信息Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMachineOnlineInfoServiceImpl extends AbstractDocService<LhMachineOnlineInfo> implements ILhMachineOnlineInfoService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private LhMachineOnlineInfoMapper lhMachineOnlineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "LH_MACHINE_ONLINE";
    }

    @Override
    public int saveOrUpdateBatch(List<LhMachineOnlineInfo> list) {
        baseDao.insertBatch(list);
        return list.size();
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, String updateBy, List<LhMachineOnlineInfo> insertList) {
        log.info("硫化在机同步-事务开始：逻辑删除分厂{}旧数据，待插入数量={}", factoryCode, CollectionUtils.size(insertList));
        lhMachineOnlineInfoMapper.logicDeleteByFactoryCode(factoryCode, updateBy, new Date());
        log.info("硫化在机同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<LhMachineOnlineInfo> subList = insertList.subList(i, end);
                baseDao.insertBatch(subList);
                log.info("硫化在机同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("硫化在机同步-事务完成：分厂{}，插入数量={}", factoryCode, CollectionUtils.size(insertList));
    }
}
