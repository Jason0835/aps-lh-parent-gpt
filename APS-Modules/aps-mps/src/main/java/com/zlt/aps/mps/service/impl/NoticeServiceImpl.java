package com.zlt.aps.mps.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.alibaba.nacos.shaded.com.google.common.base.Objects;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.mail.api.EmailMessage;
import com.ruoyi.mail.api.service.IEmailRemoteService;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.mps.api.domain.ProductionReminderUser;
import com.zlt.aps.mps.api.domain.UnfinishedScheduleResult;
import com.zlt.aps.mps.mapper.NoticeMapper;
import com.zlt.aps.mps.service.INoticeService;

import lombok.extern.slf4j.Slf4j;

/**
 * 消息通知接口实现类
 * 
 * @author zlt
 *
 */
@Service
@Slf4j
public class NoticeServiceImpl implements INoticeService {
    @Autowired
    private IEmailRemoteService iEmailRemoteService;
    @Autowired
    private NoticeMapper noticeMapper;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Override
    public AjaxResult unfinishedSchedule() {
        // 加载通知人
        List<ProductionReminderUser> users = noticeMapper.queryProductionReminder();
        if (CollectionUtils.isEmpty(users)) {
            String errorMsg = "没有配置通知人，不需要通知";
            log.info(errorMsg);
            return AjaxResult.success(errorMsg);
        }
        Date scheduleDate = DateUtil.thatDay(DateUtil.now());
        // 加载未完成规格信息
        List<UnfinishedScheduleResult> resultList = noticeMapper.queryUnfinishedSchedule(scheduleDate);
        if (CollectionUtils.isEmpty(resultList)) {
            String errorMsg = "没有未完成的情况，不需要通知";
            log.info(errorMsg);
            return AjaxResult.success(errorMsg);
        }

        // 工序类型编号
        Map<String, String> processesMap = iSysDictDataCacheService.getType("PROCEDURE_CODE").stream()
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (d1, d2) -> d1));

        // 通知人与通知信息配对，仅保留两边都有的记录
        Map<String, List<ProductionReminderUser>> userMap = users.stream()
                .collect(Collectors.groupingBy(ProductionReminderUser::getProcesses));
        Map<String, List<UnfinishedScheduleResult>> resultMap = resultList.stream()
                .collect(Collectors.groupingBy(UnfinishedScheduleResult::getProcesses));

        String scheduleDateStr = DateUtil.formatDate(scheduleDate);
        // 找到有配置人且有待通知消息的工序
        for (Entry<String, List<UnfinishedScheduleResult>> entry : resultMap.entrySet()) {
            String processesKey = entry.getKey();
            List<UnfinishedScheduleResult> result = entry.getValue();
            List<ProductionReminderUser> matchUser = userMap.get(processesKey);
            String processesName = processesMap.getOrDefault(processesKey, processesKey); // 工序名称
            if (CollectionUtils.isEmpty(matchUser)) {
                log.info(String.format("有未完成的情况，但是没配置该工序的通知人，不通知，工序：%s", processesName));
                continue;
            }

            String email = matchUser.stream().map(ProductionReminderUser::getEmail).distinct()
                    .collect(Collectors.joining(";"));
            // 邮件标题，内容：%s%s工序计划完成情况
            String title = String.format(I18nUtil.getMessage("notice.unfinishedSchedule.email.title"), scheduleDateStr, processesName);
            // 邮件正文，内容：各位领导好，附件为%s%s工序计划完成情况，请查核。
            String content = String.format(I18nUtil.getMessage("notice.unfinishedSchedule.email.content"), scheduleDateStr, processesName);
            // 生成excel
            try {
                byte[] excelByte = this.createExcel(result, title, scheduleDateStr, processesName);
                // 构建邮件发送信息
                EmailMessage emailMessage = new EmailMessage();
                emailMessage.setReceiverEmail(email); // 接收人，通知工序相同的人设置群发
                emailMessage.setTitle(title);
                emailMessage.setContent(content);
                emailMessage.setAttachmentNameList(Collections.singletonList(title + ".xlsx")); // 附件名称
                emailMessage.setAttachmentList(Collections.singletonList(excelByte)); // 附件信息
                AjaxResult ajaxResult = iEmailRemoteService.sendMail(emailMessage);
                if (Objects.equal(ajaxResult.get(AjaxResult.CODE_TAG), HttpStatus.ERROR)) {
                    log.error(String.valueOf(ajaxResult.get(AjaxResult.MSG_TAG)));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                continue;
            }

        }
        return AjaxResult.success();
    }

    /**
     * 构建附件Excel
     * 
     * @param list          数据
     * @param sheetName     页签名称
     * @param scheduleDate  排产日期
     * @param processesName 工序名称
     * @return
     * @throws Exception
     */
    private byte[] createExcel(List<UnfinishedScheduleResult> list, String sheetName, String scheduleDate,
            String processesName) throws Exception {
        // 读取Excel模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("excelModel/UnfinishedScheduleResult.xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);
        Sheet sheet = webBook.getSheetAt(0);
        webBook.setSheetName(0, sheetName);
        // 填充数据
        for (int i = 0, size = list.size(); i < size; i++) {
            int n = 0;
            UnfinishedScheduleResult scheduleResult = list.get(i);
            Row row = sheet.createRow(i + 1); // 从第二行开始
            row.createCell(n++).setCellValue(scheduleDate);
            row.createCell(n++).setCellValue(processesName);
            row.createCell(n++).setCellValue(StringUtils.defaultString(scheduleResult.getMachineName()));
            row.createCell(n++).setCellValue(StringUtils.defaultString(scheduleResult.getMatrialCode()));
            row.createCell(n++).setCellValue(BigDecimalUtil.defaultDoubleValue(scheduleResult.getDayPlanQty()));
            row.createCell(n++).setCellValue(BigDecimalUtil.defaultDoubleValue(scheduleResult.getDayFinishQty()));
            row.createCell(n++).setCellValue(StringUtils.defaultString(scheduleResult.getDayFinishRate()));
            row.createCell(n++).setCellValue(StringUtils.defaultString(scheduleResult.getDayHandAnalysis()));
//            row.createCell(n++).setCellValue(BigDecimalUtil.defaultDoubleValue(scheduleResult.getNightPlanQty()));
//            row.createCell(n++).setCellValue(BigDecimalUtil.defaultDoubleValue(scheduleResult.getNightFinishQty()));
//            row.createCell(n++).setCellValue(StringUtils.defaultString(scheduleResult.getNightFinishRate()));
//            row.createCell(n++).setCellValue(StringUtils.defaultString(scheduleResult.getNightHandAnalysis()));
        }

        // 写出字节流
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            webBook.write(out);
            return out.toByteArray();
        }
    }
}
