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
            log.info("开始从模具清洗预警同步数据");

            List<LhMouldCleanWarn> warnList = lhMouldCleanWarnMapper.selectList(null);
            if (warnList == null || warnList.isEmpty()) {
                log.info("模具清洗预警数据为空");
                return 0;
            }

            Map<String, List<LhMouldCleanWarn>> machineMap = new HashMap<>();
            for (LhMouldCleanWarn warn : warnList) {
                String machineCode = extractMachineCode(warn.getLhCode());
                if (machineCode != null) {
                    machineMap.computeIfAbsent(machineCode, k -> new ArrayList<>()).add(warn);
                }
            }

            int cleanDays = getCleanDays();
            Date now = new Date();

            Set<String> syncMachineCodes = machineMap.keySet();

            QueryWrapper<LhMouldCleanPlan> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.in("LH_CODE", syncMachineCodes);
            deleteWrapper.eq("DATA_SOURCE", "1");
            deleteWrapper.eq("IS_DELETE", 0);
            lhMouldCleanPlanMapper.delete(deleteWrapper);
            log.info("已删除{}台机台的旧模具清洗计划数据", syncMachineCodes.size());

            List<LhMouldCleanPlan> planList = new ArrayList<>();
            for (Map.Entry<String, List<LhMouldCleanWarn>> entry : machineMap.entrySet()) {
                String machineCode = entry.getKey();
                List<LhMouldCleanWarn> warns = entry.getValue();

                List<LhMouldCleanPlan> plans = buildCleanPlans(machineCode, warns, cleanDays);
                planList.addAll(plans);
            }

            for (LhMouldCleanPlan plan : planList) {
                plan.setCreateBy("SYSTEM");
                plan.setCreateTime(now);
                plan.setUpdateBy("SYSTEM");
                plan.setUpdateTime(now);
                plan.setIsDelete(0);
            }

            if (!planList.isEmpty()) {
                baseDao.insertBatch(planList);
                log.info("成功同步{}条模具清洗计划数据", planList.size());
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

    private List<LhMouldCleanPlan> buildCleanPlans(String machineCode, List<LhMouldCleanWarn> warns, int cleanDays) {
        List<LhMouldCleanPlan> result = new ArrayList<>();

        LhMouldCleanWarn leftWarn = null;
        LhMouldCleanWarn rightWarn = null;

        for (LhMouldCleanWarn warn : warns) {
            String lhCode = warn.getLhCode();
            if (lhCode != null) {
                if (lhCode.endsWith(" L")) {
                    leftWarn = warn;
                } else if (lhCode.endsWith(" R")) {
                    rightWarn = warn;
                }
            }
        }

        if (leftWarn != null && rightWarn != null) {
            String leftCleanType = determineCleanType(leftWarn);
            String rightCleanType = determineCleanType(rightWarn);

            if (leftCleanType.equals(rightCleanType)) {
                LhMouldCleanPlan plan = new LhMouldCleanPlan();
                plan.setLhCode(machineCode);
                plan.setDataSource("1");
                plan.setLeftRightMould("LR");
                plan.setCleanType(leftCleanType);

                Date leftCleanTime = calculateCleanTime(leftWarn, cleanDays);
                Date rightCleanTime = calculateCleanTime(rightWarn, cleanDays);
                Date cleanTime = leftCleanTime.after(rightCleanTime) ? leftCleanTime : rightCleanTime;
                plan.setCleanTime(cleanTime);

                plan.setFactoryCode(leftWarn.getFactoryCode());
                plan.setCompanyCode(leftWarn.getCompanyCode());

                result.add(plan);
            } else {
                LhMouldCleanPlan leftPlan = new LhMouldCleanPlan();
                leftPlan.setLhCode(machineCode);
                leftPlan.setDataSource("1");
                leftPlan.setLeftRightMould("L");
                leftPlan.setCleanType(leftCleanType);
                leftPlan.setCleanTime(calculateCleanTime(leftWarn, cleanDays));
                leftPlan.setFactoryCode(leftWarn.getFactoryCode());
                leftPlan.setCompanyCode(leftWarn.getCompanyCode());
                result.add(leftPlan);

                LhMouldCleanPlan rightPlan = new LhMouldCleanPlan();
                rightPlan.setLhCode(machineCode);
                rightPlan.setDataSource("1");
                rightPlan.setLeftRightMould("R");
                rightPlan.setCleanType(rightCleanType);
                rightPlan.setCleanTime(calculateCleanTime(rightWarn, cleanDays));
                rightPlan.setFactoryCode(rightWarn.getFactoryCode());
                rightPlan.setCompanyCode(rightWarn.getCompanyCode());
                result.add(rightPlan);
            }
        } else {
            LhMouldCleanPlan plan = new LhMouldCleanPlan();
            plan.setLhCode(machineCode);
            plan.setDataSource("1");

            String leftRightMould = buildLeftRightMould(warns);
            plan.setLeftRightMould(leftRightMould);

            String cleanType = determineCleanType(warns);
            plan.setCleanType(cleanType);

            Date cleanTime = calculateCleanTime(warns, cleanDays);
            plan.setCleanTime(cleanTime);

            if (!warns.isEmpty()) {
                plan.setFactoryCode(warns.get(0).getFactoryCode());
                plan.setCompanyCode(warns.get(0).getCompanyCode());
            }

            result.add(plan);
        }

        return result;
    }

    private String determineCleanType(LhMouldCleanWarn warn) {
        if (warn.getSecondWashTime() != null) {
            return "02";
        } else if (warn.getFirstWashTime() != null) {
            return "01";
        } else if (warn.getOperTime() != null) {
            return "01";
        }
        return "01";
    }

    private String determineCleanType(List<LhMouldCleanWarn> warns) {
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

        if (secondWashTime != null) {
            return "02";
        } else if (firstWashTime != null) {
            return "01";
        } else if (operTime != null) {
            return "01";
        }
        return "01";
    }

    private Date calculateCleanTime(LhMouldCleanWarn warn, int cleanDays) {
        if (warn.getSecondWashTime() != null) {
            return DateUtil.offsetDay(warn.getSecondWashTime(), cleanDays);
        } else if (warn.getFirstWashTime() != null) {
            return DateUtil.offsetDay(warn.getFirstWashTime(), cleanDays);
        } else if (warn.getOperTime() != null) {
            return DateUtil.offsetDay(warn.getOperTime(), cleanDays);
        }
        return DateUtil.offsetDay(new Date(), cleanDays);
    }

    private Date calculateCleanTime(List<LhMouldCleanWarn> warns, int cleanDays) {
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

        if (secondWashTime != null) {
            return DateUtil.offsetDay(secondWashTime, cleanDays);
        } else if (firstWashTime != null) {
            return DateUtil.offsetDay(firstWashTime, cleanDays);
        } else if (operTime != null) {
            return DateUtil.offsetDay(operTime, cleanDays);
        }
        return DateUtil.offsetDay(new Date(), cleanDays);
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
