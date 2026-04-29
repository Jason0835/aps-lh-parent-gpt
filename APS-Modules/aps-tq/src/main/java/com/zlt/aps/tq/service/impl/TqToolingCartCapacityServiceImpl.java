package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqToolingCartCapacity;
import com.zlt.aps.tq.mapper.TqToolingCartCapacityMapper;
import com.zlt.aps.tq.service.ITqToolingCartCapacityService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

@Slf4j
@Service
public class TqToolingCartCapacityServiceImpl extends AbstractDocService<TqToolingCartCapacity> implements ITqToolingCartCapacityService {

    @Resource
    private TqToolingCartCapacityMapper tqToolingCartCapacityMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ_TOOLING_CART_CAPACITY";
    }

    @Override
    public String checkUnique(TqToolingCartCapacity entity) {
        QueryWrapper<TqToolingCartCapacity> wrapper = new QueryWrapper<>();
        wrapper.ne(entity.getId() != null, "ID", entity.getId());
        wrapper.eq("CART_CODE", entity.getCartCode());
        wrapper.eq("MATERIAL_CODE", entity.getMaterialCode());
        wrapper.eq("IS_DELETE", 0);
        if (tqToolingCartCapacityMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("cartCode", "materialCode");
    }

    @Override
    public void deleteAllToolingCartCapacity() {
        tqToolingCartCapacityMapper.deleteAllToolingCartCapacity();
    }

    @Override
    public AjaxResult importData(List<TqToolingCartCapacity> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqToolingCartCapacity> importList = new ArrayList<>();

        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(a -> (a.getCartCode() + a.getMaterialCode()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqToolingCartCapacity entity = list.get(i);

            Long hasValue = groupMap.get(entity.getCartCode() + entity.getMaterialCode());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                entity.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.tq.toolingCartCapacity.column.cartCode");
                String columnName2 = I18nUtil.getMessage("ui.tq.toolingCartCapacity.column.materialCode");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            if (validated.isEmpty()) {
                importList.add(entity);
            } else {
                failureNum++;
                entity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            if (updateSupport && !importList.isEmpty()) {
                successNum = importList.size();
                tqToolingCartCapacityMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqToolingCartCapacity excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = tqToolingCartCapacityMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.tq.toolingCartCapacity.column.conflict"), importErrorLogs);
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
