package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.mapper.MdmMouldCleanPlanMapper;
import com.zlt.aps.maindata.mapper.MdmMouldCleanPlanEntityMapper;
import com.zlt.aps.maindata.service.IMdmMouldCleanPlanService;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanPlan;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanWarn;
import com.zlt.aps.mdm.api.enums.DataSourceEnum;
import com.zlt.aps.mdm.api.enums.MouldCleanTypeEnum;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * APS模具清洗计划Service实现
 *
 * @author APS Team
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmMouldCleanPlanServiceImpl extends AbstractDocService<MdmMouldCleanPlan> implements IMdmMouldCleanPlanService {

    @Resource
    private MdmMouldCleanPlanMapper mdmMouldCleanPlanMapper;

    @Resource
    private MdmMouldCleanPlanEntityMapper mdmMouldCleanWarnEntityMapper;

    @Autowired
    private ISysConfigService configService;

    @Override
    protected String getDocTypeCode() {
        return "0";
    }

    @Override
    public int syncFromMouldCleanWarn() {
        log.info("开始从模具清洗预警同步生成模具清洗计划");

        List<MdmMouldCleanWarn> warnList = mdmMouldCleanWarnEntityMapper.selectList(
            new LambdaQueryWrapper<MdmMouldCleanWarn>()
                .eq(MdmMouldCleanWarn::getIsDelete, 0)
        );

        if (CollectionUtils.isEmpty(warnList)) {
            log.info("模具清洗预警数据为空，无需同步");
            return 0;
        }

        String daysConfig = configService.selectConfigByKey("mould.clean.plan.days");
        int days = StringUtil.isBlank(daysConfig) ? 25 : Integer.parseInt(daysConfig);
        log.info("清洗计划生成天数配置：{}天", days);

        Map<String, List<MdmMouldCleanWarn>> groupByMachine = 
            warnList.stream().collect(Collectors.groupingBy(MdmMouldCleanWarn::getLhCode));

        List<MdmMouldCleanPlan> planList = new ArrayList<>();

        for (Map.Entry<String, List<MdmMouldCleanWarn>> entry : groupByMachine.entrySet()) {
            String lhCode = entry.getKey();
            List<MdmMouldCleanWarn> machineWarnList = entry.getValue();

            machineWarnList.sort((a, b) -> {
                if (a.getOperTime() == null && b.getOperTime() == null) return 0;
                if (a.getOperTime() == null) return 1;
                if (b.getOperTime() == null) return -1;
                return b.getOperTime().compareTo(a.getOperTime());
            });

            MdmMouldCleanWarn latestWarn = machineWarnList.get(0);

            MdmMouldCleanPlan plan = generateCleanPlan(latestWarn, days);
            if (plan != null) {
                planList.add(plan);
            }
        }

        if (CollectionUtils.isNotEmpty(planList)) {
            for (MdmMouldCleanPlan plan : planList) {
                LambdaQueryWrapper<MdmMouldCleanPlan> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MdmMouldCleanPlan::getLhCode, plan.getLhCode());
                wrapper.eq(MdmMouldCleanPlan::getFactoryCode, plan.getFactoryCode());
                MdmMouldCleanPlan existPlan = mdmMouldCleanPlanMapper.selectOne(wrapper);

                if (existPlan != null) {
                    plan.setId(existPlan.getId());
                    plan.setUpdateBy(existPlan.getCreateBy());
                    plan.setUpdateTime(new Date());
                    mdmMouldCleanPlanMapper.updateById(plan);
                } else {
                    plan.setCreateBy("SYSTEM");
                    plan.setCreateTime(new Date());
                    mdmMouldCleanPlanMapper.insert(plan);
                }
            }
        }

        log.info("从模具清洗预警同步生成模具清洗计划完成，共生成{}条", planList.size());
        return planList.size();
    }

    private MdmMouldCleanPlan generateCleanPlan(MdmMouldCleanWarn warn, int days) {
        MdmMouldCleanPlan plan = new MdmMouldCleanPlan();
        plan.setLhCode(warn.getLhCode());
        plan.setFactoryCode(warn.getFactoryCode());
        plan.setCompanyCode(warn.getCompanyCode());
        plan.setDataSource(DataSourceEnum.SYSTEM.getCode());

        Date cleanTime = null;
        String cleanType = null;

        if (warn.getSecondWashTime() != null) {
            cleanType = MouldCleanTypeEnum.SAND_BLAST.getCode();
            LocalDate baseDate = toLocalDate(warn.getSecondWashTime());
            cleanTime = Date.from(baseDate.plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        else if (warn.getFirstWashTime() != null && warn.getSecondWashTime() == null) {
            cleanType = MouldCleanTypeEnum.DRY_ICE.getCode();
            LocalDate baseDate = toLocalDate(warn.getFirstWashTime());
            cleanTime = Date.from(baseDate.plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        else if (warn.getOperTime() != null && warn.getFirstWashTime() == null 
                 && warn.getSecondWashTime() == null) {
            cleanType = MouldCleanTypeEnum.DRY_ICE.getCode();
            LocalDate baseDate = toLocalDate(warn.getOperTime());
            cleanTime = Date.from(baseDate.plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        else if (warn.getOperTime() == null && warn.getFirstWashTime() == null 
                 && warn.getSecondWashTime() == null) {
            cleanType = MouldCleanTypeEnum.DRY_ICE.getCode();
            cleanTime = Date.from(LocalDate.now().plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        if (cleanTime != null && cleanType != null) {
            plan.setCleanTime(cleanTime);
            plan.setCleanType(cleanType);
            return plan;
        }

        return null;
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @Override
    public AjaxResult importData(List<MdmMouldCleanPlan> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<MdmMouldCleanPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmMouldCleanPlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmMouldCleanPlan docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                }
                importList.add(docEntity);
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = baseDao.saveBatch(importList);

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public String checkUnique(MdmMouldCleanPlan docEntityVO) {
        QueryWrapper<MdmMouldCleanPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getFieldValueByFieldName("id")), "ID", docEntityVO.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", docEntityVO.getFactoryCode());
        queryWrapper.eq("LH_CODE", docEntityVO.getLhCode());

        if (mdmMouldCleanPlanMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "lhCode");
    }
}
