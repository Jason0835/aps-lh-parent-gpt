package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqTooling;
import com.zlt.aps.tq.mapper.TqToolingMapper;
import com.zlt.aps.tq.service.ITqToolingService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

@Slf4j
@Service
public class TqToolingServiceImpl extends AbstractDocService<TqTooling> implements ITqToolingService {

    @Resource
    private TqToolingMapper tqToolingMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ_TOOLING";
    }

    @Override
    public String checkUnique(TqTooling tooling) {
        QueryWrapper<TqTooling> wrapper = new QueryWrapper<>();
        wrapper.ne(tooling.getId() != null, "ID", tooling.getId());
        wrapper.eq("TOOLING_CODE", tooling.getToolingCode());
        wrapper.eq("IS_DELETE", 0);
        if (tqToolingMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Collections.singletonList("toolingCode");
    }

    @Override
    public void deleteAllTooling() {
        tqToolingMapper.deleteAllTooling();
    }

    @Override
    public AjaxResult importData(List<TqTooling> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqTooling> importList = new ArrayList<>();

        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(TqTooling::getToolingCode, Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqTooling tooling = list.get(i);

            Long hasValue = groupMap.get(tooling.getToolingCode());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                tooling.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.tq.tooling.column.toolingCode");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, tooling);
            if (validated.isEmpty()) {
                importList.add(tooling);
            } else {
                failureNum++;
                tooling.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            if (updateSupport && !importList.isEmpty()) {
                successNum = importList.size();
                tqToolingMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqTooling excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = tqToolingMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.tq.tooling.column.conflict"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
