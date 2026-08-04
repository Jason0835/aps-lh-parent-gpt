package com.zlt.aps.xwyy.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyShiftConfig;
import com.zlt.aps.xwyy.domain.vo.XwyyScheduleResultTemplateImportVO;
import com.zlt.aps.xwyy.mapper.XwyyScheduleResultMapper;
import com.zlt.aps.xwyy.mapper.XwyyShiftConfigMapper;
import com.zlt.aps.xwyy.service.IXwyyScheduleResultService;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

@Service
@Transactional(rollbackFor = Exception.class)
public class XwyyScheduleResultServiceImpl extends AbstractDocService<XwyyScheduleResult> implements IXwyyScheduleResultService {

    @Resource
    private XwyyScheduleResultMapper xwyyScheduleResultMapper;
    @Resource
    private XwyyShiftConfigMapper xwyyShiftConfigMapper;

    @Override
    protected String getDocTypeCode() {
        return "XWYY_SCHEDULE_RESULT";
    }

    @Override
    public AjaxResult autoSchedule(XwyyScheduleResult entity) {
        if (PubUtil.isEmpty(entity.getFactoryCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.required.factoryCode"));
        }
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.required.scheduleDate"));
        }
        // 检查是否已有排程结果
        // TODO: 实现自动排程算法，创建异步任务，返回 taskId
        return AjaxResult.success(I18nUtil.getMessage("ui.message.xwyyScheduleResult.autoSchedule.success"));
    }

