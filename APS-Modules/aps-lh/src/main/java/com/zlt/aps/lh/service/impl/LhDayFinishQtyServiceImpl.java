package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.mapper.LhDayFinishQtyMapper;
import com.zlt.aps.lh.service.ILhDayFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 硫化排程日完成量Service实现
 *
 * @author APS Team
 * @since 2026/04/13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhDayFinishQtyServiceImpl extends AbstractDocService<LhDayFinishQty> implements ILhDayFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private LhDayFinishQtyMapper lhDayFinishQtyMapper;

    @Override
    protected String getDocTypeCode() {
        return "LH_DAY_FINISH";
    }

    @Override
    public int saveOrUpdateBatch(List<LhDayFinishQty> list) {
        baseDao.saveBatch(list);
        return list.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<LhDayFinishQty> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<LhDayFinishQty> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhDayFinishQty docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (PubUtil.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhDayFinishQty docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (docEntity.getFinishDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.day.finish.qty.finishDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (StringUtil.isBlank(docEntity.getMaterialCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.day.finish.qty.materialCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (docEntity.getDayFinishQty() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.day.finish.qty.dayFinishQtyRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(docEntity.getFinishDate());
//            BigDecimal year = docEntity.getYear() != null ? docEntity.getYear() : new BigDecimal(cal.get(Calendar.YEAR));
            
            QueryWrapper<LhDayFinishQty> existWrapper = new QueryWrapper<>();
            existWrapper.eq("FINISH_DATE", docEntity.getFinishDate());
            existWrapper.eq("MATERIAL_CODE", docEntity.getMaterialCode());
            existWrapper.eq("FACTORY_CODE", docEntity.getFactoryCode());
            existWrapper.eq("IS_DELETE", 0);
            List<LhDayFinishQty> existList = lhDayFinishQtyMapper.selectList(existWrapper);
            
            if (!existList.isEmpty()) {
                for (LhDayFinishQty exist : existList) {
                    lhDayFinishQtyMapper.deleteById(exist.getId());
                }
            }
            
            docEntity.setIsDelete(0);
//            docEntity.setYear(year);
            
            importList.add(docEntity);
            successNum++;
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        baseDao.insertBatch(importList);

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum + "," + failureNum);
        }
    }

    @Override
    public List<LhDayFinishQty> selectList(LhDayFinishQty queryVO) {
        QueryWrapper<LhDayFinishQty> wrapper = new QueryWrapper<>();
        if (queryVO != null) {
            if (queryVO.getFactoryCode() != null) {
                wrapper.eq("FACTORY_CODE", queryVO.getFactoryCode());
            }
            if (queryVO.getMaterialCode() != null) {
                wrapper.eq("MATERIAL_CODE", queryVO.getMaterialCode());
            }
//            if (queryVO.getYear() != null) {
//                wrapper.eq("YEAR", queryVO.getYear());
//            }
        }
        wrapper.eq("IS_DELETE", 0);
        wrapper.orderByDesc("FINISH_DATE");
        return lhDayFinishQtyMapper.selectList(wrapper);
    }
}
