package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.mapper.TqEngineMapper;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S4: 结果校验与持久化Handler。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>分离外协排程数据和非外协排程数据</li>
 *   <li>同步排程数据到日志表，删除历史数据</li>
 *   <li>创建自动排程记录</li>
 *   <li>合并已有排程记录（重排场景下保留已发布数据）</li>
 *   <li>批量保存排程结果</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqResultPersistHandler extends AbsTqScheduleStepHandler {

    @Resource
    private TqEngineMapper tqEngineMapper;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Override
    protected String getStepName() {
        return "S4-结果校验与持久化";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        String scheduleDate = context.getScheduleDate();
        String batchNo = context.getBatchNo();
        String cxBatchNo = context.getCxBatchNo();
        Map<String, String> assistSpecMap = context.getAssistSpecMap();
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();

        // 1. 分离外协排程数据
        List<TqScheduleResultVo> assistScheduleList = scheduleList.stream()
                .filter(r -> assistSpecMap.containsKey(r.getBeadCode()))
                .collect(Collectors.toList());
        List<TqScheduleResultVo> normalScheduleList = scheduleList.stream()
                .filter(r -> !assistSpecMap.containsKey(r.getBeadCode()))
                .collect(Collectors.toList());
        context.setAssistScheduleList(assistScheduleList);
        context.setNormalScheduleList(normalScheduleList);

        log.info("[S4] 外协排程数据:{}条, 非外协排程数据:{}条", assistScheduleList.size(), normalScheduleList.size());

        // 2. 同步排程数据到日志表，删除历史数据
        syncTqScheduleToLog(scheduleDate);

        // 3. 创建自动排程记录
        createScheduleRecord(scheduleDate, cxBatchNo, batchNo);

        // 4. 批量新增外协排程结果数据
        if (CollectionUtils.isNotEmpty(assistScheduleList)) {
            tqEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);
            log.info("[S4] 外协排程结果保存完成, 记录数:{}", assistScheduleList.size());
        }

        // 5. 查询已有排程记录并合并
        List<TqScheduleResultVo> existScheduleList = tqEngineMapper.listTqEnginSchedule(scheduleDate);
        context.setExistScheduleList(existScheduleList);
        normalScheduleList = mergeExistSchedule(batchNo, normalScheduleList, existScheduleList);

        // 6. 批量新增非外协排程结果数据
        if (CollectionUtils.isNotEmpty(normalScheduleList)) {
            tqEngineMapper.batchCreateScheduleResult(normalScheduleList);
            context.setInsertedCount(normalScheduleList.size());
            log.info("[S4] 非外协排程结果保存完成, 记录数:{}", normalScheduleList.size());
        }

        autoScheduleLogService.insertTqScheduleLog(batchNo, "",
                "自动排程完成", "排程记录数:" + scheduleList.size()
                        + ", 外协:" + assistScheduleList.size()
                        + ", 非外协:" + normalScheduleList.size());
    }

    /**
     * 把排程数据同步到日志表，删除历史排程数据。
     */
    private void syncTqScheduleToLog(String scheduleDate) {
        tqEngineMapper.syncTqScheduleToLog(scheduleDate);
        tqEngineMapper.deleteTqSchedule(scheduleDate);
        tqEngineMapper.deleteTqAssistSchedule(scheduleDate);
    }

    /**
     * 创建自动排程记录。
     */
    private void createScheduleRecord(String scheduleDate, String cxBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("cxBatchNo", cxBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());
        tqEngineMapper.createScheduleRecord(params);
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到自动排程的列表中。
     *
     * <p>合并规则：</p>
     * <ul>
     *   <li>重排前已发布且只有一条记录：沿用原订单号和机台，计划量按新排程</li>
     *   <li>重排前已发布且有多条记录：保留原排产数据，新排程计划量写入备注</li>
     *   <li>重排前未找到记录：直接使用新排程数据</li>
     *   <li>重排前已发布但新排程中没有的规格：保留原排产数据</li>
     * </ul>
     */
    private List<TqScheduleResultVo> mergeExistSchedule(String batchNo, List<TqScheduleResultVo> autoScheduleList,
                                                         List<TqScheduleResultVo> existScheduleList) {
        if (CollectionUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }

        List<TqScheduleResultVo> mergeList = new ArrayList<>();

        // 拿到重排前已经发布给MES的排产数据，key为胎圈代码
        Map<String, List<TqScheduleResultVo>> existScheduleMap = existScheduleList.stream()
                .filter(s -> s.getPublishSuccessCount() != null && s.getPublishSuccessCount() > 0)
                .collect(Collectors.groupingBy(TqScheduleResultVo::getBeadCode));

        for (TqScheduleResultVo autoSchedule : autoScheduleList) {
            List<TqScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getBeadCode());

            if (existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                // 对应规格重排前已发布，且只有一条排程记录（只对应了一个机台）
                TqScheduleResultVo existSchedule = existScheduleGroupList.get(0);
                autoSchedule.setOrderNo(existSchedule.getOrderNo());
                autoSchedule.setPublishSuccessCount(existSchedule.getPublishSuccessCount());
                autoSchedule.setNewestPublishTime(existSchedule.getNewestPublishTime());
                autoSchedule.setIsRelease(ApsConstant.WAIT_RELEASING);
                autoSchedule.setMachineId(existSchedule.getMachineId());
                mergeList.add(autoSchedule);
            } else if (existScheduleGroupList != null && existScheduleGroupList.size() > 1) {
                // 对应规格重排前已发布，且有多条排程记录（对应了多个机台）
                String remarkTip = I18nUtil.getMessage("reschedule.double.spec.remark2");
                remarkTip = StringUtils.format(remarkTip,
                        stripZeros(autoSchedule.getClass1PlanQty()),
                        stripZeros(autoSchedule.getClass2PlanQty()),
                        stripZeros(autoSchedule.getClass3PlanQty()),
                        stripZeros(autoSchedule.getClass4PlanQty()),
                        stripZeros(autoSchedule.getClass5PlanQty()),
                        stripZeros(autoSchedule.getClass6PlanQty()));
                for (TqScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                // 对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getBeadCode());
        }

        // 重排前的已发布规格如果没有在重排后的列表中，也需要保留
        for (List<TqScheduleResultVo> list : existScheduleMap.values()) {
            list.forEach(r -> r.setBatchNo(batchNo));
            mergeList.addAll(list);
        }

        return mergeList;
    }

    /**
     * 去除数字末尾的0（如 1200.0 → 1200）
     */
    private String stripZeros(Double value) {
        if (value == null) {
            return "0";
        }
        if (value == value.longValue()) {
            return String.valueOf(value.longValue());
        }
        return String.valueOf(value);
    }
}