    @Override
    public AjaxResult insert(XwyyScheduleResult entity) {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult changeMachine(XwyyScheduleResult entity) {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult adjustQty(XwyyScheduleResult entity) {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult publish(XwyyScheduleResult entity) {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult importData(List<XwyyScheduleResult> list, boolean updateSupport, Long importLogId) {
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success"));
    }

    /**
     * 按固定生产计划模板整体覆盖导入。
     * Excel的N2作为结果主排程日期，各CLASS日期按当前工厂启用班次配置计算。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importScheduleTemplate(List<XwyyScheduleResultTemplateImportVO> rows,
                                             XwyyScheduleResult condition,
                                             boolean updateSupport) {
        if (condition == null || !PubUtil.isNotEmpty(condition.getFactoryCode())) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.xwyyScheduleResult.importFactoryRequired"));
        }
        if (condition.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.xwyyScheduleResult.importDateRequired"));
        }
        if (rows == null || rows.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.xwyyScheduleResult.importEmpty"));
        }
        String factoryCode = condition.getFactoryCode().trim();
        java.util.Date scheduleDate = DateUtil.beginOfDay(condition.getScheduleDate());
        List<XwyyShiftConfig> shiftConfigs = this.xwyyShiftConfigMapper.selectList(
                new LambdaQueryWrapper<XwyyShiftConfig>()
                        .eq(XwyyShiftConfig::getFactoryCode, factoryCode)
                        .eq(XwyyShiftConfig::getIsActive, 1)
                        .orderByAsc(XwyyShiftConfig::getShiftOrder));
        Map<String, XwyyShiftConfig> shiftConfigMap = new HashMap<>();
        for (XwyyShiftConfig shiftConfig : shiftConfigs) {
            if (PubUtil.isNotEmpty(shiftConfig.getClassField())) {
                shiftConfigMap.put(shiftConfig.getClassField().trim().toUpperCase(), shiftConfig);
            }
        }
        boolean shiftConfigInvalid = IntStream.rangeClosed(1, 8)
                .mapToObj(classIndex -> shiftConfigMap.get("CLASS" + classIndex))
                .anyMatch(shiftConfig -> shiftConfig == null
                        || shiftConfig.getScheduleDay() == null);
        if (shiftConfigInvalid) {
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.column.xwyyScheduleResult.importShiftConfigInvalid"), factoryCode));
        }

        Set<String> bigRollCodes = new HashSet<>();
        List<String> errors = new ArrayList<>();
        List<XwyyScheduleResultTemplateImportVO> validRows = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            XwyyScheduleResultTemplateImportVO row = rows.get(index);
            // 模板第1行是隐藏字段键，第2至4行是标题和表头，明细从第5行开始
            int excelRow = index + 5;
            if (row == null || !PubUtil.isNotEmpty(row.getBigRollCode())) {
                errors.add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.column.xwyyScheduleResult.importRowRequired"), excelRow));
                continue;
            }
            List<BigDecimal> quantities = IntStream.rangeClosed(1, 8)
                    .mapToObj(classIndex -> (BigDecimal) row.getFieldValueByFieldName(
                            String.format("class%dPlanQty", classIndex)))
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
            if (quantities.stream().anyMatch(quantity -> quantity.signum() < 0)) {
                errors.add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.column.xwyyScheduleResult.importNegative"), excelRow));
                continue;
            }
            if (quantities.stream().noneMatch(quantity -> quantity.signum() > 0)) {
                continue;
            }
            String bigRollCode = row.getBigRollCode().trim();
            if (!bigRollCodes.add(bigRollCode)) {
                errors.add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.column.xwyyScheduleResult.importDuplicate"), excelRow));
                continue;
            }
            validRows.add(row);
        }
        if (!errors.isEmpty()) {
            return AjaxResult.error(String.join("<br>", errors));
        }
        if (validRows.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.xwyyScheduleResult.importEmpty"));
        }

        // 批次号：XWYY + 年月日 + 3位定长自增序号（每重新导入一次递增）；工单号：批次号 + 4位定长自增序号
        String batchPrefix = "XWYY" + DateUtil.format(scheduleDate, "yyyyMMdd");
        String batchNo = this.nextBatchNo(factoryCode, batchPrefix);
        int[] classOrders = new int[8];
        int orderSeq = 0;
        List<XwyyScheduleResult> insertList = new ArrayList<>();
        for (XwyyScheduleResultTemplateImportVO row : validRows) {
            XwyyScheduleResult result = new XwyyScheduleResult();
            result.setFactoryCode(factoryCode);
            result.setScheduleDate(scheduleDate);
            result.setBatchNo(batchNo);
            result.setOrderNo(batchNo + String.format("%04d", ++orderSeq));
            result.setBigRollCode(row.getBigRollCode().trim());
            result.setIsRelease("0");
            result.setProductionStatus("0");
            result.setDataSource("2");
            result.setExtraPlanFlag("0");
            result.setPublishSuccessCount(0);
            for (int classIndex = 1; classIndex <= 8; classIndex++) {
                XwyyShiftConfig shiftConfig = shiftConfigMap.get("CLASS" + classIndex);
                result.setFieldValueByFieldName(
                        String.format("class%dScheduleDate", classIndex),
                        DateUtil.offsetDay(scheduleDate, shiftConfig.getScheduleDay() - 2));
                BigDecimal planQty = (BigDecimal) row.getFieldValueByFieldName(
                        String.format("class%dPlanQty", classIndex));
                result.setFieldValueByFieldName(
                        String.format("class%dPlanQty", classIndex), planQty);
                // 生产顺位：Excel 已填则按导入值，未填则按文件行序逐班从 1 递增；仅对计划量大于 0 的班次生成
                if (planQty != null && planQty.signum() > 0) {
                    BigDecimal produceOrder = (BigDecimal) row.getFieldValueByFieldName(
                            String.format("class%dProduceOrder", classIndex));
                    if (produceOrder == null) {
                        produceOrder = BigDecimal.valueOf(++classOrders[classIndex - 1]);
                    }
                    result.setFieldValueByFieldName(
                            String.format("class%dProduceOrder", classIndex), produceOrder);
                }
            }
            insertList.add(result);
        }

        this.xwyyScheduleResultMapper.update(null,
                new LambdaUpdateWrapper<XwyyScheduleResult>()
                        .eq(XwyyScheduleResult::getFactoryCode, factoryCode)
                        .eq(XwyyScheduleResult::getScheduleDate, scheduleDate)
                        .set(XwyyScheduleResult::getIsDelete, 1));
        int successNum = this.baseDao.insertBatch(insertList);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success")
                + "," + successNum);
    }

    /**
     * 生成新批次号：按前缀取当前工厂未删除记录的最大序号并 +1。
     * 规则：XWYY + yyyyMMdd + 3位定长自增序号（每重新导入一次递增）。
     *
     * @param factoryCode 工厂编码
     * @param prefix      批次号前缀，如 XWYY20260804
     * @return 新批次号
     */
    private String nextBatchNo(String factoryCode, String prefix) {
        int maxSeq = this.xwyyScheduleResultMapper.selectList(
                        new LambdaQueryWrapper<XwyyScheduleResult>()
                                .eq(XwyyScheduleResult::getFactoryCode, factoryCode)
                                .likeRight(XwyyScheduleResult::getBatchNo, prefix))
                .stream()
                .map(XwyyScheduleResult::getBatchNo)
                .filter(Objects::nonNull)
                .map(batchNo -> batchNo.substring(prefix.length()))
                .filter(suffix -> suffix.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return prefix + String.format("%03d", maxSeq + 1);
    }

    @Override
    public byte[] exportData(List<XwyyScheduleResult> currentResults,
                             XwyyScheduleResult query) {
        Map<String, Object> tableMap = new HashMap<>();
        if (query != null && query.getScheduleDate() != null) {
            tableMap.put("previousDate", DateUtil.offsetDay(query.getScheduleDate(), -1));
            tableMap.put("scheduleDate", query.getScheduleDate());
            tableMap.put("nextDate", DateUtil.offsetDay(query.getScheduleDate(), 1));
            tableMap.put("nextTwoDate", DateUtil.offsetDay(query.getScheduleDate(), 2));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (XwyyScheduleResult result : currentResults) {
            Map<String, Object> row = new HashMap<>();
            row.put("bigRollCode", result.getBigRollCode());
            for (int classIndex = 1; classIndex <= 8; classIndex++) {
                row.put(String.format("class%dProduceOrder", classIndex),
                        result.getFieldValueByFieldName(
                                String.format("class%dProduceOrder", classIndex)));
                row.put(String.format("class%dPlanQty", classIndex),
                        result.getFieldValueByFieldName(
                                String.format("class%dPlanQty", classIndex)));
            }
            rows.add(row);
        }
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/xwyyScheduleResult.xlsx");
        if (inputStream == null) {
            throw new IllegalStateException("xwyyScheduleResult.xlsx");
        }
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap,
                Collections.singletonList(rows));
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("XWYY_SCHEDULE_RESULT");
        return t;
    }
}
