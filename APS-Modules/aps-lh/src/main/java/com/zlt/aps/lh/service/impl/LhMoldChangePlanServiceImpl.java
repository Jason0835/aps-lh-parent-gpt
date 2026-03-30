package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.CommonRedisService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constants.LhPrefixConstants;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.ChangeTypeEnum;
import com.zlt.aps.lh.mapper.LhMoldChangePlanEntityMapper;
import com.zlt.aps.lh.service.ILhDayFinishQtyService;
import com.zlt.aps.lh.service.ILhMoldChangePlanService;
import com.zlt.aps.lh.service.LhScheduleResultService;
import com.zlt.aps.mp.api.domain.vo.SpecCodeAndProductCodeVO;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMoldChangePlanServiceImpl.java
 * 描    述：LhMoldChangePlanServiceImpl模具变动单业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-17
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMoldChangePlanServiceImpl extends AbstractDocService<LhMoldChangePlan> implements ILhMoldChangePlanService {

    @Autowired
    private LhScheduleResultService lhScheduleResultService;

    @Resource
    private LhMoldChangePlanEntityMapper lhMoldChangePlanMapper;
    @Autowired
    private CommonRedisService commonCacheService;
    @Autowired
    private ILhDayFinishQtyService lhDayFinishQtyService;
    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "0301";
    }


    /**
     * 根据条件查询List
     *
     * @param queryWrapper
     * @return
     */
    @Override
    public List<LhMoldChangePlan> selectList(QueryWrapper<LhMoldChangePlan> queryWrapper) {
        return lhMoldChangePlanMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moldReplacementPlan(LhMoldChangePlan queryVO) throws BusinessException {
        // 获取批次号和工厂编码
        String batchNo = queryVO.getLhResultBatchNo();
        if (StringUtils.isBlank(batchNo)) {
            String msg = I18nUtil.getMessage("ui.data.column.lhMoldChangePlan.batchNo.required");
            throw new BusinessException(msg);
        }
        String factoryCode = queryVO.getFactoryCode();

        // 检查当前批次是否存在已发布的换模计划
        LambdaQueryWrapper<LhMoldChangePlan> releasedQuery = new LambdaQueryWrapper<>();
        releasedQuery.eq(LhMoldChangePlan::getFactoryCode, factoryCode)
                .eq(LhMoldChangePlan::getIsRelease, ApsConstant.IS_RELEASE)
                .eq(LhMoldChangePlan::getLhResultBatchNo, batchNo);
        Long releasedCount = lhMoldChangePlanMapper.selectCount(releasedQuery);
        if (releasedCount > 0) {
            // 当前批次已存在已发布的换模计划，不再生成新的计划
            String msg = I18nUtil.getMessage("ui.data.column.lhMoldChangePlan.alreadyPublished") + batchNo;
            throw new BusinessException(msg);
        }

        // 删除当前批次下未发布的换模计划
        LambdaQueryWrapper<LhMoldChangePlan> unReleasedQuery = new LambdaQueryWrapper<>();
        unReleasedQuery.eq(LhMoldChangePlan::getFactoryCode, factoryCode)
                .eq(LhMoldChangePlan::getIsRelease, ApsConstant.NO_RELEASE)
                .eq(LhMoldChangePlan::getLhResultBatchNo, batchNo);
        lhMoldChangePlanMapper.delete(unReleasedQuery);

        // 根据批次号查询硫化计划记录
        QueryWrapper<LhScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BATCH_NO", batchNo);
        List<LhScheduleResult> scheduleResultList = lhScheduleResultService.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(scheduleResultList)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.noQueryLhScheduleResultByBatchNo") + batchNo);
        }

        // 按硫化机台分组
        Map<String, List<LhScheduleResult>> machineGroupMap = scheduleResultList.stream()
                .collect(Collectors.groupingBy(LhScheduleResult::getLhMachineCode));

        for (Map.Entry<String, List<LhScheduleResult>> entry : machineGroupMap.entrySet()) {
            List<LhScheduleResult> machineSchedules = entry.getValue();
            // 判断该机台是否存在多个不同规格
            Set<String> specSet = machineSchedules.stream()
                    .map(LhScheduleResult::getSpecCode)
                    .collect(Collectors.toSet());
            if (specSet.size() <= 1) {
                // 同一规格，无需生成换模计划
                continue;
            }

            // 根据规格结束时间(specEndTime)升序排序：最早的为前规格，后面的为后规格
            machineSchedules.sort(Comparator.comparing(LhScheduleResult::getSpecEndTime));

            // 遍历相邻两条记录，生成换模计划（若有N个规格，则生成N-1条换模计划）
            for (int i = 0; i < machineSchedules.size() - 1; i++) {
                LhScheduleResult before = machineSchedules.get(i);
                LhScheduleResult after = machineSchedules.get(i + 1);

                // 构建换模计划对象
                LhMoldChangePlan moldChangePlan = new LhMoldChangePlan();
                moldChangePlan.setFactoryCode(before.getFactoryCode());
                // 生成模具变动单批次号（示例：使用当前时间戳和机台编号拼接，实际规则可调整）
                moldChangePlan.setMoldBatchNo("MOLD" + System.currentTimeMillis() + before.getLhMachineCode());
                moldChangePlan.setLhMachineCode(before.getLhMachineCode());
                moldChangePlan.setLhMachineName(before.getLhMachineName());
                moldChangePlan.setBeforeSpecCode(before.getSpecCode());
                moldChangePlan.setBeforeSpecDesc(before.getSpecDesc());
                // 使用硫化计划中的胎胚库存作为粗略库存
                moldChangePlan.setTireRoughStock(before.getEmbryoStock());
                moldChangePlan.setChangeType(ChangeTypeEnum.DEMOULDING_AND_REPLACEMENT.getCode());
                moldChangePlan.setAfterSpecCode(after.getSpecCode());
                moldChangePlan.setAfterSpecDesc(after.getSpecDesc());
                moldChangePlan.setStockArea("");
                // 更换时间采用前规格的结束时间
                moldChangePlan.setChangeTime(before.getSpecEndTime());
                moldChangePlan.setIsRelease(ApsConstant.NO_RELEASE);
                // 设置换模计划对应的硫化结果批次号
                moldChangePlan.setLhResultBatchNo(batchNo);

                // 保存换模计划记录
                lhMoldChangePlanMapper.insert(moldChangePlan);
            }
        }
    }

    /**
     * 生成换模计划
     * @param before
     * @param after
     * @return
     */
    @Override
    public LhMoldChangePlan genMoldChangePlan(LhScheduleResult before, LhScheduleResult after){
        // 构建换模计划对象
        LhMoldChangePlan moldChangePlan = new LhMoldChangePlan();
        moldChangePlan.setScheduleDate(before.getScheduleDate());
        moldChangePlan.setFactoryCode(before.getFactoryCode());
        // 生成模具变动单批次号（示例：使用当前时间戳和机台编号拼接，实际规则可调整）
        moldChangePlan.setMoldBatchNo("MOLD" + System.currentTimeMillis() + before.getLhMachineCode());
        moldChangePlan.setLhMachineCode(before.getLhMachineCode());
        moldChangePlan.setLhMachineName(before.getLhMachineName());
        moldChangePlan.setBeforeSpecCode(before.getSpecCode());
        moldChangePlan.setBeforeSpecDesc(before.getSpecDesc());
        // 使用硫化计划中的胎胚库存作为粗略库存
        moldChangePlan.setTireRoughStock(before.getEmbryoStock());
        moldChangePlan.setChangeType(ChangeTypeEnum.DEMOULDING_AND_REPLACEMENT.getCode());
        moldChangePlan.setAfterSpecCode(after.getSpecCode());
        moldChangePlan.setAfterSpecDesc(after.getSpecDesc());
        moldChangePlan.setStockArea("");
        // 更换时间采用前规格的结束时间
        moldChangePlan.setChangeTime(before.getSpecEndTime());
        moldChangePlan.setIsRelease(ApsConstant.NO_RELEASE);
        // 设置换模计划对应的硫化结果批次号
        moldChangePlan.setLhResultBatchNo(before.getBatchNo());
        return moldChangePlan;
    }

    /**
     * 批量插入
     * @param list
     */
    @Override
    public void insertList(List<LhMoldChangePlan> list){
        baseDao.saveBatch(list);
    }

    @Override
    public AjaxResult importMoldChangePlan(List<LhMoldChangePlan> list, Long importLogId, Date scheduleDate) {
        //0.初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhScheduleResult> importList = new ArrayList<>();
        List<LhScheduleResult> updateList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        //对所有list设置排程日期，让下面可以做唯一校验
        list.forEach(item ->{
            item.setScheduleDate(scheduleDate);
            item.setFactoryCode("116");
        });
        //1.进行非空校验,Excel中数据重复校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhMoldChangePlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", scheduleDate);
        List<LhScheduleResult> dbLhScheduleResultList = lhScheduleResultService.getScheduleResultList(scheduleDate);
        String batchNo = PubUtil.isNotEmpty(dbLhScheduleResultList) ? dbLhScheduleResultList.get(0).getBatchNo() : "";
        // 赋值批次号
        if (com.ruoyi.common.utils.StringUtils.isEmpty(batchNo)) {
            batchNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_BATCH_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_BATCH_NO_PREFIX + scheduleDateStr);
        }

        Map<String, LhScheduleResult> dbScheduleResultMap = new HashMap<>();
        if (PubUtil.isNotEmpty(dbLhScheduleResultList)) {
            for (LhScheduleResult scheduleResult : dbLhScheduleResultList) {
                dbScheduleResultMap.put(scheduleResult.getLhMachineCode() + scheduleResult.getSpecCode() + scheduleResult.getLeftRightMold(), scheduleResult);
            }
        }

        List<LhMoldChangePlan> allLhMoldChangePlan = new ArrayList<>();
        //2.进行数据库唯一性校验
        for (LhMoldChangePlan docEntity : list) {
            //前规格
            String beforeSpecCode = docEntity.getBeforeSpecCode();
            String afterSpecCode = docEntity.getAfterSpecCode();

            List<String> before = Arrays.stream(beforeSpecCode.split("\\*")).collect(Collectors.toList());
            List<String> after = Arrays.stream(afterSpecCode.split("\\*")).collect(Collectors.toList());
            List<String> allSpecCode = Stream.concat(after.stream(), before.stream()).collect(Collectors.toList());
            List<SpecCodeAndProductCodeVO> mpc = lhScheduleResultService.getConstructionList("116", allSpecCode);
            Map<String, SpecCodeAndProductCodeVO> infoMap = mpc.stream().collect(Collectors.toMap(SpecCodeAndProductCodeVO::getSpecCode, Function.identity(), (s1, s2) -> s1));

            StringBuilder embryoCode = new StringBuilder();
            StringBuilder productDesc = new StringBuilder();
            StringBuilder productCode = new StringBuilder();
            // 组装数据格式
            List<String> embryoParts = before.stream()
                    .map(be -> {SpecCodeAndProductCodeVO vo = infoMap.get(be);return (vo != null && vo.getEmbryoCode() != null) ? vo.getEmbryoCode() : "";})
                    .collect(Collectors.toList());
            List<String> productParts = before.stream()
                    .map(be -> {SpecCodeAndProductCodeVO vo = infoMap.get(be);return (vo != null && vo.getProductCode() != null) ? vo.getProductCode() : "";})
                    .collect(Collectors.toList());
            List<String> descParts = before.stream()
                    .map(be -> {SpecCodeAndProductCodeVO vo = infoMap.get(be);return (vo != null && vo.getProductDesc() != null) ? vo.getProductDesc() : "";})
                    .collect(Collectors.toList());

            embryoCode.append(String.join("*", embryoParts));
            productDesc.append(String.join("*", descParts));
            productCode.append(String.join("*", productParts));

            // 获取换模后机台信息 跟换模前的机台一样
            String lhMCode = dbLhScheduleResultList.stream().filter(it -> it.getSpecCode().equals(after.get(0)))
                    .map(LhScheduleResult::getLhMachineCode).findFirst().get();

            LhScheduleResult result = new LhScheduleResult();
            //赋值硫化时间
            result.setScheduleDate(scheduleDate);
            result.setFactoryCode("116");
            result.setLhMachineCode(lhMCode);
            result.setRealScheduleDate(scheduleDate);
            result.setBatchNo(batchNo);
            result.setClass1PlanQty(docEntity.getBeforeOnePlan());
            result.setClass1FinishQty(docEntity.getBeforeOneFinish());
            result.setEmbryoCode(embryoCode.toString());
            result.setSpecDesc(productDesc.toString());
            result.setProductCode(productCode.toString());
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            result.setSpecCode(beforeSpecCode);
            result.setMoldQty(com.ruoyi.common.utils.StringUtils.isEmpty(result.getLeftRightMold()) ? 2 : 1);
            //拆分左右模
            List<LhScheduleResult> splitList = new ArrayList<>();
            // 拆模
            lhScheduleResultService.splitLeftRightMold(result, splitList);
            LhScheduleResult dbScheduleResult;
            for (LhScheduleResult scheduleResult : splitList) {
                dbScheduleResult = dbScheduleResultMap.get(scheduleResult.getLhMachineCode() + scheduleResult.getSpecCode() + (scheduleResult.getLeftRightMold() != null ? scheduleResult.getLeftRightMold() : ""));
                if (dbScheduleResult == null) {
                    scheduleResult.setRowState(RowStateEnum.ADDED);
                    String lhOrderNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_ORDER_NO_PREFIX + scheduleDateStr);
                    scheduleResult.setOrderNo(lhOrderNo);
                    importList.add(scheduleResult);
                } else {
                    lhScheduleResultService.copyToDbScheduleResult(scheduleResult, dbScheduleResult);
                    dbScheduleResult.setRowState(RowStateEnum.ADDED);
                    importList.add(dbScheduleResult);
                }
            }
            String finalBatchNo = batchNo;
            List<String> targetList = new ArrayList<>();
            this.generateRecords(beforeSpecCode, afterSpecCode, targetList);
            targetList.forEach(all ->{
                // 组装换模计划
                LhMoldChangePlan exec = new LhMoldChangePlan();
                exec.setScheduleDate(scheduleDate);
                exec.setFactoryCode("116");
                String beSpcCode = all.split("-")[0];
                String afSpcCode = all.split("-")[1];
                exec.setBeforeSpecCode(beSpcCode);
                exec.setBeforeSpecDesc(infoMap.get(beSpcCode).getProductDesc());
                exec.setAfterSpecCode(afSpcCode);
                exec.setAfterSpecDesc(infoMap.get(afSpcCode).getProductDesc());
                exec.setChangeTime(new Date());
                exec.setLhResultBatchNo(finalBatchNo);
                exec.setLhMachineCode(lhMCode);
                allLhMoldChangePlan.add(exec);
            });
        }

        if (CollectionUtils.isNotEmpty(importList)) {
            //更新施工信息
            lhScheduleResultService.updateConstructionInfo(list.get(0).getFactoryCode(), importList);
            successNum = lhScheduleResultService.saveBatchByImport(importList);
            // 更新日完成量，完成量汇总
            lhDayFinishQtyService.updateMonthPlanSurplus(list.get(0).getFactoryCode(), scheduleDate);
            // 添加换模计划
            baseDao.saveBatch(allLhMoldChangePlan);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    private void generateRecords(String frontSpec, String backSpec, List<String> targetList) {
        String[] frontParts = frontSpec.split("\\*");
        String[] backParts = backSpec.split("\\*");

        for (String frontPart : frontParts) {
            for (String backPart : backParts) {
                String combinedRecord = frontPart + "-" + backPart;
                targetList.add(combinedRecord);
            }
        }
    }
}
