package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public void logicDeleteAndSaveBatch(String factoryCode, Date onlineDate, String updateBy, List<LhMachineOnlineInfo> insertList) {
        log.info("硫化在机同步-事务开始：逻辑删除分厂{}在线日期为{}的旧数据，待插入数量={}", factoryCode, onlineDate, CollectionUtils.size(insertList));
        lhMachineOnlineInfoMapper.logicDeleteByFactoryCodeAndOnlineDate(factoryCode, onlineDate, updateBy, new Date());
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
        log.info("硫化在机同步-事务完成：分厂{}，在线日期={}，插入数量={}", factoryCode, onlineDate, CollectionUtils.size(insertList));
    }

    /**
     * 按维度键UPSERT批量保存（MES同步新模式）
     * 维度键 = 工厂 + 机台 + 日期
     * 键存在则更新（命中已逻辑删除的行会回填ID并复活），不存在则插入；
     * 不做对账删除，MES未上报的既有键保持原状
     *
     * @param list 待保存的数据列表
     * @return 实际处理的数据条数
     */
    @Override
    public int upsertBatch(List<LhMachineOnlineInfo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }
        log.info("硫化在机同步-UPSERT开始：待处理数量={}", list.size());

        // 同批次内先按维度键去重（保留最后一条），避免重复键引发插入冲突
        Map<String, LhMachineOnlineInfo> keyMap = new LinkedHashMap<>();
        for (LhMachineOnlineInfo item : list) {
            keyMap.put(this.buildUniqueKey(item), item);
        }
        List<LhMachineOnlineInfo> upsertList = new ArrayList<>(keyMap.values());

        // 查询维度键已存在的数据（包含已逻辑删除的行）
        List<LhMachineOnlineInfo> existsList = lhMachineOnlineInfoMapper.selectByUniqueKeyList(upsertList);
        Map<String, LhMachineOnlineInfo> existsMap = existsList.stream()
                .collect(Collectors.toMap(this::buildUniqueKey, Function.identity(), (v1, v2) -> v1));

        // 命中维度键：回填ID并复活（IS_DELETE置0），saveBatch按ID走更新；未命中：无ID走插入
        int updateCount = 0;
        for (LhMachineOnlineInfo item : upsertList) {
            LhMachineOnlineInfo existsData = existsMap.get(this.buildUniqueKey(item));
            if (existsData != null) {
                item.setId(existsData.getId());
                item.setIsDelete(0);
                updateCount++;
            }
        }

        // 分批保存（saveBatch内部按ID有无区分更新/插入）
        int batchSize = 1000;
        for (int i = 0; i < upsertList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, upsertList.size());
            baseDao.saveBatch(upsertList.subList(i, end));
        }

        log.info("硫化在机同步-UPSERT完成：总数={}，更新={}，插入={}", upsertList.size(), updateCount, upsertList.size() - updateCount);
        return upsertList.size();
    }

    /**
     * 构建维度键：工厂 + 机台 + 日期（yyyy-MM-dd）
     * 硫化一台机一行，左右模/在机模号均为行内字段，键不含物料维度，MES换规格时同键直接覆盖更新
     *
     * @param item 在机信息
     * @return 维度键字符串
     */
    private String buildUniqueKey(LhMachineOnlineInfo item) {
        String onlineDateStr = item.getOnlineDate() == null ? "" : DateUtil.formatDate(item.getOnlineDate());
        return item.getFactoryCode() + "|" + item.getLhCode() + "|" + onlineDateStr;
    }
}
