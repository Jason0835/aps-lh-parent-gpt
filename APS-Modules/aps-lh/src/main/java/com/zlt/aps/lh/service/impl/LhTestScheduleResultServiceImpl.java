package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.CommonRedisService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constants.LhPrefixConstants;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhTestScheduleResult;
import com.zlt.aps.lh.mapper.LhMoldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultEntityMapper;
import com.zlt.aps.lh.mapper.LhTestScheduleResultEntityMapper;
import com.zlt.aps.lh.mapper.LhUnscheduledResultEntityMapper;
import com.zlt.aps.lh.service.ILhMachineInfoService;
import com.zlt.aps.lh.service.LhTestScheduleResultService;
import com.zlt.aps.maindata.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.api.domain.vo.MdmProductConstructionVO;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化测试排程服务实现类。
 * 负责将线下测试排程数据转换为系统可落库的硫化排程结果，并按排程日期进行覆盖写入。
 *
 * 核心流程：
 * 1) 读取排程日及次日测试排程数据；
 * 2) 拆分左右模复合规格，并平分相关计划/完成数量；
 * 3) 合并“当日夜班 + 次日早班”形成完整排程；
 * 4) 回填工艺主数据并计算单班理论产能；
 * 5) 执行机台顺序公式并批量入库。
 *
 * @author xh
 * @version 1.0
 * @date 2025/2/13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhTestScheduleResultServiceImpl extends AbstractDocService<LhTestScheduleResult> implements LhTestScheduleResultService {

    @Autowired
    private LhTestScheduleResultEntityMapper lhTestScheduleResultEntityMapper;
    @Autowired
    private LhScheduleResultEntityMapper lhScheduleResultEntityMapper;
    @Autowired
    private LhMoldChangePlanEntityMapper lhMoldChangePlanMapper;

    @Autowired
    private LhUnscheduledResultEntityMapper lhUnscheduledResultEntityMapper;

    @Autowired
    private IMdmProductConstructionService mdmProductConstructionService;

    @Autowired
    private CommonRedisService commonCacheService;

    @Autowired
    private MdmProductConstructionEntityMapper productConstructionEntityMapper;

    @Autowired
    private ILhMachineInfoService lhMachineInfoService;

    /**
     * 硫化Test自动排程
     * 注：仅供测试使用
     *
     * 说明：
     * - 以“机台编码 + 规格编码”为唯一组合键进行合并；
     * - 当夜班存在时，夜班写入Class1/2，次日早班写入Class4/5；
     * - 当夜班不存在时，仅根据次日早班生成排程；
     * - 最终会清空同工厂同日期旧排程后再写入新结果，保证幂等覆盖。
     *
     * @param autoLhScheduleResultDTO 自动排程参数（工厂、排程日期、月计划版本）
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoLhTestScheduleResult(AutoLhScheduleResultDTO autoLhScheduleResultDTO) throws BusinessException {
        //1. 获取线下排程数据
        Date scheduleTime = autoLhScheduleResultDTO.getScheduleTime();
        String factoryCode = autoLhScheduleResultDTO.getFactoryCode();
        String monthPlanVersion = autoLhScheduleResultDTO.getMonthPlanVersion();
        LambdaQueryWrapper<LhTestScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhTestScheduleResult::getScheduleDate, scheduleTime);
        List<LhTestScheduleResult> list = lhTestScheduleResultEntityMapper.selectList(wrapper);
        Calendar cal = Calendar.getInstance();
        cal.setTime(scheduleTime);
        // 排程日期增加一天
        cal.add(Calendar.DAY_OF_MONTH, 1);
        LambdaQueryWrapper<LhTestScheduleResult> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(LhTestScheduleResult::getScheduleDate, cal.getTime());
        List<LhTestScheduleResult> list2 = lhTestScheduleResultEntityMapper.selectList(wrapper2);
        //2. 拆分左右模：将“左右模复合规格”拆成两条记录，并同步处理胚体与数量字段
        List<LhTestScheduleResult> allList = new ArrayList<>();
        List<LhTestScheduleResult> tList =  splitLeftRightMold(list, allList);
        List<LhTestScheduleResult> tList2 =  splitLeftRightMold(list2, allList);
        List<String> specCodes = allList.stream().map(x->x.getSpecCode()).collect(Collectors.toList());
        List<MdmProductConstructionVO> constructionList = productConstructionEntityMapper.queryByFactoryCodeAndSpecCodes2(factoryCode, specCodes);
        Map<String, MdmProductConstructionVO> specToConstructionMap = new HashMap<>();
        for (MdmProductConstructionVO cons : constructionList) {
            specToConstructionMap.put(cons.getSpecCode(), cons);
        }
        //硫化自动排程批次号
        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", autoLhScheduleResultDTO.getScheduleTime());
        String lhBatchNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_BATCH_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_BATCH_NO_PREFIX + scheduleDateStr);

        LhMachineInfo queryLhMachineInfo = new LhMachineInfo();
        queryLhMachineInfo.setFactoryCode(factoryCode);
        //queryLhMachineInfo.setStatus(YesOrNoEnum.YES.getCode());
        List<LhMachineInfo> allMachineList = lhMachineInfoService.selectList(queryLhMachineInfo);
        Map<String, List<LhMachineInfo>> machineMap = allMachineList.stream().collect(Collectors.groupingBy(item->item.getMachineCode()));
        LhMachineInfo machineInfo;
        String trialPrefix = "T,X";
        //3. 组合当日夜班、次日早班：按“机台+规格”聚合
        List<String> combineKeyList = allList.stream().map(x->x.getLhMachineCode()+ApsConstant.SPLIT_CHAR+x.getSpecCode()).distinct().collect(Collectors.toList());
        Map<String, List<LhTestScheduleResult>> scheduledMap1 = tList.stream().collect(Collectors.groupingBy(item->item.getLhMachineCode()+ApsConstant.SPLIT_CHAR+item.getSpecCode()));
        Map<String, List<LhTestScheduleResult>> scheduledMap2 = tList2.stream().collect(Collectors.groupingBy(item->item.getLhMachineCode()+ApsConstant.SPLIT_CHAR+item.getSpecCode()));
        List<LhScheduleResult> resultScheduleList = new ArrayList<>();
        for (String key:combineKeyList){
            List<LhTestScheduleResult> oneScheduleResultList = scheduledMap1.get(key);
            LhTestScheduleResult oneScheduleResult,twoScheduleResult;
            LhScheduleResult newScheduleResult = null;
            String lhOrderNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_ORDER_NO_PREFIX + scheduleDateStr);
            if (PubUtil.isNotEmpty(oneScheduleResultList)){
                //同一组合键通常仅有1笔，按首笔处理
                oneScheduleResult = oneScheduleResultList.get(0);
                newScheduleResult = new LhScheduleResult();
                newScheduleResult.setBatchNo(lhBatchNo);
                newScheduleResult.setOrderNo(lhOrderNo);
                newScheduleResult.setScheduleDate(scheduleTime);
                newScheduleResult.setRealScheduleDate(scheduleTime);
                newScheduleResult.setFactoryCode(oneScheduleResult.getFactoryCode());
                newScheduleResult.setLhMachineCode(oneScheduleResult.getLhMachineCode());
                List<LhMachineInfo> machineInfos = machineMap.get(oneScheduleResult.getLhMachineCode());
                machineInfo = machineInfos.get(0);
                newScheduleResult.setSpecCode(oneScheduleResult.getSpecCode());
                newScheduleResult.setSpecDesc(oneScheduleResult.getSpecDesc());
                newScheduleResult.setEmbryoCode(oneScheduleResult.getEmbryoCode());
                newScheduleResult.setLeftRightMold(oneScheduleResult.getLeftRightMold());
                newScheduleResult.setMonthPlanVersion(monthPlanVersion);
                if (machineInfo.getMaxMoldNum() == 1){
                    newScheduleResult.setMoldQty(1);
                }else{
                    newScheduleResult.setMoldQty(StringUtils.isBlank(oneScheduleResult.getLeftRightMold()) ? 2:1);
                }
                MdmProductConstructionVO cons = specToConstructionMap.get(oneScheduleResult.getSpecCode());
                if (cons != null){
                    if (StringUtils.isNotEmpty(trialPrefix) && StringUtils.isNotEmpty(cons.getConstructionCode())){
                        newScheduleResult.setIsTrial(trialPrefix.indexOf(cons.getConstructionCode().substring(0,1)) >= 0 ? ApsConstant.TRUE:ApsConstant.FALSE);
                    }
                    newScheduleResult.setBomVersion(cons.getBomVersion());
                    newScheduleResult.setMouldMethod(cons.getMouldMethod());
                    if (cons.getCuringTime() != null) {
                        newScheduleResult.setLhTime(BigDecimal.valueOf(cons.getCuringTime()));
                        int singleMoldShiftLhQty = calcPeriodCapacity(BigDecimal.valueOf(43200),BigDecimalUtils.add(BigDecimal.valueOf(cons.getCuringTime()),180),newScheduleResult.getMoldQty());
                        newScheduleResult.setSingleMoldShiftLhQty(singleMoldShiftLhQty);
                    }


                    if (StringUtils.isEmpty(newScheduleResult.getProductCode())){
                        newScheduleResult.setProductCode(cons.getProductCode());
                    }
                }
                //对应当日夜班（Class1/2）
                newScheduleResult.setClass1PlanQty(oneScheduleResult.getClass1PlanQty());
                newScheduleResult.setClass2PlanQty(oneScheduleResult.getClass2PlanQty());
                newScheduleResult.setClass1FinishQty(oneScheduleResult.getClass1FinishQty());
                newScheduleResult.setClass2FinishQty(oneScheduleResult.getClass2FinishQty());
                List<LhTestScheduleResult> twoScheduleResultList = scheduledMap2.get(key);
                if (PubUtil.isNotEmpty(twoScheduleResultList)){
                    twoScheduleResult = twoScheduleResultList.get(0);
                    //对应次日早班（映射到Class4/5）
                    newScheduleResult.setClass4PlanQty(twoScheduleResult.getClass1PlanQty());
                    newScheduleResult.setClass5PlanQty(twoScheduleResult.getClass2PlanQty());
                }
                newScheduleResult.setDailyPlanQty((newScheduleResult.getClass1PlanQty() != null ? newScheduleResult.getClass1PlanQty():0) + (newScheduleResult.getClass2PlanQty() != null ? newScheduleResult.getClass2PlanQty():0));
            }else{
                //没有夜班时，仅使用次日早班数据生成排程
                List<LhTestScheduleResult> twoScheduleResultList = scheduledMap2.get(key);
                if (PubUtil.isNotEmpty(twoScheduleResultList)){
                    twoScheduleResult = twoScheduleResultList.get(0);
                    newScheduleResult = new LhScheduleResult();
                    newScheduleResult.setBatchNo(lhBatchNo);
                    newScheduleResult.setOrderNo(lhOrderNo);
                    newScheduleResult.setScheduleDate(scheduleTime);
                    newScheduleResult.setRealScheduleDate(cal.getTime());
                    newScheduleResult.setFactoryCode(twoScheduleResult.getFactoryCode());
                    newScheduleResult.setLhMachineCode(twoScheduleResult.getLhMachineCode());
                    machineInfo = machineMap.get(twoScheduleResult.getLhMachineCode()).get(0);
                    newScheduleResult.setSpecCode(twoScheduleResult.getSpecCode());
                    newScheduleResult.setSpecDesc(twoScheduleResult.getSpecDesc());
                    newScheduleResult.setEmbryoCode(twoScheduleResult.getEmbryoCode());
                    newScheduleResult.setLeftRightMold(twoScheduleResult.getLeftRightMold());
                    newScheduleResult.setMonthPlanVersion(monthPlanVersion);
                    if (machineInfo.getMaxMoldNum() == 1){
                        newScheduleResult.setMoldQty(1);
                    }else{
                        newScheduleResult.setMoldQty(StringUtils.isBlank(twoScheduleResult.getLeftRightMold()) ? 2:1);
                    }
                    MdmProductConstructionVO cons = specToConstructionMap.get(twoScheduleResult.getSpecCode());
                    if (cons != null){
                        if (StringUtils.isNotEmpty(trialPrefix) && StringUtils.isNotEmpty(cons.getConstructionCode())){
                            newScheduleResult.setIsTrial(trialPrefix.indexOf(cons.getConstructionCode().substring(0,1)) >= 0 ? ApsConstant.TRUE:ApsConstant.FALSE);
                        }
                        newScheduleResult.setBomVersion(cons.getBomVersion());
                        newScheduleResult.setMouldMethod(cons.getMouldMethod());
                        if (cons.getCuringTime() != null) {
                            newScheduleResult.setLhTime(BigDecimalUtils.add(BigDecimal.valueOf(cons.getCuringTime()), 180));
                            int singleMoldShiftLhQty = calcPeriodCapacity(BigDecimal.valueOf(43200), BigDecimal.valueOf(cons.getCuringTime()), newScheduleResult.getMoldQty());
                            newScheduleResult.setSingleMoldShiftLhQty(singleMoldShiftLhQty);
                        }
                        if (StringUtils.isEmpty(newScheduleResult.getProductCode())){
                            newScheduleResult.setProductCode(cons.getProductCode());
                        }
                    }
                    //对应次日早班（映射到Class4/5）
                    newScheduleResult.setClass4PlanQty(twoScheduleResult.getClass1PlanQty());
                    newScheduleResult.setClass5PlanQty(twoScheduleResult.getClass2PlanQty());
                    //newScheduleResult.setDailyPlanQty(newScheduleResult.getClass2PlanQty());
                }
            }
            if (newScheduleResult != null){
                resultScheduleList.add(newScheduleResult);
            }
        }
       //4. 生成测试排程数据：先执行公式，再覆盖写入
        if (PubUtil.isNotEmpty(resultScheduleList)){
            execMachineOrderFormulas(resultScheduleList);
            doScheduleResult(autoLhScheduleResultDTO, scheduleTime, factoryCode, resultScheduleList);
        }
        return;
    }

    /**
     * 硫化Test自动排程
     * 注：仅供成型使用
     *
     * 与测试场景方法的主要区别：
     * - 模具数直接按左右模字段判断，不叠加机台最大模位规则；
     * - 日计划量写入口径保持成型侧现有逻辑。
     *
     * @param autoLhScheduleResultDTO 自动排程参数（工厂、排程日期、月计划版本）
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoLhTestScheduleResultCx(AutoLhScheduleResultDTO autoLhScheduleResultDTO) throws BusinessException {
        //1. 获取线下排程数据
        Date scheduleTime = autoLhScheduleResultDTO.getScheduleTime();
        String factoryCode = autoLhScheduleResultDTO.getFactoryCode();
        String monthPlanVersion = autoLhScheduleResultDTO.getMonthPlanVersion();
        LambdaQueryWrapper<LhTestScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhTestScheduleResult::getScheduleDate, scheduleTime);
        List<LhTestScheduleResult> list = lhTestScheduleResultEntityMapper.selectList(wrapper);
        Calendar cal = Calendar.getInstance();
        cal.setTime(scheduleTime);
        // 排程日期增加一天
        cal.add(Calendar.DAY_OF_MONTH, 1);
        LambdaQueryWrapper<LhTestScheduleResult> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(LhTestScheduleResult::getScheduleDate, cal.getTime());
        List<LhTestScheduleResult> list2 = lhTestScheduleResultEntityMapper.selectList(wrapper2);
        //2. 拆分左右模
        List<LhTestScheduleResult> allList = new ArrayList<>();
        List<LhTestScheduleResult> tList =  splitLeftRightMold(list, allList);
        List<LhTestScheduleResult> tList2 =  splitLeftRightMold(list2, allList);
        List<String> specCodes = allList.stream().map(x->x.getSpecCode()).collect(Collectors.toList());
        List<MdmProductConstructionVO> constructionList = productConstructionEntityMapper.queryByFactoryCodeAndSpecCodes2(factoryCode, specCodes);
        Map<String, MdmProductConstructionVO> specToConstructionMap = new HashMap<>();
        for (MdmProductConstructionVO cons : constructionList) {
            specToConstructionMap.put(cons.getSpecCode(), cons);
        }
        //硫化自动排程批次号
        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", autoLhScheduleResultDTO.getScheduleTime());
        String lhBatchNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_BATCH_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_BATCH_NO_PREFIX + scheduleDateStr);

        //3. 组合当日夜班、次日早班
        List<String> combineKeyList = allList.stream().map(x->x.getLhMachineCode()+ApsConstant.SPLIT_CHAR+x.getSpecCode()).distinct().collect(Collectors.toList());
        Map<String, List<LhTestScheduleResult>> scheduledMap1 = tList.stream().collect(Collectors.groupingBy(item->item.getLhMachineCode()+ApsConstant.SPLIT_CHAR+item.getSpecCode()));
        Map<String, List<LhTestScheduleResult>> scheduledMap2 = tList2.stream().collect(Collectors.groupingBy(item->item.getLhMachineCode()+ApsConstant.SPLIT_CHAR+item.getSpecCode()));
        List<LhScheduleResult> resultScheduleList = new ArrayList<>();
        for (String key:combineKeyList){
            List<LhTestScheduleResult> oneScheduleResultList = scheduledMap1.get(key);
            LhTestScheduleResult oneScheduleResult,twoScheduleResult;
            LhScheduleResult newScheduleResult = null;
            String lhOrderNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_ORDER_NO_PREFIX + scheduleDateStr);
            if (PubUtil.isNotEmpty(oneScheduleResultList)){
                //当作只有1笔，正常也是
                oneScheduleResult = oneScheduleResultList.get(0);
                newScheduleResult = new LhScheduleResult();
                newScheduleResult.setBatchNo(lhBatchNo);
                newScheduleResult.setOrderNo(lhOrderNo);
                newScheduleResult.setScheduleDate(scheduleTime);
                newScheduleResult.setRealScheduleDate(scheduleTime);
                newScheduleResult.setFactoryCode(oneScheduleResult.getFactoryCode());
                newScheduleResult.setLhMachineCode(oneScheduleResult.getLhMachineCode());
                newScheduleResult.setSpecCode(oneScheduleResult.getSpecCode());
                newScheduleResult.setSpecDesc(oneScheduleResult.getSpecDesc());
                newScheduleResult.setEmbryoCode(oneScheduleResult.getEmbryoCode());
                newScheduleResult.setLeftRightMold(oneScheduleResult.getLeftRightMold());
                newScheduleResult.setMonthPlanVersion(monthPlanVersion);
                newScheduleResult.setMoldQty(StringUtils.isBlank(oneScheduleResult.getLeftRightMold()) ? 2:1);
                MdmProductConstructionVO cons = specToConstructionMap.get(oneScheduleResult.getSpecCode());
                if (cons != null){
                    newScheduleResult.setBomVersion(cons.getBomVersion());
                    newScheduleResult.setMouldMethod(cons.getMouldMethod());
                    newScheduleResult.setLhTime(BigDecimalUtils.add(BigDecimal.valueOf(cons.getCuringTime()),180));
                    int singleMoldShiftLhQty = calcPeriodCapacity(BigDecimal.valueOf(43200),BigDecimal.valueOf(cons.getCuringTime()),newScheduleResult.getMoldQty());
                    newScheduleResult.setSingleMoldShiftLhQty(singleMoldShiftLhQty);
                }
                //对应夜班
                newScheduleResult.setClass1PlanQty(oneScheduleResult.getClass1PlanQty());
                newScheduleResult.setClass2PlanQty(oneScheduleResult.getClass2PlanQty());
                List<LhTestScheduleResult> twoScheduleResultList = scheduledMap2.get(key);
                if (PubUtil.isNotEmpty(twoScheduleResultList)){
                    twoScheduleResult = twoScheduleResultList.get(0);
                    //对应早班
                    newScheduleResult.setClass4PlanQty(twoScheduleResult.getClass1PlanQty());
                    //对应早班
                    newScheduleResult.setClass5PlanQty(twoScheduleResult.getClass2PlanQty());
                }
                newScheduleResult.setDailyPlanQty((newScheduleResult.getClass1PlanQty() != null ? newScheduleResult.getClass1PlanQty():0) + (newScheduleResult.getClass2PlanQty() != null ? newScheduleResult.getClass2PlanQty():0));
            }else{
                //没有夜班
                List<LhTestScheduleResult> twoScheduleResultList = scheduledMap2.get(key);
                if (PubUtil.isNotEmpty(twoScheduleResultList)){
                    twoScheduleResult = twoScheduleResultList.get(0);
                    newScheduleResult = new LhScheduleResult();
                    newScheduleResult.setBatchNo(lhBatchNo);
                    newScheduleResult.setOrderNo(lhOrderNo);
                    newScheduleResult.setRealScheduleDate(scheduleTime);
                    newScheduleResult.setFactoryCode(twoScheduleResult.getFactoryCode());
                    newScheduleResult.setLhMachineCode(twoScheduleResult.getLhMachineCode());
                    newScheduleResult.setSpecCode(twoScheduleResult.getSpecCode());
                    newScheduleResult.setSpecDesc(twoScheduleResult.getSpecDesc());
                    newScheduleResult.setEmbryoCode(twoScheduleResult.getEmbryoCode());
                    newScheduleResult.setLeftRightMold(twoScheduleResult.getLeftRightMold());
                    newScheduleResult.setMonthPlanVersion(monthPlanVersion);
                    newScheduleResult.setMoldQty(StringUtils.isBlank(twoScheduleResult.getLeftRightMold()) ? 2:1);
                    MdmProductConstructionVO cons = specToConstructionMap.get(twoScheduleResult.getSpecCode());
                    if (cons != null){
                        newScheduleResult.setBomVersion(cons.getBomVersion());
                        newScheduleResult.setMouldMethod(cons.getMouldMethod());
                        newScheduleResult.setLhTime(BigDecimalUtils.add(BigDecimal.valueOf(cons.getCuringTime()),180));
                        int singleMoldShiftLhQty = calcPeriodCapacity(BigDecimal.valueOf(43200),BigDecimal.valueOf(cons.getCuringTime()),newScheduleResult.getMoldQty());
                        newScheduleResult.setSingleMoldShiftLhQty(singleMoldShiftLhQty);
                    }
                    //对应早班
                    newScheduleResult.setClass4PlanQty(twoScheduleResult.getClass1PlanQty());
                    newScheduleResult.setClass5PlanQty(twoScheduleResult.getClass2PlanQty());
                    newScheduleResult.setDailyPlanQty(newScheduleResult.getClass2PlanQty());
                }
            }
            if (newScheduleResult != null){
                resultScheduleList.add(newScheduleResult);
            }
        }
        //4. 生成测试排程数据
        doScheduleResult(autoLhScheduleResultDTO, scheduleTime, factoryCode, resultScheduleList);
        return;
    }


    /**
     * 执行机台顺序公式
     * 通过公式引擎从机台主数据表回填 machineOrder，便于结果按机台顺序展示。
     *
     * @param scheduleResultList
     */
    private void execMachineOrderFormulas(List<LhScheduleResult> scheduleResultList) {
        //执行公式
        try {
            QueryFormulaUtil.execFormula(scheduleResultList, new String[]{
                    "machineOrder -> getcolvaluewithcondition(T_LH_MACHINE_INFO, MACHINE_ORDER, MACHINE_CODE, lhMachineCode, IS_DELETE = 0)",
            });
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
    }

    /**
     * 计算指定时段内的理论产能。
     *
     * 计算规则：
     * - 先用“时段秒数 / 单次硫化时长”向下取整，得到单模循环次数；
     * - 再乘以模具数得到时段产能；
     * - 双模场景若结果为奇数，补齐为偶数，避免左右模不平衡。
     *
     * @param durationSec 时段总秒数
     * @param lhTime 单次硫化时长（秒）
     * @param moldNum 模具数
     * @return 理论产能
     */
    private int calcPeriodCapacity(BigDecimal durationSec, BigDecimal lhTime, int moldNum) {
        BigDecimal periodCapacity = BigDecimalUtils.div(durationSec,lhTime,2,true,BigDecimal.ROUND_DOWN);
        int capacity = periodCapacity.intValue();
        capacity = capacity * moldNum;
        if (moldNum == 2 && capacity % 2!=0){
            capacity = capacity+1;
        }
        return capacity;
    }

    /**
     * 排程结果落库。
     * 先删除同工厂同排程日期历史数据，再批量插入本次生成结果。
     *
     * @param autoLhScheduleResultDTO 自动排程参数
     * @param scheduleTime 排程日期
     * @param factoryCode 工厂编码
     * @param resultScheduleList 排程结果列表
     */
    private void doScheduleResult(AutoLhScheduleResultDTO autoLhScheduleResultDTO, Date scheduleTime, String factoryCode, List<LhScheduleResult> resultScheduleList) {
        if (PubUtil.isEmpty(resultScheduleList)){
            return;
        }
        //String oriBatchNo = lhScheduleResultEntityMapper.selectBatchNoByScheduleDateAndFactoryCode(scheduleTime, factoryCode);
        //4.1 删除换模记录
        /*LambdaQueryWrapper<LhMoldChangePlan> unReleasedQuery = new LambdaQueryWrapper<>();
        unReleasedQuery.eq(LhMoldChangePlan::getFactoryCode, autoLhScheduleResultDTO.getFactoryCode())
                .eq(LhMoldChangePlan::getIsRelease, ApsConstant.NO_RELEASE)
                .eq(LhMoldChangePlan::getLhResultBatchNo, oriBatchNo);
        lhMoldChangePlanMapper.delete(unReleasedQuery);*/
        //4.2 删除未排
        /*Map<String, Object> params = new HashMap<>();
        params.put("FACTORY_CODE", factoryCode);
        params.put("BATCH_NO", oriBatchNo);
        lhUnscheduledResultEntityMapper.deleteByMap(params);*/
        //4.3 删除排程
        lhScheduleResultEntityMapper.deleteByFactoryCodeAndScheduleDate(factoryCode, scheduleTime);
        //4.4 插入新排程
        baseDao.insertBatch(resultScheduleList);
    }

    /**
     * 拆分左右模复合记录。
     *
     * 处理规则：
     * - 当规格编码包含“*”时拆分为左右两条；
     * - 胚体编码、规格描述若包含“*”，同步拆分；
     * - 班次计划/完成数量与日计划数量按 1/2 平分；
     * - 左记录标记为L，右记录标记为R。
     *
     * @param list 原始测试排程列表
     * @param allList 汇总列表（用于后续组合键构建）
     * @return 拆分后的排程列表
     */
    private List<LhTestScheduleResult> splitLeftRightMold(List<LhTestScheduleResult> list, List<LhTestScheduleResult> allList) {
        List<LhTestScheduleResult> resultList = new ArrayList<>();
        for (LhTestScheduleResult scheduleResult: list){
            if (scheduleResult.getSpecCode().contains("*")){
                String[] specArr = new String[]{scheduleResult.getSpecCode().substring(0,4),scheduleResult.getSpecCode().substring(5,9)};
                String[] embryoArr,specDescArr;
                if (scheduleResult.getEmbryoCode().contains("*")){
                    embryoArr = new String[]{scheduleResult.getEmbryoCode().substring(0,4),scheduleResult.getEmbryoCode().substring(5,9)};
                }else{
                    embryoArr = new String[]{scheduleResult.getEmbryoCode()};
                };
                int xinIndex = scheduleResult.getSpecDesc().indexOf("*");
                int totalLen = scheduleResult.getSpecDesc().length();
                if (xinIndex >=0){
                    specDescArr = new String[]{scheduleResult.getSpecDesc().substring(0,xinIndex),scheduleResult.getSpecDesc().substring(xinIndex+1,totalLen)};
                }else{
                    specDescArr = new String[]{scheduleResult.getSpecDesc()};
                };
                //循环生成左右模两条明细
                for (int i=0; i<specArr.length; i++){
                    LhTestScheduleResult newTestScheduleResult = new LhTestScheduleResult();
                    BeanUtils.copyProperties(scheduleResult,newTestScheduleResult);
                    newTestScheduleResult.setSpecCode(specArr[i]);
                    if (embryoArr.length ==2){
                        newTestScheduleResult.setEmbryoCode(embryoArr[i]);
                    }
                    if (specDescArr.length ==2){
                        newTestScheduleResult.setSpecDesc(specDescArr[i]);
                    }
                    if (newTestScheduleResult.getClass1PlanQty() != null){
                        newTestScheduleResult.setClass1PlanQty(newTestScheduleResult.getClass1PlanQty()/2);
                    }
                    if (newTestScheduleResult.getClass2PlanQty() != null){
                        newTestScheduleResult.setClass2PlanQty(newTestScheduleResult.getClass2PlanQty()/2);
                    }
                    if (newTestScheduleResult.getClass1FinishQty() != null){
                        newTestScheduleResult.setClass1FinishQty(newTestScheduleResult.getClass1FinishQty()/2);
                    }
                    if (newTestScheduleResult.getClass2FinishQty() != null){
                        newTestScheduleResult.setClass2FinishQty(newTestScheduleResult.getClass2FinishQty()/2);
                    }
                    if (newTestScheduleResult.getDailyPlanQty() != null){
                        newTestScheduleResult.setDailyPlanQty(newTestScheduleResult.getDailyPlanQty()/2);
                    }
                    if (i == 0){
                        newTestScheduleResult.setLeftRightMold("L");
                    }else{
                        newTestScheduleResult.setLeftRightMold("R");
                    }
                    resultList.add(newTestScheduleResult);
                    allList.add(newTestScheduleResult);
                }
            }else{
                resultList.add(scheduleResult);
                allList.add(scheduleResult);
            }
        }

        return resultList;
    }

    @Override
    public String getDocTypeCode() {
        return "OUT2046";
    }
}
