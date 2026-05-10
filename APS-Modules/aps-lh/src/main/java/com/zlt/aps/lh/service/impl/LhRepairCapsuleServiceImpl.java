package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.mapper.LhRepairCapsuleMapper;
import com.zlt.aps.lh.service.ILhRepairCapsuleService;
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
 * 胶囊已使用次数Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhRepairCapsuleServiceImpl extends AbstractDocService<LhRepairCapsule> implements ILhRepairCapsuleService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private LhRepairCapsuleMapper lhRepairCapsuleMapper;

    @Override
    protected String getDocTypeCode() {
        return "LH_REPAIR_CAPSULE";
    }

    @Override
    public int saveOrUpdateBatch(List<LhRepairCapsule> list) {
        baseDao.insertBatch(list);
        return list.size();
    }

    /**
     * 查询导出数据
     *
     * @return 结果
     */
    @Override
    public String[] getQueryFormulas() {
        return new String[]{
                "materialDesc -> getcolvalue(T_MDM_MATERIAL_INFO, MATERIAL_DESC, MATERIAL_CODE, materialCode)",
        };
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, String updateBy, List<LhRepairCapsule> insertList) {
        log.info("胶囊已使用次数同步-事务开始：逻辑删除分厂{}旧数据，待插入数量={}", factoryCode, CollectionUtils.size(insertList));
        lhRepairCapsuleMapper.logicDeleteByFactoryCode(factoryCode, updateBy, new Date());
        log.info("胶囊已使用次数同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<LhRepairCapsule> subList = insertList.subList(i, end);
                baseDao.insertBatch(subList);
                log.info("胶囊已使用次数同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("胶囊已使用次数同步-事务完成：分厂{}，插入数量={}", factoryCode, CollectionUtils.size(insertList));
    }
}
