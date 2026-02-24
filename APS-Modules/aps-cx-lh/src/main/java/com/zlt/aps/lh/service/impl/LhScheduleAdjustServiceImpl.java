package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.Constant;
import com.zlt.aps.constant.IncrementConstant;
import com.zlt.aps.enums.AdjustTypeEnums;
import com.zlt.aps.utils.IncrementService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.cx.mapper.entity.CxScheduleResultEntityMapper;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultUpdateDTO;
import com.zlt.aps.lh.api.domain.entity.LhCxLinkageConfirm;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.mapper.LhScheduleAdjustEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultEntityMapper;
import com.zlt.aps.lh.service.ILhDispatcherLogService;
import com.zlt.aps.lh.service.LhScheduleAdjustService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 硫化排程调整实现类
 * @author pancd
 * @version 1.0
 * @Description
 * @date 2025/2/18
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhScheduleAdjustServiceImpl extends AbstractDocService<LhCxLinkageConfirm>  implements LhScheduleAdjustService {

    @Autowired
    private LhScheduleAdjustEntityMapper lhScheduleAdjustEntityMapper;

    @Autowired
    private LhScheduleResultEntityMapper lhScheduleResultEntityMapper;

    @Autowired
    private CxScheduleResultEntityMapper cxScheduleResultEntityMapper;
    
    @Autowired
    private CxScheduleResultService cxScheduleResultService;
    @Autowired
    private ILhDispatcherLogService lhDispatcherLogService;

    @Autowired
    private IncrementService incrementService;

    @Override
    public String getDocTypeCode() {
        return "LH2025416";
    }

    /**
     * 1、保存调量信息
     * 2、预调整信息，如果发布后的记录，还需要生成联动调整记录（特别的，目前拼模不会联动修改）
     */
    @Override
    public void preAdjustment(LhScheduleResultUpdateDTO updateResultDto) {
        // 取原始的排产结果
        LhScheduleResult originResult = lhScheduleResultEntityMapper.selectById(updateResultDto.getId());
        if (originResult == null) {
            return;
        }
        //插入硫化操作日志
        this.updateScheduleResult(updateResultDto);
        LhScheduleResult updateResult = new LhScheduleResult();
        BeanUtils.copyProperties(updateResultDto, updateResult);
        for (int i = 1; i <= ApsConstant.SIX_CLASS; i++) {
            // 如果原始记录的计划量或者原因分析有值，更新时设置一个默认值
            String classNPlanQtyField = "class" + i + "PlanQty";
            Integer oldClassNPlanQty = (Integer) originResult.getFieldValueByFieldName(classNPlanQtyField);
            if (oldClassNPlanQty != null) {
                Integer updateClassNPlanQty = (Integer) updateResult.getFieldValueByFieldName(classNPlanQtyField);
                updateResult.setFieldValueByFieldName(classNPlanQtyField, updateClassNPlanQty == null ? 0 : updateClassNPlanQty);
            }
            String classNAnalysisField = "class" + i + "Analysis";
            String oldClassNAnalysis = (String) originResult.getFieldValueByFieldName(classNAnalysisField);
            if (oldClassNAnalysis != null) {
                String updateClassNAnalysis = (String) updateResult.getFieldValueByFieldName(classNAnalysisField);
                updateResult.setFieldValueByFieldName(classNAnalysisField, updateClassNAnalysis == null ? "" : updateClassNAnalysis);
            }
        }
        // 已发布直接改，发布中或者超时的不能调量
        if (!ApsConstant.IS_RELEASE.equals(originResult.getIsRelease())) {
            // 更新调整字段
            copyClassPlanAnalysis(originResult, updateResult);
            lhScheduleResultEntityMapper.updateById(originResult);

            return;
        }

        // 当前的调整类型（默认部分调整）
        String operType = updateResultDto.getOperType() != null ? updateResultDto.getOperType() : AdjustTypeEnums.PART_ADJUST.getCode();

        // 记录首班、调整数量
        Long firstClass = null;
        long adjustQty = 0L;

        for (int i = 1; i <= ApsConstant.SIX_CLASS; i++) {
            String classNPlanQtyField = "class" + i + "PlanQty";
            Integer updateQty = (Integer) updateResult.getFieldValueByFieldName(classNPlanQtyField);
            Integer originQty = (Integer) originResult.getFieldValueByFieldName(classNPlanQtyField);

            if (Objects.equals(updateQty, originQty)) {
                continue;
            }
            updateQty = updateQty == null ? 0 : updateQty;
            originQty = originQty == null ? 0 : originQty;

            // 记录首班
            firstClass = firstClass != null ? firstClass : i;
            // 记录总调整量
            adjustQty += updateQty - originQty;

            if (updateQty < originQty && AdjustTypeEnums.ALL_ADJUST.getCode().equals(operType)) {
                // 如果是调减，而且是全部调整，后续所有的计划量都变成0
                for (int j = i + 1; j <= ApsConstant.SIX_CLASS; j++) {
                    classNPlanQtyField = "class" + j + "PlanQty";
                    // 记录调整量
                    Serializable planQty = updateResult.getFieldValueByFieldName(classNPlanQtyField);
                    if (planQty != null) {
                        adjustQty -= (int) planQty;
                    }
                    updateResult.setFieldValueByFieldName(classNPlanQtyField, 0);
                }

                break;
            }
        }

        // 更新调整字段
        copyClassPlanAnalysis(originResult, updateResult);
        originResult.setIsRelease(ApsConstant.WAIT_RELEASING);
        lhScheduleResultEntityMapper.updateById(originResult);
        // 没有调整班次，无需联动
        if (firstClass == null) {
            log.info("硫化联动成型，没有调整班次，硫化排程id：{}", originResult.getId());
            return;
        }
        log.info("硫化联动成型，硫化排程id：{}，调整前变化量{}，调整班次{}", originResult.getId(), adjustQty, firstClass);

        String versionNo = incrementService.getBillNoSequenceByExpire(IncrementConstant.LH_LINKAGE_CONFIRM + DateUtils.dateTimeNow(), 1, 60);
        // 调用成型接口，计算调整后的各班计划量
        List<LhCxLinkageConfirm> changeCxResultList = changeCxResult(originResult.getId(), adjustQty, false);
        if (CollectionUtils.isEmpty(changeCxResultList)) {
            return;
        }
        for (LhCxLinkageConfirm confirm : changeCxResultList) {
            confirm.setAdjustBatchNo(versionNo);
            setLhConfirm(confirm, originResult, adjustQty, operType);
            confirm.setIsConfirm(ApsConstant.FALSE);
        }
        this.baseDao.insertBatch(changeCxResultList);
    }

    /**
     * 调量插入操作日志
     * @param dto
     */
    private void updateScheduleResult(LhScheduleResultUpdateDTO dto) {
        //判断是否发布
        LhScheduleResult lhScheduleResult = lhScheduleResultEntityMapper.selectById(dto.getId());
        //插入调量
        //插入排程操作日志
        LhDispatcherLog lhDispatcherLog = new LhDispatcherLog();
        lhDispatcherLog.setFactoryCode(lhScheduleResult.getFactoryCode());
        lhDispatcherLog.setScheduleId(lhScheduleResult.getId());
        lhDispatcherLog.setOperType(ApsConstant.APS_STRING_1);
        lhDispatcherLog.setScheduleDate(lhScheduleResult.getScheduleDate());
        lhDispatcherLog.setProductCode(lhScheduleResult.getProductCode());
        lhDispatcherLog.setSpecCode(lhScheduleResult.getSpecCode());
        lhDispatcherLog.setIsDelivery(lhScheduleResult.getIsDelivery());
        lhDispatcherLog.setBeforeMachineCode(lhScheduleResult.getLhMachineCode());
        lhDispatcherLog.setAfterMachineCode(lhScheduleResult.getLhMachineCode());
        lhDispatcherLog.setBeforeClass1Plan(lhScheduleResult.getClass1PlanQty());
        lhDispatcherLog.setBeforeClass2Plan(lhScheduleResult.getClass2PlanQty());
        lhDispatcherLog.setBeforeClass3Plan(lhScheduleResult.getClass3PlanQty());
        lhDispatcherLog.setBeforeClass4Plan(lhScheduleResult.getClass4PlanQty());
        lhDispatcherLog.setBeforeClass5Plan(lhScheduleResult.getClass5PlanQty());
        lhDispatcherLog.setBeforeClass6Plan(lhScheduleResult.getClass6PlanQty());
        lhDispatcherLog.setAfterClass1Plan(dto.getClass1PlanQty());
        lhDispatcherLog.setAfterClass2Plan(dto.getClass2PlanQty());
        lhDispatcherLog.setAfterClass3Plan(dto.getClass3PlanQty());
        lhDispatcherLog.setAfterClass4Plan(dto.getClass4PlanQty());
        lhDispatcherLog.setAfterClass5Plan(dto.getClass5PlanQty());
        lhDispatcherLog.setAfterClass6Plan(dto.getClass6PlanQty());
        lhDispatcherLog.setBaseVale(null);
        //操作人
        lhDispatcherLogService.insert(lhDispatcherLog);
    }
    /**
     * 复制各班计划量和原因分析
     */
    private void copyClassPlanAnalysis(LhScheduleResult originResult, LhScheduleResult updateResult) {
        // 复制各班计划量和原因分析
        originResult.setClass1PlanQty(updateResult.getClass1PlanQty());
        originResult.setClass2PlanQty(updateResult.getClass2PlanQty());
        originResult.setClass3PlanQty(updateResult.getClass3PlanQty());
        originResult.setClass4PlanQty(updateResult.getClass4PlanQty());
        originResult.setClass5PlanQty(updateResult.getClass5PlanQty());
        originResult.setClass6PlanQty(updateResult.getClass6PlanQty());
        originResult.setClass1Analysis(updateResult.getClass1Analysis());
        originResult.setClass2Analysis(updateResult.getClass2Analysis());
        originResult.setClass3Analysis(updateResult.getClass3Analysis());
        originResult.setClass4Analysis(updateResult.getClass4Analysis());
        originResult.setClass5Analysis(updateResult.getClass5Analysis());
        originResult.setClass6Analysis(updateResult.getClass6Analysis());
    }

    /**
     * 调用成型接口，计算调整后的各班计划量
     *
     * @param lhResultId 硫化排程ID
     * @param adjustQty  总修改量
     * @param isResorted 是否成型重排
     */
    private List<LhCxLinkageConfirm> changeCxResult(Long lhResultId, double adjustQty, boolean isResorted) {
        // 根据硫化id，查询成型的记录
        List<CxScheduleResult> cxResultList = cxScheduleResultEntityMapper.selectList(Wrappers.lambdaQuery(CxScheduleResult.class).and(
                v -> v.eq(CxScheduleResult::getLhScheduleIds, lhResultId)
                        .or().likeRight(CxScheduleResult::getLhScheduleIds, lhResultId + "/")
                        .or().likeLeft(CxScheduleResult::getLhScheduleIds, "/" + lhResultId)
                        .or().like(CxScheduleResult::getLhScheduleIds, "/" + lhResultId + "/")
        ));
        // 没有成型计划
        if (CollectionUtils.isEmpty(cxResultList)) {
            log.info("硫化联动成型，没有成型计划，硫化排程id：{}", lhResultId);
            return null;
        }

        Comparator<CxScheduleResult> cxComparator = Comparator.comparingDouble(CxScheduleResult::getProductNum);
        // 如果调整量小于0，从最大的开始扣减；如果调整量大于0，从最小的开始叠加
        if (adjustQty < 0) {
            cxComparator = cxComparator.reversed();
        }
        List<CxScheduleResult> cxSortList = cxResultList.stream().sorted(cxComparator).collect(Collectors.toList());

        // 记录需要重拍的记录（成型能够保证，根据硫化id查询出来的记录，不会是同一个机台的记录，可以无需去重再成型重排）
        List<CxScheduleResult> reScheduleList = new ArrayList<>();
        // 记录需要重拍的成型排程记录
        if (adjustQty >= 0) {
            /*
                如果调整量大于等于0，加到【成型排程计划合计】最小的记录上，进行重排
                调整等于0（可能是某班增加，其他班减少，总调整量为0）虽然不会触发成型调整，但是联动调整记录需要显示这条变更记录
             */
            CxScheduleResult firstCxResult = cxSortList.get(0);
            firstCxResult.setProductNum(BigDecimalUtil.add(firstCxResult.getProductNum(), adjustQty));
            reScheduleList.add(firstCxResult);
            adjustQty = 0;
        } else {
            // 扣减对应调整量，如果不够扣减，扣减下一条
            for (CxScheduleResult cxResult : cxSortList) {
                if (cxResult.getProductNum() != null && cxResult.getProductNum() > 0) {
                    double productNum = BigDecimalUtil.add(adjustQty, cxResult.getProductNum());
                    if (productNum >= 0) {
                        // 如果足够扣减，扣减结束
                        cxResult.setProductNum(productNum);
                        reScheduleList.add(cxResult);
                        adjustQty = 0;

                        break;
                    }

                    // 不够扣减，继续扣减下一条，如果不能扣减，直接结束
                    cxResult.setProductNum(0d);
                    reScheduleList.add(cxResult);
                    adjustQty = productNum;
                }
            }
        }
        log.info("硫化联动成型，硫化排程id：{}，调整后剩余量{}", lhResultId, adjustQty);

        // 记录对应成型ID-调整记录的Map
        Map<Long, LhCxLinkageConfirm> confirmMap = new HashMap<>();
        for (CxScheduleResult cxScheduleResult : reScheduleList) {
            List<CxScheduleResult> allTasks = cxScheduleResultService.loadRelatedTasks(cxScheduleResult);
            // 构建调整前记录
            for (CxScheduleResult cxResult : allTasks) {
                LhCxLinkageConfirm confirm = new LhCxLinkageConfirm();
                setCxBeforeConfirm(confirm, cxResult);
                confirmMap.put(cxResult.getId(), confirm);
            }

            // 执行重排
            List<CxScheduleResult> resortedTasks = cxScheduleResultService.cxScheduleResultListReSort(allTasks, cxScheduleResult.getScheduleDate());
            if (isResorted) {
                cxScheduleResultService.updateTasksStatus(resortedTasks);
            }
            
            for (CxScheduleResult resortedTask : resortedTasks) {
                // 记录重拍后的字段
                LhCxLinkageConfirm confirm = confirmMap.get(resortedTask.getId());
                if (confirm == null) {
                    confirm = new LhCxLinkageConfirm();
                }
                setCxAfterConfirm(confirm, resortedTask);
            }
        }

        return new ArrayList<>(confirmMap.values());
    }

    /**
     * 复制成型修改前的字段
     *
     * @param confirm       联动确认记录
     * @param cxAfterResult 成型排程
     */
    private void setCxAfterConfirm(LhCxLinkageConfirm confirm, CxScheduleResult cxAfterResult) {
        // 复制成型修改后的字段
        confirm.setNewCxMachineCode(cxAfterResult.getCxMachineCode());
        confirm.setNewCxSpecCode(cxAfterResult.getSpecCode());
        confirm.setNewCxEmbryoCode(cxAfterResult.getEmbryoCode());
        confirm.setNewCxClass1PlanQty(cxAfterResult.getClass1PlanQty());
        confirm.setNewCxClass2PlanQty(cxAfterResult.getClass2PlanQty());
        confirm.setNewCxClass3PlanQty(cxAfterResult.getClass3PlanQty());
        confirm.setNewCxClass4PlanQty(cxAfterResult.getClass4PlanQty());
        confirm.setNewCxClass5PlanQty(cxAfterResult.getClass5PlanQty());
        confirm.setNewCxClass6PlanQty(cxAfterResult.getClass6PlanQty());
    }

    /**
     * 复制成型修改前的字段
     *
     * @param confirm        联动确认记录
     * @param cxBeforeResult 成型排程
     */
    private void setCxBeforeConfirm(LhCxLinkageConfirm confirm, CxScheduleResult cxBeforeResult) {
        // 复制成型修改前的字段
        confirm.setCxScheduleId(cxBeforeResult.getId());
        confirm.setOriCxMachineCode(cxBeforeResult.getCxMachineCode());
        confirm.setOriCxSpecCode(cxBeforeResult.getSpecCode());
        confirm.setOriCxEmbryoCode(cxBeforeResult.getEmbryoCode());
        confirm.setOriCxClass1PlanQty(cxBeforeResult.getClass1PlanQty());
        confirm.setOriCxClass2PlanQty(cxBeforeResult.getClass2PlanQty());
        confirm.setOriCxClass3PlanQty(cxBeforeResult.getClass3PlanQty());
        confirm.setOriCxClass4PlanQty(cxBeforeResult.getClass4PlanQty());
        confirm.setOriCxClass5PlanQty(cxBeforeResult.getClass5PlanQty());
        confirm.setOriCxClass6PlanQty(cxBeforeResult.getClass6PlanQty());
    }

    /**
     * 根据硫化记录，构建联动调整记录
     *
     * @param confirm      硫化联动调整记录
     * @param originResult 硫化排程记录
     * @param adjustQty    调整数量
     * @param operType     操作类型
     */
    private void setLhConfirm(LhCxLinkageConfirm confirm, LhScheduleResult originResult, long adjustQty, String operType) {
        // 复制硫化字段
        confirm.setFactoryCode(originResult.getFactoryCode());
        confirm.setBatchNo(originResult.getBatchNo());
        confirm.setOrderNo(originResult.getOrderNo());
        confirm.setLhScheduleId(originResult.getId());
        confirm.setLhMachineCode(originResult.getLhMachineCode());
        confirm.setSpecCode(originResult.getSpecCode());
        confirm.setEmbryoCode(originResult.getEmbryoCode());
        confirm.setSpecDesc(originResult.getSpecDesc());
        confirm.setAdjustType(operType);
        confirm.setAdjustQty(adjustQty);
        confirm.setScheduleDate(originResult.getScheduleDate());
    }

    /**
     * 生成硫化排程调整信息
     * @param lhScheduleResult
     * @return
     */
    @Override
    @Deprecated
    public AjaxResult generateLhScheduleAdjust(LhScheduleResult lhScheduleResult) {
        //1. 获取当前最新的排产结果
        LhScheduleResult oriLhScheduleResult = lhScheduleResultEntityMapper.selectById(lhScheduleResult.getId());
        //2. 获取拼模中的另一模排产结果
        LhScheduleResult oppositeLhScheduleResult = getOppositeLhScheduleResult(lhScheduleResult);
        //3.解析计划的调整原因,并更新硫化排程自身的计划量
        String adjustReason = "";
        int adjustQty = 0;
        int totalAdjustQty = 0;
        int adjustClass = 0;
        for (int i = 1; i<= ApsConstant.FOUR_CLASS; i++){
            adjustReason = (String)lhScheduleResult.getFieldValueByFieldName("class"+i+"Analysis");
            if (AdjustTypeEnums.PART_ADJUST.getCode().equals(adjustReason)){
                //3.1 部分调整，仅影响当前班次计划
                adjustQty = getAdjustPlanQty(lhScheduleResult, oriLhScheduleResult, i);
                adjustClass = adjustQty != 0 ? i:0;
                totalAdjustQty = totalAdjustQty + adjustQty;
                if (!StringUtil.isEmptyWithTrim(lhScheduleResult.getLeftRightMold())
                        && oppositeLhScheduleResult != null){
                    //若存在左右模，则同步更新另一模的计划量及调整原因
                    //累加另一模的调整量
                    totalAdjustQty = totalAdjustQty + adjustQty;
                    oppositeLhScheduleResult.setFieldValueByFieldName("class"+i+"Analysis",adjustReason);
                    oppositeLhScheduleResult.setFieldValueByFieldName("class"+i+"PlanQty",lhScheduleResult.getFieldValueByFieldName("class"+i+"PlanQty"));
                }
            }else if (AdjustTypeEnums.ALL_ADJUST.getCode().equals(adjustReason)){
                //3.2 整体调整，除了调整当前班次计划，将后续班次全部调整为0
                adjustQty = getAdjustPlanQty(lhScheduleResult, oriLhScheduleResult, i);
                adjustClass = adjustQty != 0 ? i:0;
                totalAdjustQty = totalAdjustQty + adjustQty;
                if (!StringUtil.isEmptyWithTrim(lhScheduleResult.getLeftRightMold())
                        && oppositeLhScheduleResult != null){
                    //若存在左右模，则同步更新另一模的计划量及调整原因
                    //累加另一模的调整量
                    totalAdjustQty = totalAdjustQty + adjustQty;
                    oppositeLhScheduleResult.setFieldValueByFieldName("class"+i+"Analysis",adjustReason);
                    oppositeLhScheduleResult.setFieldValueByFieldName("class"+i+"PlanQty",lhScheduleResult.getFieldValueByFieldName("class"+i+"PlanQty"));
                }
                for (int j=i+1;j<=ApsConstant.FOUR_CLASS;j++){
                    if (lhScheduleResult.getFieldValueByFieldName("class"+ i +"PlanQty") != null){
                        adjustQty = adjustQty +(Integer) lhScheduleResult.getFieldValueByFieldName("class"+ i +"PlanQty");
                    }
                    lhScheduleResult.setFieldValueByFieldName("class"+j+"Analysis",AdjustTypeEnums.ALL_ADJUST.getCode());
                    lhScheduleResult.setFieldValueByFieldName("class"+j+"PlanQty",0);
                    if (!StringUtil.isEmptyWithTrim(lhScheduleResult.getLeftRightMold())
                            && oppositeLhScheduleResult != null){
                        //若存在左右模，则同步更新另一模的计划量及调整原因
                        if (oppositeLhScheduleResult.getFieldValueByFieldName("class"+ i +"PlanQty") != null){
                            adjustQty = adjustQty +(Integer) oppositeLhScheduleResult.getFieldValueByFieldName("class"+ i +"PlanQty");
                        }
                        oppositeLhScheduleResult.setFieldValueByFieldName("class"+i+"Analysis",adjustReason);
                        oppositeLhScheduleResult.setFieldValueByFieldName("class"+i+"PlanQty",lhScheduleResult.getFieldValueByFieldName("class"+i+"PlanQty"));
                    }
                }
                break;
            }
        }
        //更新硫化计划的计划量及调整原因
        lhScheduleResultEntityMapper.updateById(lhScheduleResult);
        if (oppositeLhScheduleResult != null){
            lhScheduleResultEntityMapper.updateById(oppositeLhScheduleResult);
        }

        if (adjustClass ==0){
            //若没有调整班次，直接返回
            return AjaxResult.success();
        }
        //4.更新成型计划的联动计划量
        //4.1 获取硫化计划对应的成型计划
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", lhScheduleResult.getFactoryCode());
        queryWrapper.eq("SCHEDULE_DATE", lhScheduleResult.getScheduleDate());
        queryWrapper.in("LH_MACHINE_CODE", lhScheduleResult.getLhMachineCode());
        queryWrapper.eq("IS_DELETE", Constant.FALSE);
        CxScheduleResult linkCxScheduleResult = cxScheduleResultEntityMapper.selectOne(queryWrapper);
        //4.2 根据成型机台信息，获取成型计划
        QueryWrapper<CxScheduleResult> mQueryWrapper = new QueryWrapper<>();
        mQueryWrapper.eq("FACTORY_CODE", linkCxScheduleResult.getFactoryCode());
        mQueryWrapper.eq("SCHEDULE_DATE", linkCxScheduleResult.getScheduleDate());
        mQueryWrapper.eq("CX_MACHINE_CODE", linkCxScheduleResult.getCxMachineCode());
        mQueryWrapper.eq("IS_DELETE", Constant.FALSE);
        List<CxScheduleResult> cxScheduleResultList = cxScheduleResultEntityMapper.selectList(mQueryWrapper);
        if (PubUtil.isNotEmpty(cxScheduleResultList)){
            Optional<CxScheduleResult> cxScheduleResultOpt = cxScheduleResultList.stream().filter(x->x.getEmbryoCode().equals(lhScheduleResult.getEmbryoCode())).findFirst();
            if (cxScheduleResultOpt.isPresent()){
                //剩余量=一次投产量-总调整量
                int remainQty = cxScheduleResultOpt.get().getProductNum().intValue() - totalAdjustQty;
                //汇总调整班次前的总计划量
                int beforeClassPlanQty =0;
                for (int m=1;m<adjustClass; m++){
                    if (cxScheduleResultOpt.get().getFieldValueByFieldName("class"+adjustClass+"PlanQty") != null){
                        beforeClassPlanQty = beforeClassPlanQty+(Integer) cxScheduleResultOpt.get().getFieldValueByFieldName("class"+adjustClass+"PlanQty");
                    }
                }
                //计算调整班次的计划量
                int curAdjustQty = remainQty-beforeClassPlanQty;
                cxScheduleResultOpt.get().setFieldValueByFieldName("class"+adjustClass+"PlanQty",curAdjustQty);

                //调整班次根据排产顺序排序
                cxScheduleResultList = cxScheduleResultList.stream().sorted(Comparator.nullsLast(Comparator.comparing(CxScheduleResult::getClass1Sort))).collect(Collectors.toList());
            }

        }

        return null;
    }

    /**
     * 确认联动调整记录
     *
     * @param linkageConfirm 联动调整记录
     */
    @Override
    public AjaxResult confirmAdjust(LhCxLinkageConfirm linkageConfirm) {
        LhCxLinkageConfirm originConfirm = lhScheduleAdjustEntityMapper.selectById(linkageConfirm.getId());
        if (originConfirm.getAdjustQty() == null || originConfirm.getAdjustQty() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.LhCxLinkageConfirm.adjustQtyZero"));
        }

        // 查询所有的联动调整记录
        List<LhCxLinkageConfirm> oldConfirmList = lhScheduleAdjustEntityMapper.selectList(Wrappers.lambdaQuery(LhCxLinkageConfirm.class)
                .eq(LhCxLinkageConfirm::getAdjustBatchNo, originConfirm.getAdjustBatchNo())
                .eq(LhCxLinkageConfirm::getLhScheduleId, originConfirm.getLhScheduleId()));
        if (oldConfirmList.stream().anyMatch(v -> ApsConstant.TRUE.equals(v.getIsConfirm()))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.LhCxLinkageConfirm.alreadyConfirm"));
        }
        
        Map<Long, LhCxLinkageConfirm> oldConfirmMap = oldConfirmList.stream()
                .collect(Collectors.toMap(LhCxLinkageConfirm::getCxScheduleId, Function.identity(), (v1, v2) -> v1));

        // 调整前后理论上的调整记录条数是不变的
        List<LhCxLinkageConfirm> newConfirmList = changeCxResult(originConfirm.getLhScheduleId(), originConfirm.getAdjustQty(), true);
        if (CollectionUtils.isEmpty(newConfirmList)) {
            log.error("硫化联动成型，硫化排程id：{}，确认后的成型记录为空", originConfirm.getLhScheduleId());
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.LhCxLinkageConfirm.cxEmpty"));
        }

        // 修改记录、新增记录
        List<LhCxLinkageConfirm> insertList = new ArrayList<>();
        List<LhCxLinkageConfirm> updateList = new ArrayList<>();
        // 处理新排程的记录
        for (LhCxLinkageConfirm newConfirm : newConfirmList) {
            LhCxLinkageConfirm oldConfirm = oldConfirmMap.get(newConfirm.getCxScheduleId());
            if (oldConfirm != null) {
                // 如果历史联动记录存在，仅更新历史联动记录的成型修改前后相关字段
                oldConfirm.setOriCxMachineCode(newConfirm.getOriCxMachineCode());
                oldConfirm.setOriCxSpecCode(newConfirm.getOriCxSpecCode());
                oldConfirm.setOriCxEmbryoCode(newConfirm.getOriCxEmbryoCode());
                oldConfirm.setOriCxClass1PlanQty(newConfirm.getOriCxClass1PlanQty());
                oldConfirm.setOriCxClass2PlanQty(newConfirm.getOriCxClass2PlanQty());
                oldConfirm.setOriCxClass3PlanQty(newConfirm.getOriCxClass3PlanQty());
                oldConfirm.setOriCxClass4PlanQty(newConfirm.getOriCxClass4PlanQty());
                oldConfirm.setOriCxClass5PlanQty(newConfirm.getOriCxClass5PlanQty());
                oldConfirm.setOriCxClass6PlanQty(newConfirm.getOriCxClass6PlanQty());
                
                oldConfirm.setNewCxMachineCode(newConfirm.getNewCxMachineCode());
                oldConfirm.setNewCxSpecCode(newConfirm.getNewCxSpecCode());
                oldConfirm.setNewCxEmbryoCode(newConfirm.getNewCxEmbryoCode());
                oldConfirm.setNewCxClass1PlanQty(newConfirm.getNewCxClass1PlanQty());
                oldConfirm.setNewCxClass2PlanQty(newConfirm.getNewCxClass2PlanQty());
                oldConfirm.setNewCxClass3PlanQty(newConfirm.getNewCxClass3PlanQty());
                oldConfirm.setNewCxClass4PlanQty(newConfirm.getNewCxClass4PlanQty());
                oldConfirm.setNewCxClass5PlanQty(newConfirm.getNewCxClass5PlanQty());
                oldConfirm.setNewCxClass6PlanQty(newConfirm.getNewCxClass6PlanQty());

                oldConfirm.setIsConfirm(ApsConstant.TRUE);
                updateList.add(oldConfirm);
            } else {
                // 如果历史联动不存在，复制硫化/联动调整信息，保存新联动记录
                newConfirm.setFactoryCode(originConfirm.getFactoryCode());
                newConfirm.setBatchNo(originConfirm.getBatchNo());
                newConfirm.setOrderNo(originConfirm.getOrderNo());
                newConfirm.setAdjustBatchNo(originConfirm.getAdjustBatchNo());
                newConfirm.setLhScheduleId(originConfirm.getLhScheduleId());
                newConfirm.setLhMachineCode(originConfirm.getLhMachineCode());
                newConfirm.setSpecCode(originConfirm.getSpecCode());
                newConfirm.setEmbryoCode(originConfirm.getEmbryoCode());
                newConfirm.setAdjustType(originConfirm.getAdjustType());
                newConfirm.setAdjustQty(originConfirm.getAdjustQty());
                newConfirm.setScheduleDate(originConfirm.getScheduleDate());
                
                newConfirm.setIsConfirm(ApsConstant.TRUE);
                insertList.add(newConfirm);
            }
        }

        // 处理历史排程的记录，如果历史有，现在没有，将修改后的字段清空或者设置为0
        Set<Long> newCxScheduleSet = newConfirmList.stream().map(LhCxLinkageConfirm::getCxScheduleId).collect(Collectors.toSet());
        for (LhCxLinkageConfirm oldConfirm : oldConfirmList) {
            if (!newCxScheduleSet.contains(oldConfirm.getCxScheduleId())) {
                oldConfirm.setNewCxMachineCode("");
                oldConfirm.setNewCxSpecCode("");
                oldConfirm.setNewCxEmbryoCode("");
                oldConfirm.setNewCxClass1PlanQty(0);
                oldConfirm.setNewCxClass2PlanQty(0);
                oldConfirm.setNewCxClass3PlanQty(0);
                oldConfirm.setNewCxClass4PlanQty(0);
                oldConfirm.setNewCxClass5PlanQty(0);
                oldConfirm.setNewCxClass6PlanQty(0);

                oldConfirm.setIsConfirm(ApsConstant.TRUE);
                updateList.add(oldConfirm);
            }
        }

        this.baseDao.insertBatch(insertList);
        this.baseDao.updateBatch(updateList);

        return AjaxResult.success();
    }

    /**
     * 列表查询
     *
     * @param query
     */
    @Override
    public List<LhCxLinkageConfirm> selectList(LhCxLinkageConfirm query) {
        QueryWrapper<LhCxLinkageConfirm> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, query);
        return lhScheduleAdjustEntityMapper.selectList(wrapper);
    }

    protected void builderCondition(QueryWrapper<LhCxLinkageConfirm> queryWrapper, LhCxLinkageConfirm queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("batchNo")), "BATCH_NO", queryVO.getFieldValueByFieldName("batchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("adjustBatchNo")), "ADJUST_BATCH_NO", queryVO.getFieldValueByFieldName("adjustBatchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhScheduleId")), "LH_SCHEDULE_ID", queryVO.getFieldValueByFieldName("lhScheduleId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineCode")), "LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("lhMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specDesc")), "SPEC_DESC", queryVO.getFieldValueByFieldName("specDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("adjustType")), "ADJUST_TYPE", queryVO.getFieldValueByFieldName("adjustType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("adjustQty")), "ADJUST_QTY", queryVO.getFieldValueByFieldName("adjustQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxScheduleId")), "CX_SCHEDULE_ID", queryVO.getFieldValueByFieldName("cxScheduleId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxMachineCode")), "ORI_CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("oriCxMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxSpecCode")), "ORI_CX_SPEC_CODE", queryVO.getFieldValueByFieldName("oriCxSpecCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxEmbryoCode")), "ORI_CX_EMBRYO_CODE", queryVO.getFieldValueByFieldName("oriCxEmbryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxClass1PlanQty")), "ORI_CX_CLASS1_PLAN_QTY", queryVO.getFieldValueByFieldName("oriCxClass1PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxClass2PlanQty")), "ORI_CX_CLASS2_PLAN_QTY", queryVO.getFieldValueByFieldName("oriCxClass2PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxClass3PlanQty")), "ORI_CX_CLASS3_PLAN_QTY", queryVO.getFieldValueByFieldName("oriCxClass3PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxClass4PlanQty")), "ORI_CX_CLASS4_PLAN_QTY", queryVO.getFieldValueByFieldName("oriCxClass4PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxClass5PlanQty")), "ORI_CX_CLASS5_PLAN_QTY", queryVO.getFieldValueByFieldName("oriCxClass5PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriCxClass6PlanQty")), "ORI_CX_CLASS6_PLAN_QTY", queryVO.getFieldValueByFieldName("oriCxClass6PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxMachineCode")), "NEW_CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("newCxMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxSpecCode")), "NEW_CX_SPEC_CODE", queryVO.getFieldValueByFieldName("newCxSpecCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxEmbryoCode")), "NEW_CX_EMBRYO_CODE", queryVO.getFieldValueByFieldName("newCxEmbryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxClass1PlanQty")), "NEW_CX_CLASS1_PLAN_QTY", queryVO.getFieldValueByFieldName("newCxClass1PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxClass2PlanQty")), "NEW_CX_CLASS2_PLAN_QTY", queryVO.getFieldValueByFieldName("newCxClass2PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxClass3PlanQty")), "NEW_CX_CLASS3_PLAN_QTY", queryVO.getFieldValueByFieldName("newCxClass3PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxClass4PlanQty")), "NEW_CX_CLASS4_PLAN_QTY", queryVO.getFieldValueByFieldName("newCxClass4PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxClass5PlanQty")), "NEW_CX_CLASS5_PLAN_QTY", queryVO.getFieldValueByFieldName("newCxClass5PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newCxClass6PlanQty")), "NEW_CX_CLASS6_PLAN_QTY", queryVO.getFieldValueByFieldName("newCxClass6PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isConfirm")), "IS_CONFIRM", queryVO.getFieldValueByFieldName("isConfirm"));
    }

    /**
     * 获取调整计划量
     * @param lhScheduleResult
     * @param oriLhScheduleResult
     * @param i
     * @return
     */
    private int getAdjustPlanQty(LhScheduleResult lhScheduleResult, LhScheduleResult oriLhScheduleResult, int i) {
        int oriPlanQty = 0;
        if (oriLhScheduleResult.getFieldValueByFieldName("class"+ i +"PlanQty") != null){
            oriPlanQty = (Integer) oriLhScheduleResult.getFieldValueByFieldName("class"+ i +"PlanQty");
        }
        int newPlanQty = 0;
        if (lhScheduleResult.getFieldValueByFieldName("class"+ i +"PlanQty") != null){
            newPlanQty = (Integer) lhScheduleResult.getFieldValueByFieldName("class"+ i +"PlanQty");
        }
        return newPlanQty - oriPlanQty;
    }

    /**
     * 获取另一模的硫化排程结果
     * @param lhScheduleResult
     * @return
     */
    private LhScheduleResult getOppositeLhScheduleResult(LhScheduleResult lhScheduleResult) {
        QueryWrapper<LhScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", lhScheduleResult.getFactoryCode());
        queryWrapper.eq("SCHEDULE_DATE", lhScheduleResult.getScheduleDate());
        queryWrapper.eq("LH_MACHINE_CODE", lhScheduleResult.getLhMachineCode());
        queryWrapper.eq("SPEC_CODE", lhScheduleResult.getSpecCode());
        String leftRightMold = lhScheduleResult.getLeftRightMold();
        if (leftRightMold.contains(ApsConstant.L_MOLD)) {
            leftRightMold = leftRightMold.replace(ApsConstant.L_MOLD, ApsConstant.R_MOLD);
        }
        if (leftRightMold.contains(ApsConstant.R_MOLD)) {
            leftRightMold = leftRightMold.replace(ApsConstant.R_MOLD, ApsConstant.L_MOLD);
        }
        queryWrapper.eq("LEFT_RIGHT_MOLD", leftRightMold);
        queryWrapper.eq("IS_DELETE", Constant.FALSE);
        return lhScheduleResultEntityMapper.selectOne(queryWrapper);
    }
}
