package com.zlt.aps.common.engine.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.domain.IFinishQtyImport;
import com.zlt.aps.common.core.enums.HalfComponentFinishTableEnum;
import com.zlt.aps.common.engine.mapper.BaseFinishQtyImportMapper;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 半部件各个工序完成量导入基础服务类
 * @author zlt
 */
@Service
public class BaseFinishQtyImportService {

    @Autowired
    private BaseFinishQtyImportMapper baseFinishQtyImportMapper;

    /**
     * 导入完成量
     * @param list 要导入的半部件完成量集合
     */
    public AjaxResult importFinishQty(List<? extends IFinishQtyImport> list, Long importLogId
            , HalfComponentFinishTableEnum halfComponentFinishTableEnum) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<IFinishQtyImport> importList = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        String orderNoColumnName = I18nUtil.getMessage("ui.data.column.dayFinishQty.orderNo");
//        String dateColumnName = I18nUtil.getMessage("ui.data.column.dayFinishQty.scheduleDate");
        // 按业务主键分组
        Map<String, Long> groupMap = list.stream().filter(item -> StringUtils.isNotBlank(item.getOrderNo())).collect(Collectors.groupingBy(IFinishQtyImport::getOrderNo, Collectors.counting()));
        /*Map<String, Long> groupMap1 = list.stream().filter(item -> Objects.nonNull(item.getScheduleDate()))
                .collect(Collectors.groupingBy(item ->
                String.join("|", DateUtils.parseDateToStr("yyyy-MM-dd", item.getScheduleDate()),
                        item.getCodeField(), item.getCodeField1()), Collectors.counting()));*/
        for (int i = 0; i < list.size(); i++) {
            IFinishQtyImport qtyImport = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, i + 2, qtyImport);
            // excel内业务主键唯一校验
            Long hasValue = groupMap.getOrDefault(qtyImport.getOrderNo(), 0L);
            if (hasValue > 1) {
                addImportErrorLog(importLogId, i + 2,
                        String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), orderNoColumnName),
                        validated);
            }

            /*if (Objects.nonNull(qtyImport.getScheduleDate())) {
                Long hasValue1 = groupMap1.get(String.join("|", DateUtils.parseDateToStr("yyyy-MM-dd", qtyImport.getScheduleDate()),
                        qtyImport.getCodeField(), qtyImport.getCodeField1()));
                if (hasValue1 > 1) {
                    addImportErrorLog(importLogId, i + 2,
                            String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"),
                                    dateColumnName + "+代码"),
                            validated);
                }
            }*/
            // 业务校验，暂不校验

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                qtyImport.setBaseVale(null);
                importList.add(qtyImport);
            }
        }
        if (CollectionUtils.isNotEmpty(importList)) {
            try {
                successNum = saveBatch(importList, importList.get(0).getScheduleDate(), halfComponentFinishTableEnum);
            } catch (Exception e) {
                e.printStackTrace();
                // 执行sql失败，插入导入失败记录
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 批量保存半部件完成量
     *
     * @param finishQtyImportList          结果集合
     * @param halfComponentFinishTableEnum 半部件完成量表名枚举
     */
    public int saveBatch(List<? extends IFinishQtyImport> finishQtyImportList, Date scheduleDate, HalfComponentFinishTableEnum halfComponentFinishTableEnum) {
        baseFinishQtyImportMapper.removeDayFinishQtyByDate(halfComponentFinishTableEnum.getFinishQtyTableName(), scheduleDate);
        int result = baseFinishQtyImportMapper.saveDayFinishQtyList(halfComponentFinishTableEnum.getFinishQtyTableName(),
                finishQtyImportList,
                halfComponentFinishTableEnum.getCodeColumnName(),
                halfComponentFinishTableEnum.getCodeColumnName1(),
                halfComponentFinishTableEnum.getClass1PlanQtyColumnName(),
                halfComponentFinishTableEnum.getClass2PlanQtyColumnName());
        // 完成量汇总表
        baseFinishQtyImportMapper.removeDayFinishQtyByDate(halfComponentFinishTableEnum.getFinishTotalTableName(), scheduleDate);
        baseFinishQtyImportMapper.saveDayFinishQtyTotalByDayFinish(
                halfComponentFinishTableEnum.getFinishQtyTableName(),
                halfComponentFinishTableEnum.getFinishTotalTableName(),
                halfComponentFinishTableEnum.getCodeColumnName(),
                halfComponentFinishTableEnum.getCodeColumnName1(),
                halfComponentFinishTableEnum.getClass1PlanQtyColumnName(),
                halfComponentFinishTableEnum.getClass2PlanQtyColumnName());
        return result;
    }
}
