package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.aps.gsq.mapper.GsqStockMapper;
import com.zlt.aps.gsq.service.IGsqStockService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 钢丝圈库存管理Service实现
 *
 * @author zlt
 * @date 2026-07-08
 */
@Slf4j
@Service
public class GsqStockServiceImpl extends AbstractDocService<GsqStock>
        implements IGsqStockService {

    @Resource
    private GsqStockMapper gsqStockMapper;

    /**
     * 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "GSQ_STEEL_RING_STOCK";
    }

    /**
     * 唯一性校验字段：库存日期+钢丝圈代码
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return java.util.Arrays.asList("stockDate", "steelRingCode");
    }

    /**
     * 校验"库存日期+钢丝圈代码"组合唯一性
     * 框架已自动过滤逻辑删除数据，无需手动追加 IS_DELETE 条件
     *
     * @param entity 实体
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(GsqStock entity) {
        LambdaQueryWrapper<GsqStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(entity.getId() != null, GsqStock::getId, entity.getId());
        wrapper.eq(GsqStock::getStockDate, entity.getStockDate());
        wrapper.eq(GsqStock::getSteelRingCode, entity.getSteelRingCode());
        wrapper.eq(GsqStock::getIsDelete, "0");
        if (gsqStockMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据，并保存记录
     * 校验规则：钢丝圈代码、库存日期、库存量必填；按"库存日期+钢丝圈代码"校验重复
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GsqStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqStock> importList = new ArrayList<>();

        // 按"库存日期+钢丝圈代码"分组，识别文件内重复数据
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(
                        a -> (a.getStockDate() + "_" + a.getSteelRingCode()),
                        Collectors.counting()));

        // 逐行校验
        for (int i = 0; i < list.size(); i++) {
            GsqStock entity = list.get(i);

            // 文件内重复校验
            String groupKey = entity.getStockDate() + "_" + entity.getSteelRingCode();
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                entity.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.gsq.stock.stockDate")
                        + "+" + I18nUtil.getMessage("ui.data.column.gsq.stock.steelRingCode");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            // 字段格式校验（必填+数据类型）
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            if (validated.isEmpty()) {
                importList.add(entity);
            } else {
                failureNum++;
                entity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 保存：updateSupport=true 走 mergeSql（存在则更新）；否则逐条校验唯一后 save
        try {
            if (updateSupport && !importList.isEmpty()) {
                successNum = importList.size();
                gsqStockMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    GsqStock excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = gsqStockMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.gsq.stock.conflict"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("导入钢丝圈库存管理异常", e);
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
