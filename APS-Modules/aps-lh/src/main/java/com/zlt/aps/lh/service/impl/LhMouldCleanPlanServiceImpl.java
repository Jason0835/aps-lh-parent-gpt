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
    public int save(LhMouldCleanPlan entity) {
        if (entity.getId() != null) {
            entity.setBaseVale(entity.getId());
        } else {
            entity.setBaseVale(null);
        }
        return super.save(entity);
    }

    @Override
    public int syncFromMouldCleanWarn() {
        String lockKey = "sync:mould:clean:plan";
        if (redisService.getCacheObject(lockKey) != null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.message.sync.in.progress"));
        }

        try {
            redisService.setCacheObject(lockKey, "1");
            log.info("开始从模具清洗预警增量同步清洗计划数据");

            // 只取最新版本号的预警数据
            QueryWrapper<LhMouldCleanWarn> maxVersionWrapper = new QueryWrapper<>();
            maxVersionWrapper.select("MAX(DATA_VERSION) as dataVersion");
            LhMouldCleanWarn maxVersionResult = lhMouldCleanWarnMapper.selectOne(maxVersionWrapper);
            String maxVersion = maxVersionResult != null ? maxVersionResult.getDataVersion() : null;

            if (maxVersion == null || maxVersion.isEmpty()) {
                log.info("模具清洗预警数据为空，无版本号");
                return 0;
            }

            QueryWrapper<LhMouldCleanWarn> warnWrapper = new QueryWrapper<>();
            warnWrapper.eq("DATA_VERSION", maxVersion);
            List<LhMouldCleanWarn> warnList = lhMouldCleanWarnMapper.selectList(warnWrapper);
            if (warnList == null || warnList.isEmpty()) {
                log.info("最新版本号{}的模具清洗预警数据为空", maxVersion);
                return 0;
            }
            log.info("获取最新版本号{}的模具清洗预警数据{}条", maxVersion, warnList.size());

            // 按机台分组预警数据
            Map<String, List<LhMouldCleanWarn>> machineMap = new HashMap<>();
            for (LhMouldCleanWarn warn : warnList) {
                String machineCode = extractMachineCode(warn.getLhCode());
                if (machineCode != null) {
                    machineMap.computeIfAbsent(machineCode, k -> new ArrayList<>()).add(warn);
                }
            }

            int cleanDays = getCleanDays();
            Date now = new Date();

            // 查询当前所有数据来源为预警的清洗计划（未删除）
            QueryWrapper<LhMouldCleanPlan> existWrapper = new QueryWrapper<>();
            existWrapper.eq("DATA_SOURCE", "1");
            existWrapper.eq("IS_DELETE", 0);
            List<LhMouldCleanPlan> existingPlans = lhMouldCleanPlanMapper.selectList(existWrapper);

            // 按机台+左右模分组已有计划，key格式：lhCode|leftRightMould
            Map<String, LhMouldCleanPlan> existPlanMap = new HashMap<>();
            for (LhMouldCleanPlan plan : existingPlans) {
                String key = plan.getLhCode() + "|" + (plan.getLeftRightMould() != null ? plan.getLeftRightMould() : "");
                existPlanMap.put(key, plan);
            }

            // 收集预警中已删除的机台编码（isDelete=1的预警对应的机台）
            Set<String> deletedMachineKeys = new HashSet<>();
            for (LhMouldCleanWarn warn : warnList) {
                if (warn.getIsDelete() != null && warn.getIsDelete() == 1) {
                    String machineCode = extractMachineCode(warn.getLhCode());
                    if (machineCode != null) {
                        deletedMachineKeys.add(machineCode);
                    }
                }
            }

            // 收集预警中未删除的机台编码
            Set<String> activeMachineCodes = new HashSet<>();
            for (LhMouldCleanWarn warn : warnList) {
                if (warn.getIsDelete() == null || warn.getIsDelete() == 0) {
                    String machineCode = extractMachineCode(warn.getLhCode());
                    if (machineCode != null) {
                        activeMachineCodes.add(machineCode);
                    }
                }
            }

            int updateCount = 0;
            int insertCount = 0;
            int deleteCount = 0;

            // 1. 处理已删除的预警：更新对应清洗计划的删除标识
            for (String deletedMachineCode : deletedMachineKeys) {
                // 如果该机台同时有未删除的预警，则跳过（不删除计划）
                if (activeMachineCodes.contains(deletedMachineCode)) {
                    continue;
                }
                // 更新该机台所有未删除的计划为已删除
                QueryWrapper<LhMouldCleanPlan> delUpdateWrapper = new QueryWrapper<>();
                delUpdateWrapper.eq("LH_CODE", deletedMachineCode);
                delUpdateWrapper.eq("DATA_SOURCE", "1");
                delUpdateWrapper.eq("IS_DELETE", 0);
                LhMouldCleanPlan delUpdate = new LhMouldCleanPlan();
                delUpdate.setIsDelete(1);
                delUpdate.setUpdateBy("SYSTEM");
                delUpdate.setUpdateTime(now);
                int cnt = lhMouldCleanPlanMapper.update(delUpdate, delUpdateWrapper);
                deleteCount += cnt;
                log.info("机台{}的预警已删除，更新{}条清洗计划为已删除", deletedMachineCode, cnt);
            }

            // 2. 处理未删除的预警：更新已有计划的时间或新增计划
            for (Map.Entry<String, List<LhMouldCleanWarn>> entry : machineMap.entrySet()) {
                String machineCode = entry.getKey();
                List<LhMouldCleanWarn> warns = entry.getValue();

                // 过滤掉已删除的预警
                List<LhMouldCleanWarn> activeWarns = new ArrayList<>();
                for (LhMouldCleanWarn w : warns) {
                    if (w.getIsDelete() == null || w.getIsDelete() == 0) {
                        activeWarns.add(w);
                    }
                }
                if (activeWarns.isEmpty()) {
                    continue;
                }

                List<LhMouldCleanPlan> newPlans = buildCleanPlans(machineCode, activeWarns, cleanDays);

                for (LhMouldCleanPlan newPlan : newPlans) {
                    String planKey = newPlan.getLhCode() + "|" + (newPlan.getLeftRightMould() != null ? newPlan.getLeftRightMould() : "");

                    if (existPlanMap.containsKey(planKey)) {
                        // 已有计划：更新清洗时间和清洗类型
                        LhMouldCleanPlan existPlan = existPlanMap.get(planKey);
                        LhMouldCleanPlan updatePlan = new LhMouldCleanPlan();
                        updatePlan.setId(existPlan.getId());
                        updatePlan.setCleanType(newPlan.getCleanType());
                        updatePlan.setCleanTime(newPlan.getCleanTime());
                        updatePlan.setUpdateBy("SYSTEM");
                        updatePlan.setUpdateTime(now);
                        lhMouldCleanPlanMapper.updateById(updatePlan);
                        updateCount++;
                        log.debug("更新机台{}的清洗计划，清洗类型={}，清洗时间={}", machineCode, newPlan.getCleanType(), newPlan.getCleanTime());
                    } else {
                        // 新机台：生成新的清洗计划
                        newPlan.setCreateBy("SYSTEM");
                        newPlan.setCreateTime(now);
                        newPlan.setUpdateBy("SYSTEM");
                        newPlan.setUpdateTime(now);
                        newPlan.setIsDelete(0);
                        baseDao.insert(newPlan);
                        insertCount++;
                        log.info("新增机台{}的清洗计划，清洗类型={}，清洗时间={}", machineCode, newPlan.getCleanType(), newPlan.getCleanTime());
                    }
                }
            }

            log.info("模具清洗计划增量同步完成，更新{}条，新增{}条，删除标记{}条", updateCount, insertCount, deleteCount);
            return updateCount + insertCount + deleteCount;
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
