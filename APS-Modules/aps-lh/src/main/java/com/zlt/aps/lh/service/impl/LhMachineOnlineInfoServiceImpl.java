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
     * 先删后插批量保存（MES同步模式）
     * 批内按维度键（工厂+机台+日期）去重（保留最后一条）后，按（工厂+日期）分组逐组：
     * 先逻辑删除该（工厂+日期）下全部旧数据（实现该日全量对账：MES未上报的机台行会被清理），再插入新数据；
     * 整个操作处于类级事务内，删除或插入失败整体回滚，不会出现"删了没插"
     *
     * @param updateBy 更新者（MES同步传MES，清理任务传CLEAN_TASK）
     * @param list 待保存的数据列表
     * @return 实际处理的数据条数
     */
    @Override
    public int deleteAndSaveBatch(String updateBy, List<LhMachineOnlineInfo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }
        log.info("硫化在机同步-先删后插开始：待处理数量={}", list.size());

        // 同批次内先按维度键去重（保留最后一条），避免同键多行插入产生重复数据
        Map<String, LhMachineOnlineInfo> keyMap = new LinkedHashMap<>();
        for (LhMachineOnlineInfo item : list) {
            keyMap.put(this.buildUniqueKey(item), item);
        }
        List<LhMachineOnlineInfo> dedupList = new ArrayList<>(keyMap.values());

        // 按（工厂+日期）分组，组顺序保持插入顺序
        Map<String, List<LhMachineOnlineInfo>> groupMap = dedupList.stream()
                .collect(Collectors.groupingBy(this::buildDateGroupKey, LinkedHashMap::new, Collectors.toList()));

        // 逐组先删后插：先逻辑删除该（工厂+日期）下全部旧数据，再分批插入新数据
        Date now = new Date();
        int deleteCount = 0;
        for (Map.Entry<String, List<LhMachineOnlineInfo>> entry : groupMap.entrySet()) {
            LhMachineOnlineInfo firstItem = entry.getValue().get(0);
            // 在线日期为空时无法圈定删除范围，跳过删除仅插入
            if (firstItem.getOnlineDate() != null) {
                deleteCount += lhMachineOnlineInfoMapper.logicDeleteByFactoryCodeAndOnlineDate(
                        firstItem.getFactoryCode(), DateUtil.beginOfDay(firstItem.getOnlineDate()), updateBy, now);
            }

            // 分批插入（1000条/批）
            List<LhMachineOnlineInfo> groupList = entry.getValue();
            int batchSize = 1000;
            for (int i = 0; i < groupList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, groupList.size());
                baseDao.insertBatch(groupList.subList(i, end));
            }
        }

        log.info("硫化在机同步-先删后插完成：分组数={}，逻辑删除行数={}，插入数量={}", groupMap.size(), deleteCount, dedupList.size());
        return dedupList.size();
    }

    /**
     * 构建维度键：工厂 + 机台 + 日期（yyyy-MM-dd）
     * 硫化一台机一行，左右模/在机模号均为行内字段，键不含物料维度
     *
     * @param item 在机信息
     * @return 维度键字符串
     */
    private String buildUniqueKey(LhMachineOnlineInfo item) {
        String onlineDateStr = item.getOnlineDate() == null ? "" : DateUtil.formatDate(item.getOnlineDate());
        return item.getFactoryCode() + "|" + item.getLhCode() + "|" + onlineDateStr;
    }

    /**
     * 构建（工厂+日期）分组键：用于先删后插的对账范围圈定
     *
     * @param item 在机信息
     * @return 分组键字符串
     */
    private String buildDateGroupKey(LhMachineOnlineInfo item) {
        String onlineDateStr = item.getOnlineDate() == null ? "" : DateUtil.formatDate(item.getOnlineDate());
        return item.getFactoryCode() + "|" + onlineDateStr;
    }
}
