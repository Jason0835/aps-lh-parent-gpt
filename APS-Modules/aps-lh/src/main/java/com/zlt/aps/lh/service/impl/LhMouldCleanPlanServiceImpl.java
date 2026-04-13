package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanPlan;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.lh.mapper.LhMouldCleanPlanMapper;
import com.zlt.aps.lh.mapper.LhMouldCleanWarnMapper;
import com.zlt.aps.lh.service.ILhMouldCleanPlanService;
import com.zlt.aps.lh.service.ILhParamsService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMouldCleanPlanServiceImpl extends AbstractDocService<LhMouldCleanPlan> implements ILhMouldCleanPlanService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private LhMouldCleanPlanMapper lhMouldCleanPlanMapper;

    @Autowired
    private LhMouldCleanWarnMapper lhMouldCleanWarnMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ILhParamsService lhParamsService;

    @Override
    protected String getDocTypeCode() {
        return "LH_MOULD_CLEAN_PLAN";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("LH_MOULD_CLEAN_PLAN");
        return sysDocType;
    }

    @Override
    public int syncFromMouldCleanWarn() {
        String lockKey = "sync:mould:clean:plan";
        if (redisService.getCacheObject(lockKey) != null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.message.sync.in.progress"));
        }

        try {
            redisService.setCacheObject(lockKey, "1");

            List<LhMouldCleanWarn> warnList = lhMouldCleanWarnMapper.selectList(null);
            if (warnList == null || warnList.isEmpty()) {
                return 0;
            }

            Map<String, List<LhMouldCleanWarn>> machineMap = new HashMap<>();
            for (LhMouldCleanWarn warn : warnList) {
                String machineCode = extractMachineCode(warn.getLhCode());
                machineMap.computeIfAbsent(machineCode, k -> new ArrayList<>()).add(warn);
            }

            int cleanDays = getCleanDays();

            List<LhMouldCleanPlan> planList = new ArrayList<>();
            for (Map.Entry<String, List<LhMouldCleanWarn>> entry : machineMap.entrySet()) {
                String machineCode = entry.getKey();
                List<LhMouldCleanWarn> warns = entry.getValue();

                LhMouldCleanPlan plan = buildCleanPlan(machineCode, warns, cleanDays);
                planList.add(plan);
            }

            List<String> machineCodeList = planList.stream()
                    .map(LhMouldCleanPlan::getLhCode)
                    .collect(java.util.stream.Collectors.toList());

            QueryWrapper<LhMouldCleanPlan> existWrapper = new QueryWrapper<>();
            existWrapper.in("LH_CODE", machineCodeList);
            List<LhMouldCleanPlan> existingList = lhMouldCleanPlanMapper.selectList(existWrapper);

            Map<String, LhMouldCleanPlan> existingMap = existingList.stream()
                    .collect(java.util.stream.Collectors.toMap(LhMouldCleanPlan::getLhCode, p -> p, (a, b) -> a));

            List<LhMouldCleanPlan> insertList = new ArrayList<>();
            List<LhMouldCleanPlan> updateList = new ArrayList<>();

            for (LhMouldCleanPlan plan : planList) {
                LhMouldCleanPlan existing = existingMap.get(plan.getLhCode());
                if (existing != null) {
                    plan.setId(existing.getId());
                    plan.setUpdateBy("SYSTEM");
                    plan.setUpdateTime(new Date());
                    updateList.add(plan);
                } else {
                    plan.setCreateBy("SYSTEM");
                    plan.setCreateTime(new Date());
                    insertList.add(plan);
                }
            }

            if (!insertList.isEmpty()) {
                baseDao.insertBatch(insertList);
            }
            if (!updateList.isEmpty()) {
                baseDao.updateBatch(updateList);
            }

            return planList.size();
        } finally {
            redisService.deleteObject(lockKey);
        }
    }

    private String extractMachineCode(String lhCode) {
        if (lhCode == null) return null;
        return lhCode.replaceAll("\\s+[LR]$", "").trim();
    }

    private LhMouldCleanPlan buildCleanPlan(String machineCode, List<LhMouldCleanWarn> warns, int cleanDays) {
        LhMouldCleanPlan plan = new LhMouldCleanPlan();
        plan.setLhCode(machineCode);
        plan.setDataSource("1");
        plan.setCreateTime(new Date());
        plan.setCreateBy("SYSTEM");

        String leftRightMould = buildLeftRightMould(warns);
        plan.setLeftRightMould(leftRightMould);

        Date secondWashTime = null;
        Date firstWashTime = null;
        Date operTime = null;

        for (LhMouldCleanWarn warn : warns) {
            if (warn.getSecondWashTime() != null) {
                if (secondWashTime == null || warn.getSecondWashTime().after(secondWashTime)) {
                    secondWashTime = warn.getSecondWashTime();
                }
            }
            if (warn.getFirstWashTime() != null) {
                if (firstWashTime == null || warn.getFirstWashTime().after(firstWashTime)) {
                    firstWashTime = warn.getFirstWashTime();
                }
            }
            if (warn.getOperTime() != null) {
                if (operTime == null || warn.getOperTime().after(operTime)) {
                    operTime = warn.getOperTime();
                }
            }
        }

        Date cleanTime = null;
        String cleanType = null;

        if (secondWashTime != null) {
            cleanType = "02";
            cleanTime = DateUtil.offsetDay(secondWashTime, cleanDays);
        } else if (firstWashTime != null) {
            cleanType = "01";
            cleanTime = DateUtil.offsetDay(firstWashTime, cleanDays);
        } else if (operTime != null) {
            cleanType = "01";
            cleanTime = DateUtil.offsetDay(operTime, cleanDays);
        } else {
            cleanType = "01";
            cleanTime = DateUtil.offsetDay(new Date(), cleanDays);
        }

        plan.setCleanType(cleanType);
        plan.setCleanTime(cleanTime);

        if (!warns.isEmpty()) {
            plan.setFactoryCode(warns.get(0).getFactoryCode());
            plan.setCompanyCode(warns.get(0).getCompanyCode());
        }

        return plan;
    }

    private String buildLeftRightMould(List<LhMouldCleanWarn> warns) {
        Set<String> mouldSet = new HashSet<>();

        for (LhMouldCleanWarn warn : warns) {
            String lhCode = warn.getLhCode();
            if (lhCode != null) {
                if (lhCode.endsWith(" L")) {
                    mouldSet.add("L");
                } else if (lhCode.endsWith(" R")) {
                    mouldSet.add("R");
                }
            }
        }

        if (mouldSet.size() == 2) {
            return "LR";
        } else if (mouldSet.size() == 1) {
            return mouldSet.iterator().next();
        } else {
            return null;
        }
    }

    private int getCleanDays() {
        LhParams params = lhParamsService.selectOneByParamCode("MOULD_CLEAN_DAYS", null);
        if (params != null && params.getParamValue() != null) {
            try {
                return Integer.parseInt(params.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("清洗间隔天数参数配置错误，使用默认值25");
            }
        }
        return 25;
    }
}
