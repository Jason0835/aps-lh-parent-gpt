package com.zlt.aps.gdyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.mapper.GdyyScheduleResultMapper;
import com.zlt.aps.gdyy.service.IGdyyScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 钢带压延排程结果 服务实现。
 * 自动排程算法不在本服务中实现，本服务只负责排程结果的 CRUD 入口。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class GdyyScheduleResultServiceImpl extends AbstractDocService<GdyyScheduleResult> implements IGdyyScheduleResultService {

    @Resource
    private GdyyScheduleResultMapper gdyyScheduleResultMapper;

    @Override
    protected String getDocTypeCode() {
        return "GDYY_SCHEDULE_RESULT";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("GDYY_SCHEDULE_RESULT");
        return t;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "scheduleDate", "bigRollCode", "machineCode");
    }

    @Override
    public String checkUnique(GdyyScheduleResult entity) {
        LambdaQueryWrapper<GdyyScheduleResult> w = new LambdaQueryWrapper<>();
        w.eq(GdyyScheduleResult::getFactoryCode, entity.getFactoryCode());
        w.eq(GdyyScheduleResult::getScheduleDate, entity.getScheduleDate());
        w.eq(GdyyScheduleResult::getBigRollCode, entity.getBigRollCode());
        w.eq(GdyyScheduleResult::getMachineCode, entity.getMachineCode());
        w.ne(entity.getId() != null, GdyyScheduleResult::getId, entity.getId());
        return gdyyScheduleResultMapper.selectCount(w) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<GdyyScheduleResult> list, boolean updateSupport, Long importLogId) {
        int sn = 0, fn = 0;
        List<GdyyScheduleResult> il = new ArrayList<>();
        List<ImportErrorLog> el = new ArrayList<>();
        String um = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            GdyyScheduleResult de = list.get(i);
            List<ImportErrorLog> v = ImportExcelValidatedUtils.validated(importLogId, en, de);
            ImportExcelValidatedUtils.validatedRepeat(list, de, i, 2, importLogId, v,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(v)) {
                fn++;
                de.setId(-999L);
                el.addAll(v);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            GdyyScheduleResult de = list.get(i);
            if (de.getId() != null && de.getId() == -999L) continue;
            GdyyScheduleResult ex = getExist(de);
            if (ex == null) {
                de.setRowState(RowStateEnum.ADDED);
                il.add(de);
            } else if (updateSupport) {
                updateClassFields(ex, de);
                ex.setRemark(de.getRemark());
                gdyyScheduleResultMapper.updateById(ex);
                sn++;
            } else {
                fn++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, en, String.format(um, en), el);
            }
        }

        if (PubUtil.isEmpty(il) && sn == 0)
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + sn + "," + fn, el);
        if (CollectionUtils.isNotEmpty(il)) sn += baseDao.saveBatch(il);
        if (fn > 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + sn + "," + fn, el);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + sn);
    }

    @Override
    public AjaxResult changeQty(GdyyScheduleResult entity) {
        // 调量入口：只做框架入口，具体业务逻辑后续实现
        GdyyScheduleResult existing = gdyyScheduleResultMapper.selectById(entity.getId());
        if (existing == null) {
            return AjaxResult.error("记录不存在");
        }
        updateClassFields(existing, entity);
        // 修改计划量后更新发布状态为待发布
        existing.setIsRelease("5");
        gdyyScheduleResultMapper.updateById(existing);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult changeMachine(GdyyScheduleResult entity) {
        // 转机台入口：只做框架入口，具体业务逻辑后续实现
        GdyyScheduleResult existing = gdyyScheduleResultMapper.selectById(entity.getId());
        if (existing == null) {
            return AjaxResult.error("记录不存在");
        }
        existing.setMachineCode(entity.getMachineCode());
        // 转机台后更新发布状态为待发布
        existing.setIsRelease("5");
        gdyyScheduleResultMapper.updateById(existing);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult publish(GdyyScheduleResult entity) {
        // 发布入口：只做框架入口，具体MES发布逻辑后续实现
        return AjaxResult.error("发布功能待实现");
    }

    @Override
    public AjaxResult changeReleaseStatus(GdyyScheduleResult entity) {
        GdyyScheduleResult existing = gdyyScheduleResultMapper.selectById(entity.getId());
        if (existing == null) {
            return AjaxResult.error("记录不存在");
        }
        existing.setIsRelease(entity.getIsRelease());
        gdyyScheduleResultMapper.updateById(existing);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult getSummaryVo(GdyyScheduleResult queryVO) {
        // 获取合计信息入口：只做框架入口，具体合计逻辑后续实现
        return AjaxResult.success();
    }

    @Override
    public AjaxResult importFinishQty(List<GdyyScheduleResult> list, boolean updateSupport, Long importLogId) {
        // 导入完成量入口：只做框架入口，具体逻辑后续实现
        return AjaxResult.error("导入完成量功能待实现");
    }

    private GdyyScheduleResult getExist(GdyyScheduleResult entity) {
        LambdaQueryWrapper<GdyyScheduleResult> w = new LambdaQueryWrapper<>();
        w.eq(GdyyScheduleResult::getFactoryCode, entity.getFactoryCode());
        w.eq(GdyyScheduleResult::getScheduleDate, entity.getScheduleDate());
        w.eq(GdyyScheduleResult::getBigRollCode, entity.getBigRollCode());
        w.eq(GdyyScheduleResult::getMachineCode, entity.getMachineCode());
        return gdyyScheduleResultMapper.selectOne(w);
    }

    /** 将源对象的班次字段更新到目标对象。 */
    private void updateClassFields(GdyyScheduleResult target, GdyyScheduleResult source) {
        target.setClass1ScheduleDate(source.getClass1ScheduleDate());
        target.setClass1PlanQty(source.getClass1PlanQty());
        target.setClass1CxPlanQty(source.getClass1CxPlanQty());
        target.setClass1FinishQty(source.getClass1FinishQty());
        target.setClass1ProduceOrder(source.getClass1ProduceOrder());
        target.setClass1FinishRate(source.getClass1FinishRate());
        target.setClass1Analysis(source.getClass1Analysis());
        target.setClass1AnalysisInput(source.getClass1AnalysisInput());

        target.setClass2ScheduleDate(source.getClass2ScheduleDate());
        target.setClass2PlanQty(source.getClass2PlanQty());
        target.setClass2CxPlanQty(source.getClass2CxPlanQty());
        target.setClass2FinishQty(source.getClass2FinishQty());
        target.setClass2ProduceOrder(source.getClass2ProduceOrder());
        target.setClass2FinishRate(source.getClass2FinishRate());
        target.setClass2Analysis(source.getClass2Analysis());
        target.setClass2AnalysisInput(source.getClass2AnalysisInput());

        target.setClass3ScheduleDate(source.getClass3ScheduleDate());
        target.setClass3PlanQty(source.getClass3PlanQty());
        target.setClass3CxPlanQty(source.getClass3CxPlanQty());
        target.setClass3FinishQty(source.getClass3FinishQty());
        target.setClass3ProduceOrder(source.getClass3ProduceOrder());
        target.setClass3FinishRate(source.getClass3FinishRate());
        target.setClass3Analysis(source.getClass3Analysis());
        target.setClass3AnalysisInput(source.getClass3AnalysisInput());

        target.setClass4ScheduleDate(source.getClass4ScheduleDate());
        target.setClass4PlanQty(source.getClass4PlanQty());
        target.setClass4CxPlanQty(source.getClass4CxPlanQty());
        target.setClass4FinishQty(source.getClass4FinishQty());
        target.setClass4ProduceOrder(source.getClass4ProduceOrder());
        target.setClass4FinishRate(source.getClass4FinishRate());
        target.setClass4Analysis(source.getClass4Analysis());
        target.setClass4AnalysisInput(source.getClass4AnalysisInput());

        target.setClass5ScheduleDate(source.getClass5ScheduleDate());
        target.setClass5PlanQty(source.getClass5PlanQty());
        target.setClass5CxPlanQty(source.getClass5CxPlanQty());
        target.setClass5FinishQty(source.getClass5FinishQty());
        target.setClass5ProduceOrder(source.getClass5ProduceOrder());
        target.setClass5FinishRate(source.getClass5FinishRate());
        target.setClass5Analysis(source.getClass5Analysis());
        target.setClass5AnalysisInput(source.getClass5AnalysisInput());

        target.setClass6ScheduleDate(source.getClass6ScheduleDate());
        target.setClass6PlanQty(source.getClass6PlanQty());
        target.setClass6CxPlanQty(source.getClass6CxPlanQty());
        target.setClass6FinishQty(source.getClass6FinishQty());
        target.setClass6ProduceOrder(source.getClass6ProduceOrder());
        target.setClass6FinishRate(source.getClass6FinishRate());
        target.setClass6Analysis(source.getClass6Analysis());
        target.setClass6AnalysisInput(source.getClass6AnalysisInput());

        target.setClass7ScheduleDate(source.getClass7ScheduleDate());
        target.setClass7PlanQty(source.getClass7PlanQty());
        target.setClass7CxPlanQty(source.getClass7CxPlanQty());
        target.setClass7FinishQty(source.getClass7FinishQty());
        target.setClass7ProduceOrder(source.getClass7ProduceOrder());
        target.setClass7FinishRate(source.getClass7FinishRate());
        target.setClass7Analysis(source.getClass7Analysis());
        target.setClass7AnalysisInput(source.getClass7AnalysisInput());

        target.setClass8ScheduleDate(source.getClass8ScheduleDate());
        target.setClass8PlanQty(source.getClass8PlanQty());
        target.setClass8CxPlanQty(source.getClass8CxPlanQty());
        target.setClass8FinishQty(source.getClass8FinishQty());
        target.setClass8ProduceOrder(source.getClass8ProduceOrder());
        target.setClass8FinishRate(source.getClass8FinishRate());
        target.setClass8Analysis(source.getClass8Analysis());
        target.setClass8AnalysisInput(source.getClass8AnalysisInput());
    }
}
