package com.zlt.aps.lh.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.context.AutoLhScheduleResultContextDTO;
import com.zlt.aps.enums.ReasonAnalysisEnums;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.CommonRedisService;
import com.zlt.aps.common.CommonUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constants.LhPrefixConstants;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.domain.vo.LhGanttVo;
import com.zlt.aps.lh.api.domain.vo.LhMachineInfoVo;
import com.zlt.aps.lh.api.domain.vo.LhScheduleResultVo;
import com.zlt.aps.lh.api.enums.LhParamCodeEnums;
import com.zlt.aps.lh.api.enums.MachineTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftSystemEnum;
import com.zlt.aps.lh.handle.LhScheduleResultHandle;
import com.zlt.aps.lh.mapper.LhMoldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultEntityMapper;
import com.zlt.aps.lh.mapper.LhUnscheduledResultEntityMapper;
import com.zlt.aps.lh.service.*;
import com.zlt.aps.maindata.mapper.LhMonthPlanSurplusEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.maindata.service.IMdmDeviceMaintenancePlanService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.maindata.utils.CxLhEngineUtils;
import com.zlt.aps.mp.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.aps.mp.api.domain.vo.*;
import com.zlt.aps.mp.api.remoteService.IProductStockMonthRemoteApiService;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanProdFinalRemoteService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhScheduleResultServiceImpl extends AbstractDocService<LhScheduleResult> implements LhScheduleResultService {

    @Autowired
    private LhScheduleResultEntityMapper lhScheduleResultEntityMapper;

    @Autowired
    private LhUnscheduledResultEntityMapper lhUnscheduledResultEntityMapper;

    @Autowired
    private LhMoldChangePlanEntityMapper lhMoldChangePlanMapper;

    @Autowired
    private MdmProductConstructionEntityMapper productConstructionEntityMapper;

    @Autowired
    private IMdmMaterialInfoService mdmMaterialInfoService;
    @Autowired
    private IMdmDeviceMaintenancePlanService mdmDeviceMaintenancePlanService;
    @Autowired
    private LhScheduleResultHandle lhScheduleResultHandle;
    @Autowired
    private CommonRedisService commonCacheService;

    @Autowired
    private ILhMachineInfoService lhMachineInfoService;
    @Autowired
    private ILhDispatcherLogService lhDispatcherLogServiceImpl;
    @Autowired
    private ILhScheduleResultLogService lhScheduleResultLogService;
    @Autowired
    private IMdmProductConstructionService mdmProductConstructionService;
    @Autowired
    private BaseDao baseDao;
    @Autowired
    private IFactoryMonthPlanProdFinalRemoteService factoryMonthPlanProdFinalRemoteService;
    @Autowired
    private ILhMoldChangePlanService lhMoldChangePlanService;

    @Autowired
    private ILhDayFinishQtyService lhDayFinishQtyService;

    @Autowired
    private ILhParamsService lhParamsService;

    @Autowired
    private LhMonthPlanSurplusEntityMapper lhMonthPlanSurplusEntityMapper;

    @Autowired
    private IProductStockMonthRemoteApiService iProductStockMonthRemoteService;


    @Override
    public String getDocTypeCode() {
        return "OUT2046";
    }

    private static final HashMap<String, Date> statisticsDay = new HashMap<>();
    private static final HashMap<String, String> monthPlanVersion = new HashMap<>();
    /**
     * 查询排程当天可查询机台信息
     * 根据规格代号计算机台今日剩余产能：
     * 剩余可用时间除以硫化时间（匹配机台对应的机械类型），
     * 可用时间取决于已排的规格结束时间、机台维修计划以及当日班次结束时间。
     * 进一步计算每个班次的有效可用时间和产能，且不能超过机台定额。
     * 如果规格结束时间或维修保养时间覆盖了某个班次，该班次的起始和结束时间置为 null，可用量为 0。
     *
     * @param insertParamDTO dto
     * @return List<LhOrderInsertMachineInfoVO>
     */
    @Override
    public List<LhMachineInfoVo> getScheduleMachineInfo(LhOrderInsertParamDTO insertParamDTO) {

        // 根据规格号和物料编码查询物料信息
       /* MdmMaterialInfo mdmMaterialInfo = mdmMaterialInfoService.selectOneByProductCodeAndSpecCode(insertParamDTO.getProductCode(), insertParamDTO.getFactoryCode());
        if (mdmMaterialInfo == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.productInfo.selectOneNotFound"));
        }*/
        return lhScheduleResultHandle.getScheduleMachineInfo(insertParamDTO);
    }

    /**
     * 查询一段时间内，存在于硫化排程的规格列表
     *
     * @param factoryCode
     * @param beginDate
     * @param endDate
     * @param checkSpecCodeList
     * @return
     */
    @Override
    public List<String> selectLhSpecCodeList(String factoryCode, Date beginDate, Date endDate, List<String> checkSpecCodeList) {
        return lhScheduleResultEntityMapper.selectLhSpecCodeList(factoryCode, DateUtils.parseDateToStr("yyyy-MM-dd", beginDate), DateUtils.parseDateToStr("yyyy-MM-dd", endDate), checkSpecCodeList);
    }

    /**
     * 插单
     *
     * @param dto
     * @return
     */
    @Override
    public void insertOrder(LhOrderInsertDTO dto) {
        LhScheduleResult lhScheduleResult = new LhScheduleResult();
        BeanUtils.copyProperties(dto, lhScheduleResult);
        // 根据规格号和物料编码查询物料信息
        MdmMaterialInfo mdmMaterialInfo = mdmMaterialInfoService.selectOneByProductCodeAndSpecCode(lhScheduleResult.getProductCode(), lhScheduleResult.getFactoryCode());
        if (mdmMaterialInfo == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.productInfo.selectOneNotFound"));
        }
        MdmProductConstructionVO constructionVO = getSpecConstruction(lhScheduleResult);
        if (constructionVO == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.construction.selectOneNotFound"));
        }
        lhScheduleResult.setEmbryoCode(constructionVO.getEmbryoCode());
        Map<String, String> paramsMap = lhParamsService.listLhParams(dto.getFactoryCode());
        String trialPrefix = paramsMap.get(LhParamCodeEnums.TRIAL_PRODUCTION_PRE_FIX.getCode());
        if (StringUtils.isNotEmpty(trialPrefix) && StringUtils.isNotEmpty(constructionVO.getConstructionCode())) {
            lhScheduleResult.setIsTrial(trialPrefix.indexOf(constructionVO.getConstructionCode().substring(0, 1)) >= 0 ? ApsConstant.TRUE : ApsConstant.FALSE);
        }
        boolean bSummerSeason = CommonUtils.isSummerSeason(paramsMap.get(LhParamCodeEnums.START_CURING_SUMMER_DAY.getCode()), paramsMap.get(LhParamCodeEnums.START_CURING_WINTER_DAY.getCode()));
        // 3) 机械机台操作时长，用于计算单班硫化量
        String mechanicalMachineOperTime = paramsMap.get(LhParamCodeEnums.MECHANICAL_MACHINE_OPER_TIME.getCode());
        int iMechanicalMachineOperTime = StringUtils.isNotEmpty(mechanicalMachineOperTime) ? Integer.valueOf(mechanicalMachineOperTime) : 0;
        // 4) 液压机台操作时长，用于计算单班硫化量
        String hydraulicMachineOperTime = paramsMap.get(LhParamCodeEnums.HYDRAULIC_MACHINE_OPER_TIME.getCode());
        int iHydraulicMachineOperTime = StringUtils.isNotEmpty(hydraulicMachineOperTime) ? Integer.valueOf(hydraulicMachineOperTime) : 0;

        //查询机台是否存在
        LhMachineInfo lhMachineInfo = lhMachineInfoService.selectOneByMachineCode(lhScheduleResult.getFactoryCode(), lhScheduleResult.getLhMachineCode());
        //根据机台类型取硫化时长
        int curingTime;
        if (MachineTypeEnum.MACHINERY.getCode().equals(lhMachineInfo.getMachineType())) {
            curingTime = bSummerSeason ? constructionVO.getCuringTime() : constructionVO.getCuringTime2();
            curingTime = curingTime + iMechanicalMachineOperTime;
            lhScheduleResult.setLhTime(new BigDecimal(curingTime));
        } else if (MachineTypeEnum.HYDRAULIC_PRESSURE.getCode().equals(lhMachineInfo.getMachineType())) {
            curingTime = bSummerSeason ? constructionVO.getHydraulicPressureCuringTime() : constructionVO.getHydraulicPressureCuringTime2();
            curingTime = curingTime + iHydraulicMachineOperTime;
            lhScheduleResult.setLhTime(new BigDecimal(bSummerSeason ? constructionVO.getHydraulicPressureCuringTime() : constructionVO.getHydraulicPressureCuringTime2()));
        }
        lhScheduleResult.setMachineOrder(lhMachineInfo.getMachineOrder());
        //计算单班硫化量
        calcShiftCapacity(paramsMap, lhScheduleResult);
        //设置数据来源 插单
        lhScheduleResult.setDataSource(ApsConstant.APS_STRING_1);
        //生成工单号
        //硫化自动排程批次号
        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", lhScheduleResult.getScheduleDate());
        String lhOrderNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_ORDER_NO_PREFIX + scheduleDateStr);
        lhScheduleResult.setOrderNo(lhOrderNo);
        // 赋值批次号
        String batchNo = lhScheduleResultEntityMapper.selectBatchNoByScheduleDateAndFactoryCode(lhScheduleResult.getScheduleDate(), lhScheduleResult.getFactoryCode());
        lhScheduleResult.setBatchNo(batchNo);
        //设置规格描述信息
        lhScheduleResult.setSpecDesc(mdmMaterialInfo.getMaterialDesc());
        lhScheduleResult.setProductionStatus(ApsConstant.APS_STRING_0);
        //插入排程操作日志
        insertLhDispatcherLog(dto);
        //插入硫化排程
        lhScheduleResultEntityMapper.insert(lhScheduleResult);
        //删除未排
        deleteLhUnScheduleResult(dto.getFactoryCode(), batchNo, lhScheduleResult.getSpecCode());
    }

    /**
     * 插入排程日志
     *
     * @param dto
     */
    private void insertLhDispatcherLog(LhOrderInsertDTO dto) {
        LhDispatcherLog lhDispatcherLog = new LhDispatcherLog();
        lhDispatcherLog.setFactoryCode(dto.getFactoryCode());
        lhDispatcherLog.setOperType(ApsConstant.APS_STRING_2);
        lhDispatcherLog.setScheduleDate(dto.getScheduleDate());
        lhDispatcherLog.setProductCode(dto.getProductCode());
        lhDispatcherLog.setSpecCode(dto.getSpecCode());
        lhDispatcherLog.setIsDelivery(dto.getIsDelivery());
        lhDispatcherLog.setAfterMachineCode(dto.getLhMachineCode());
        lhDispatcherLog.setAfterClass1Plan(dto.getClass1PlanQty());
        lhDispatcherLog.setAfterClass2Plan(dto.getClass2PlanQty());
        lhDispatcherLog.setAfterClass3Plan(dto.getClass3PlanQty());
        lhDispatcherLog.setAfterClass4Plan(dto.getClass4PlanQty());
        lhDispatcherLog.setAfterClass5Plan(dto.getClass5PlanQty());
        lhDispatcherLog.setAfterClass6Plan(dto.getClass6PlanQty());
        lhDispatcherLog.setBaseVale(null);
        lhDispatcherLogServiceImpl.insert(lhDispatcherLog);
    }

    /**
     * 获取规格施工
     *
     * @param lhScheduleResult
     * @return
     */
    private MdmProductConstructionVO getSpecConstruction(LhScheduleResult lhScheduleResult) {
        List<String> specCodes = new ArrayList<>();
        specCodes.add(lhScheduleResult.getSpecCode());
        List<MdmProductConstructionVO> constructionList = productConstructionEntityMapper.queryByFactoryCodeAndSpecCodes2(lhScheduleResult.getFactoryCode(), specCodes);
        if (PubUtil.isEmpty(constructionList)) {
            return null;
        }
        for (MdmProductConstructionVO constructionVO : constructionList) {
            if (lhScheduleResult.getProductCode().equals(constructionVO.getProductCode())) {
                return constructionVO;
            }
        }
        return null;
    }

    /**
     * 计算单班硫化量
     *
     * @param paramsMap
     * @param lhScheduleResult
     */
    private void calcShiftCapacity(Map<String, String> paramsMap, LhScheduleResult lhScheduleResult) {
        String shiftParam = paramsMap.get(LhParamCodeEnums.CLASS_SYSTEM.getCode());
        int moldNum = 2;
        if (ApsConstant.L_MOLD.equals(lhScheduleResult.getLeftRightMold())
                || ApsConstant.R_MOLD.equals(lhScheduleResult.getLeftRightMold())) {
            moldNum = 1;
        }
        BigDecimal durationSec = (shiftParam == null || ShiftSystemEnum.SHIFT_SYSTEM_2.getCode().equals(Integer.valueOf(shiftParam))) ? BigDecimal.valueOf(43200) : BigDecimal.valueOf(28800);
        // 2) 刷囊时间
        String brushBagTime = paramsMap.get(LhParamCodeEnums.BRUSH_BAG_TIME.getCode());
        int iBrushBagTime = StringUtils.isNotEmpty(brushBagTime) ? Integer.valueOf(brushBagTime) : 0;
        durationSec = BigDecimalUtils.sub(durationSec, iBrushBagTime);
        //BigDecimal lhTime = BigDecimalUtils.add(lhScheduleResult.getLhTime(),iBrushBagTime);
        BigDecimal lhTime = lhScheduleResult.getLhTime();
        int shiftCapacity = CommonUtils.calcPeriodCapacity(durationSec, lhTime, moldNum);
        lhScheduleResult.setSingleMoldShiftLhQty(shiftCapacity);
    }


    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(long[] ids) {
        return lhScheduleResultEntityMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 转机台
     *
     * @param dto
     */
    @Override
    public void changeMachine(LhTransferDeskDTO dto) {
        LhScheduleResult lhScheduleResult = this.selectById(dto.getId());
        String sourceMachineCode = lhScheduleResult.getLhMachineCode();
        LhMachineInfo machineInfo = lhMachineInfoService.selectOneByMachineCode(dto.getFactoryCode(), dto.getLhMachineCode());
        if (machineInfo != null) {
            lhScheduleResult.setMachineOrder(machineInfo.getMachineOrder());
        }
        //进行转机台赋值
        lhScheduleResult.setIsRelease(lhScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        lhScheduleResult.setLhMachineCode(dto.getLhMachineCode());
        lhScheduleResult.setRemark("原始机台：" + sourceMachineCode + ",转入机台：" + dto.getLhMachineCode());
        //插入排程操作日志
        LhDispatcherLog lhDispatcherLog = new LhDispatcherLog();
        lhDispatcherLog.setFactoryCode(dto.getFactoryCode());
        lhDispatcherLog.setScheduleId(lhScheduleResult.getId());
        lhDispatcherLog.setOperType(ApsConstant.APS_STRING_0);
        lhDispatcherLog.setScheduleDate(lhScheduleResult.getScheduleDate());
        lhDispatcherLog.setProductCode(lhScheduleResult.getProductCode());
        lhDispatcherLog.setIsDelivery(lhScheduleResult.getIsDelivery());
        lhDispatcherLog.setSpecCode(lhScheduleResult.getSpecCode());
        lhDispatcherLog.setBeforeMachineCode(sourceMachineCode);
        lhDispatcherLog.setBeforeClass1Plan(lhScheduleResult.getClass1PlanQty());
        lhDispatcherLog.setBeforeClass2Plan(lhScheduleResult.getClass2PlanQty());
        lhDispatcherLog.setBeforeClass3Plan(lhScheduleResult.getClass3PlanQty());
        lhDispatcherLog.setBeforeClass4Plan(lhScheduleResult.getClass4PlanQty());
        lhDispatcherLog.setBeforeClass5Plan(lhScheduleResult.getClass5PlanQty());
        lhDispatcherLog.setBeforeClass6Plan(lhScheduleResult.getClass6PlanQty());
        lhDispatcherLog.setAfterMachineCode(dto.getLhMachineCode());
        lhDispatcherLog.setAfterClass1Plan(lhScheduleResult.getClass1PlanQty());
        lhDispatcherLog.setAfterClass2Plan(lhScheduleResult.getClass2PlanQty());
        lhDispatcherLog.setAfterClass3Plan(lhScheduleResult.getClass3PlanQty());
        lhDispatcherLog.setAfterClass4Plan(lhScheduleResult.getClass4PlanQty());
        lhDispatcherLog.setAfterClass5Plan(lhScheduleResult.getClass5PlanQty());
        lhDispatcherLog.setAfterClass6Plan(lhScheduleResult.getClass6PlanQty());
        lhDispatcherLog.setBaseVale(null);
        lhDispatcherLogServiceImpl.insert(lhDispatcherLog);
        //更新排程
        lhScheduleResultEntityMapper.updateById(lhScheduleResult);
    }

    /**
     * 更新排程结果
     *
     * @param dto
     */
    @Override
    public void updateScheduleResult(LhScheduleResultUpdateDTO dto) {
        //判断是否发布
        LhScheduleResult lhScheduleResult = this.selectById(dto.getId());
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
        lhDispatcherLogServiceImpl.insert(lhDispatcherLog);

        //如果是发布，则调用发布后的调量，如果不是发布，则可以直接更新量
        if (ApsConstant.TRUE.equals(lhScheduleResult.getIsRelease())) {
            //如果是发布 调用发布后的放大
            //todo 调用俊波后续完善的方法
        } else {
            //否的情况直接更改
            LhScheduleResult update = new LhScheduleResult();
            BeanUtils.copyProperties(dto, update);
            lhScheduleResultEntityMapper.updateById(update);
        }
    }

    /**
     * 硫化自动排程
     *
     * @param autoLhScheduleResultDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoLhScheduleResult(AutoLhScheduleResultDTO autoLhScheduleResultDTO) throws BusinessException {
        //1、构建上下文对象  配置参加加载到map中
        AutoLhScheduleResultContextDTO contextDTO = lhScheduleResultHandle.buildLhScheduleResult(autoLhScheduleResultDTO);
        List<LhScheduleResultVo> lhScheduledResultAllFinalVoList = new ArrayList<>();
        List<LhUnscheduledResult> lhUnScheduledAllList = new ArrayList<>();
        //2、需要排2天的量 for循环遍历两次
        contextDTO.getLogDetail().append(String.format("硫化排程开始:T日%s", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, contextDTO.getScheduleTime()))).append(ApsConstant.DIVISION);
        contextDTO.setLhUnscheduledResultList(new ArrayList<>());
        for (int i = 1; i <= ApsConstant.TWO_DAY; i++) {
            //如果i=1说明排第2天，则当前是排T+1日
            if (i == ApsConstant.TWO_DAY) {
                // 排程时间需要加 1 天，如果是月底则自动切换到下月初（Calendar 自动处理跨月情况）
                Calendar cal = Calendar.getInstance();
                cal.setTime(contextDTO.getScheduleTime());
                // 排程日期增加一天
                cal.add(Calendar.DAY_OF_MONTH, 1);
                contextDTO.setScheduleTime(cal.getTime());
                // T日标识更新为 APS_STRING_2
                contextDTO.setTDayFlag(ApsConstant.APS_STRING_2);
                contextDTO.setHadSchedulePlanNum(0);
                // 重置未排计划
                contextDTO.setLhUnscheduledResultList(new ArrayList<>());
                contextDTO.getLogDetail().append(String.format("硫化排程开始:T+1日%s", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, contextDTO.getScheduleTime()))).append(ApsConstant.DIVISION);
            }
            //2.1 初始化硫化排程
            List<FactoryMonthPlanProdFinalVo> currentMonthPlanList = lhScheduleResultHandle.getFactoryMonthPlanProdFinalByDate(contextDTO.getScheduleTime(), autoLhScheduleResultDTO.getFactoryCode());

            if (PubUtil.isEmpty(currentMonthPlanList)) {
                throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.monthlyPlanNotFound"));
            }
            contextDTO.setCurrentMonthPlanList(currentMonthPlanList);
            if (!ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) &&
                    ApsConstant.TRUE.equals(lhScheduleResultHandle.checkMaxDayOfMonth(contextDTO))) {
                //若非T日 且 是最后1天
                continue;
            }

            lhScheduleResultHandle.initLhScheduleResult(autoLhScheduleResultDTO, contextDTO);
            //2.2 规格分组（限制、续作、收尾和新增）
            lhScheduleResultHandle.groupLhScheduleResultByWorkShifts(autoLhScheduleResultDTO, contextDTO);
            //2.3 按照顺序遍历各分组的规格进行机台挑选
            List<LhScheduleResultVo> lhScheduledResultFinalVoList = new ArrayList<>();
            lhScheduleResultHandle.traverseSpecificationsBySort(autoLhScheduleResultDTO, contextDTO, lhScheduledResultFinalVoList);
            //2.4 每日机台补量
            lhScheduleResultHandle.machineReplenishment(contextDTO, lhScheduledResultFinalVoList);
            if (i == ApsConstant.ONE_DAY) {
                lhScheduledResultFinalVoList.forEach(x -> {
                    //置T日规格完成时间，将其保存，因算T+1时，specEndTime最新；
                    x.setTDaySpecEndTime(x.getSpecEndTime());
                });
            }
            contextDTO.setTDayScheduleList(lhScheduledResultFinalVoList);
            if (PubUtil.isNotEmpty(lhScheduledResultFinalVoList)) {
                contextDTO.setTDayAllSpecCodeList(lhScheduledResultFinalVoList.stream().map(x -> x.getSpecCode()).distinct().collect(Collectors.toList()));
            }

            lhScheduledResultAllFinalVoList.addAll(lhScheduledResultFinalVoList);
            if (contextDTO.getLhUnscheduledResultList() == null) {
                contextDTO.setLhUnscheduledResultList(new ArrayList<>());
            }
            lhUnScheduledAllList.addAll(contextDTO.getLhUnscheduledResultList());
            contextDTO.setLogDetail(new StringBuilder());
        }
        //3、在合并之前，设置班次原因-换模
        setClassReasonWithChangeMould(lhScheduledResultAllFinalVoList, contextDTO);
        //4、将T日、T+1日排程结果合并,维度:外胎代码+规格代码+生胎代码+机台+左右模
        List<LhScheduleResult> lhScheduledResultAllFinalList = combineLhScheduleResult(autoLhScheduleResultDTO.getScheduleTime(), contextDTO.getBatchNo(),
                lhScheduledResultAllFinalVoList);
        //获取 原批次号
        String oriBatchNo = lhScheduleResultEntityMapper.selectBatchNoByScheduleDateAndFactoryCode(autoLhScheduleResultDTO.getScheduleTime(), autoLhScheduleResultDTO.getFactoryCode());
        //5、生成未排结果
        if (PubUtil.isNotEmpty(lhUnScheduledAllList)) {
            //根据排程日期查询批次号
            //5.1 删除旧的未排结果
            deleteLhUnScheduleResult(autoLhScheduleResultDTO.getFactoryCode(), oriBatchNo, null);
            //5.2 批量产生未排结果
            baseDao.insertBatch(lhUnScheduledAllList);
        }
        //6、生成排程结果
        if (PubUtil.isNotEmpty(lhScheduledResultAllFinalList)) {
            //6.1 插入到历史记录表中
            List<LhScheduleResultLog> insertLhScheduleResultLogList = new ArrayList<>();
            for (LhScheduleResult lhScheduleResult : lhScheduledResultAllFinalList) {
                LhScheduleResultLog lhScheduleResultLog = new LhScheduleResultLog();
                BeanUtils.copyProperties(lhScheduleResult, lhScheduleResultLog);
                insertLhScheduleResultLogList.add(lhScheduleResultLog);
            }
            lhScheduleResultLogService.insertList(insertLhScheduleResultLogList);
            //6.2 删除旧的排程结果(真删除)
            deleteLhScheduleResult(autoLhScheduleResultDTO.getFactoryCode(), autoLhScheduleResultDTO.getScheduleTime());
            //6.3 批量产生排程结果
            //execMachineOrderFormulas(lhScheduledResultAllFinalList);
            baseDao.insertBatch(lhScheduledResultAllFinalList);
        }
        //7、生成换模计划
        if (CollectionUtils.isNotEmpty(contextDTO.getMoldChangePlanList())) {
            LambdaQueryWrapper<LhMoldChangePlan> unReleasedQuery = new LambdaQueryWrapper<>();
            unReleasedQuery.eq(LhMoldChangePlan::getFactoryCode, autoLhScheduleResultDTO.getFactoryCode())
                    .eq(LhMoldChangePlan::getIsRelease, ApsConstant.NO_RELEASE)
                    .eq(LhMoldChangePlan::getLhResultBatchNo, oriBatchNo);
            lhMoldChangePlanMapper.delete(unReleasedQuery);
            List<LhMoldChangePlan> curMoldChangePlanDateList = contextDTO.getMoldChangePlanList().stream().filter(x -> x.getScheduleDate().equals(autoLhScheduleResultDTO.getScheduleTime())).collect(Collectors.toList());
            lhMoldChangePlanService.insertList(curMoldChangePlanDateList);
        }
        //8、生成自动排程日志
        //createAutoScheduleLog(contextDTO);
    }

    /**
     * 执行机台顺序公式
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
     * 根据机台编号和排程时间查询排程结果
     *
     * @param factoryCode
     * @param machineCode
     * @param scheduleDate
     * @return
     */
    @Override
    public LhScheduleResult getScheduleResultByMachineCodeAndScheduleDate(String factoryCode, String machineCode, Date scheduleDate) {
        LambdaQueryWrapper<LhScheduleResult> query = new LambdaQueryWrapper<>();
        query.eq(LhScheduleResult::getFactoryCode, factoryCode)
                .eq(LhScheduleResult::getLhMachineCode, machineCode)
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        return lhScheduleResultEntityMapper.selectOne(query);
    }

    /**
     * 根据分厂编码查询最后一条排程数据的排程时间
     *
     * @return
     */
    @Override
    public Date selectLastScheduleTime(String factoryCode, Date lastScheduleTime) {
        return lhScheduleResultEntityMapper.selectLastScheduleTime(factoryCode, lastScheduleTime);
    }

    /**
     * 根据分厂编码和排产日期查询批次号
     *
     * @param scheduleDate
     * @param factoryCode
     * @return
     */
    @Override
    public String selectBatchNoByScheduleDateAndFactoryCode(Date scheduleDate, String factoryCode) {
        return lhScheduleResultEntityMapper.selectBatchNoByScheduleDateAndFactoryCode(scheduleDate, factoryCode);
    }

    /**
     * 设置班次原因--换模
     *
     * @param lhScheduledResultAllFinalVoList
     */
    private void setClassReasonWithChangeMould(List<LhScheduleResultVo> lhScheduledResultAllFinalVoList,
                                               AutoLhScheduleResultContextDTO contextDTO) {
        if (PubUtil.isEmpty(lhScheduledResultAllFinalVoList)) {
            return;
        }
        //初始化换模计划集合
        List<LhMoldChangePlan> moldChangePlanList = new ArrayList<>();
        Set<String> moldChangePlanSet = new HashSet<>();
        contextDTO.setMoldChangePlanSet(moldChangePlanSet);
        contextDTO.setMoldChangePlanList(moldChangePlanList);
        //1. 按机台分组
        Map<String, List<LhScheduleResultVo>> machineScheduledMap = lhScheduledResultAllFinalVoList.stream().collect(Collectors.groupingBy(item -> item.getLhMachineCode()));
        for (Map.Entry<String, List<LhScheduleResultVo>> entry : machineScheduledMap.entrySet()) {
            List<LhScheduleResultVo> machineSchedules = entry.getValue();
            //将机台上规格按L/R模分组，双模排产的分别加在两边
            List<LhScheduleResultVo> lMachineScheduleList = new ArrayList<>();
            List<LhScheduleResultVo> rMachineScheduleList = new ArrayList<>();
            for (LhScheduleResultVo resultVo : machineSchedules) {
                if (StringUtils.isEmpty(resultVo.getLeftRightMold())) {
                    lMachineScheduleList.add(resultVo);
                    rMachineScheduleList.add(resultVo);
                } else if (ApsConstant.L_MOLD.equals(resultVo.getLeftRightMold())) {
                    lMachineScheduleList.add(resultVo);
                } else if (ApsConstant.R_MOLD.equals(resultVo.getLeftRightMold())) {
                    rMachineScheduleList.add(resultVo);
                }
            }
            if (PubUtil.isNotEmpty(lMachineScheduleList)) {
                //设置L边的换模
                setOneSideMoldFlag(contextDTO, lMachineScheduleList);
            }
            if (PubUtil.isNotEmpty(rMachineScheduleList)) {
                //设置R边的换模
                setOneSideMoldFlag(contextDTO, rMachineScheduleList);
            }
        }
        // 设置换模次数限制
        setChangeMouldLimit(lhScheduledResultAllFinalVoList,contextDTO);
    }

    /**
     * 设置换模次数限制
     * @param lhScheduledResultAllFinalVoList
     * @param contextDTO
     */
    private void setChangeMouldLimit(List<LhScheduleResultVo> lhScheduledResultAllFinalVoList,
                                     AutoLhScheduleResultContextDTO contextDTO) {
        // 1、判断是否超出换模次数限制
        String changeMouldLimit = contextDTO.getChangeMouldLimit();
        if (StringUtils.isEmpty(changeMouldLimit)
                || CollectionUtils.isEmpty(contextDTO.getMoldChangePlanList())) {
            return;
        }
        if (!BigDecimalUtil.isInteger(changeMouldLimit)) {
            log.warn("[硫化排程] 硫化参数：换模次数限制非整数，跳过");
            return;
        }
        int limit = Integer.parseInt(changeMouldLimit);
        if (contextDTO.getMoldChangePlanList().size() <= limit) {
            log.warn("[硫化排程] 硫化参数：换模次数限制未超出，跳过");
            return;
        }
        // 2、筛选出超出换模次数限制的硫化换模计划
        List<LhMoldChangePlan> lhMoldChangePlanList = new ArrayList<>();
        lhMoldChangePlanList.addAll(contextDTO.getMoldChangePlanList());
        // 先按照硫化机编号升序，再按照换模时间升序
        lhMoldChangePlanList.sort(Comparator.comparing(LhMoldChangePlan::getLhMachineCode)
                .thenComparing(LhMoldChangePlan::getChangeTime));
        // 保留超出限制的硫化换模计划
        List<LhMoldChangePlan> limitLhMoldChangePlanList = lhMoldChangePlanList.stream()
                .skip(Math.max(0, lhMoldChangePlanList.size() - limit + 1))
                .collect(Collectors.toList());
        log.info("[硫化排程] 设置换模次数限制 ==> 限制硫化换模计划列表={}", JSONObject.toJSONString(limitLhMoldChangePlanList));
        // 3、根据硫化机台编号、后规格品号匹配硫化排程结果
        Set<String> keys = limitLhMoldChangePlanList.stream()
                .map(v -> v.getLhMachineCode() + ApsConstant.SPLIT_CHAR + v.getAfterSpecCode())
                .collect(Collectors.toSet());
        List<LhScheduleResultVo> limitScheduleResultList = lhScheduledResultAllFinalVoList.stream()
                .filter(v -> {
            String key = v.getLhMachineCode() + ApsConstant.SPLIT_CHAR + v.getSpecCode();
            return keys.contains(key);
        }).collect(Collectors.toList());
        log.info("[硫化排程] 设置换模次数限制 ==> 限制硫化排程结果列表={}", JSONObject.toJSONString(limitScheduleResultList));
        // 4、设置硫化排程结果各班次计划量为：0，原因分析为：超出换模次数限制
        int workShifts = contextDTO.getWorkShifts();
        String[] classQtyFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassQtyFieldNames();
        String[] classAnalysisFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassAnalysisFieldNames();
        limitScheduleResultList.forEach(v -> {
            for (String fieldName : classQtyFieldNameArr) {
                v.setFieldValueByFieldName(fieldName,0);
            }
            for (String fieldName : classAnalysisFieldNameArr) {
                v.setFieldValueByFieldName(fieldName,ApsConstant.CHANGE_MOULD_LIMIT);
            }
        });
        log.info("[硫化排程] 设置换模次数限制 ==> 最终限制硫化排程结果列表={}", JSONObject.toJSONString(limitScheduleResultList));
    }


    /**
     * 设置单边的模具标志
     *
     * @param contextDTO
     * @param machineSchedules
     */
    private void setOneSideMoldFlag(AutoLhScheduleResultContextDTO contextDTO, List<LhScheduleResultVo> machineSchedules) {
        // 1.1 判断该机台是否存在多个不同规格
        Set<String> specSet = machineSchedules.stream()
                .map(LhScheduleResult::getSpecCode)
                .collect(Collectors.toSet());
        if (specSet.size() <= 1) {
            // 同一规格，无需生成换模计划
            return;
        }

        // 1.2 根据规格结束时间(specEndTime)升序排序：最早的为前规格，后面的为后规格
        machineSchedules.sort(Comparator.comparing(LhScheduleResult::getSpecEndTime));

        // 1.3 遍历相邻两条记录,检查规格结束时间落地班次，即为换模班次
        Date classStartTime, classEndTime;
        LhScheduleResultVo specScheduleResult;
        int workShifts = contextDTO.getWorkShifts();
        String[] classStartTimeFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassStartTimeFieldNames();
        String[] classEndTimeFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassEndTimeFieldNames();
        String[] classAnalysisFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassAnalysisFieldNames();
        for (int i = 0; i < machineSchedules.size() - 1; i++) {
            specScheduleResult = machineSchedules.get(i);
            if (specScheduleResult.getSpecCode().equals(machineSchedules.get(i + 1).getSpecCode())) {
                //续作规格跳过
                continue;
            }

            String reason;
            for (int j = 0; j < classAnalysisFieldNameArr.length; j++) {
                //判断规格结束时间的落地班次
                classStartTime = specScheduleResult.getFieldValueByFieldName(classStartTimeFieldNameArr[j]) != null ? (Date) specScheduleResult.getFieldValueByFieldName(classStartTimeFieldNameArr[j]) : null;
                classEndTime = specScheduleResult.getFieldValueByFieldName(classEndTimeFieldNameArr[j]) != null ? (Date) specScheduleResult.getFieldValueByFieldName(classEndTimeFieldNameArr[j]) : null;
                if (classStartTime != null && classEndTime != null &&
                        specScheduleResult.getSpecEndTime().compareTo(classStartTime) >= 0 &&
                        specScheduleResult.getSpecEndTime().compareTo(classEndTime) <= 0) {
                    reason = ReasonAnalysisEnums.CHANGE_MOULD.getDesc();
                    if (StringUtils.isNotEmpty((String) specScheduleResult.getFieldValueByFieldName(classAnalysisFieldNameArr[j])) && ((String) specScheduleResult.getFieldValueByFieldName(classAnalysisFieldNameArr[j])).indexOf(reason) < 0) {
                        reason = specScheduleResult.getFieldValueByFieldName(classAnalysisFieldNameArr[j]) + "," + reason;
                    }
                    specScheduleResult.setFieldValueByFieldName(classAnalysisFieldNameArr[j], reason);
                    //判断换模key是否存在 机台编号_前规格_后规格
                    String key = specScheduleResult.getLhMachineCode() + "_" + specScheduleResult.getSpecCode() + "_" + machineSchedules.get(i + 1).getSpecCode();
                    //不存在则组装换模对象
                    if (!(contextDTO.getMoldChangePlanSet().contains(key))) {
                        contextDTO.getMoldChangePlanSet().add(key);
                        //组装换模计划对象
                        specScheduleResult.setBatchNo(contextDTO.getBatchNo());
                        LhMoldChangePlan lhMoldChangePlan = lhMoldChangePlanService.genMoldChangePlan(specScheduleResult, machineSchedules.get(i + 1));
                        contextDTO.getMoldChangePlanList().add(lhMoldChangePlan);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 将T日、T+1日排程结果合并
     *
     * @param tDayScheduleTime                T日排程时间
     * @param batchNo                         批次号
     * @param lhScheduledResultAllFinalVoList
     */
    private List<LhScheduleResult> combineLhScheduleResult(Date tDayScheduleTime, String batchNo,
                                                           List<LhScheduleResultVo> lhScheduledResultAllFinalVoList) {
        //硫化自动排程批次号
        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", tDayScheduleTime);
        List<LhScheduleResult> lhScheduledResultAllFinalList = new ArrayList<>();
        lhScheduledResultAllFinalVoList.stream()
                .collect(Collectors.groupingBy(item -> lhCombineKey(item), Collectors.toList()))
                .forEach(
                        (key, lhScheduleResultVoList) -> {
                            //按排程日期排序，优先取T日
                            lhScheduleResultVoList = lhScheduleResultVoList.stream().sorted(Comparator.nullsLast(Comparator.comparing(LhScheduleResultVo::getScheduleDate)))
                                    .collect(Collectors.toList());
                            LhScheduleResult lhScheduleResult = new LhScheduleResult();
                            BeanUtils.copyProperties(lhScheduleResultVoList.get(0), lhScheduleResult);
                            if (lhScheduleResultVoList.size() > 1) {
                                //存在多笔，其实最多2笔，T日和T+1日
                                LhScheduleResultVo nextScheduleResultVo = lhScheduleResultVoList.get(1);
                                lhScheduleResult.setClass4PlanQty(nextScheduleResultVo.getClass4PlanQty());
                                lhScheduleResult.setClass4StartTime(nextScheduleResultVo.getClass4StartTime());
                                lhScheduleResult.setClass4EndTime(nextScheduleResultVo.getClass4EndTime());
                                lhScheduleResult.setClass4Analysis(nextScheduleResultVo.getClass4Analysis());
                                lhScheduleResult.setClass5PlanQty(nextScheduleResultVo.getClass5PlanQty());
                                lhScheduleResult.setClass5StartTime(nextScheduleResultVo.getClass5StartTime());
                                lhScheduleResult.setClass5EndTime(nextScheduleResultVo.getClass5EndTime());
                                lhScheduleResult.setClass5Analysis(nextScheduleResultVo.getClass5Analysis());
                                lhScheduleResult.setClass6PlanQty(nextScheduleResultVo.getClass6PlanQty());
                                lhScheduleResult.setClass6StartTime(nextScheduleResultVo.getClass6StartTime());
                                lhScheduleResult.setClass6EndTime(nextScheduleResultVo.getClass6EndTime());
                                lhScheduleResult.setClass6Analysis(nextScheduleResultVo.getClass6Analysis());
                            }
                            lhScheduleResult.setScheduleDate(tDayScheduleTime);
                            lhScheduleResult.setBatchNo(batchNo);
                            String lhOrderNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_ORDER_NO_PREFIX + scheduleDateStr);
                            lhScheduleResult.setOrderNo(lhOrderNo);
                            //若班次没有计划量，清空 延误补量 的原因
                            cleanClassAnalysisReason(lhScheduleResult);
                            lhScheduledResultAllFinalList.add(lhScheduleResult);
                        }
                );

        return lhScheduledResultAllFinalList;
    }

    /**
     * 若班次没有计划量，清空 延误补量 的原因
     *
     * @param lhScheduleResult
     */
    private void cleanClassAnalysisReason(LhScheduleResult lhScheduleResult) {
        if (lhScheduleResult.getClass1PlanQty() == null && ApsConstant.PLAN_DELAY_AUTO_SUPPLE.equals(lhScheduleResult.getClass1Analysis())) {
            lhScheduleResult.setClass1Analysis(null);
        }
        if (lhScheduleResult.getClass2PlanQty() == null && ApsConstant.PLAN_DELAY_AUTO_SUPPLE.equals(lhScheduleResult.getClass2Analysis())) {
            lhScheduleResult.setClass2Analysis(null);
        }
        if (lhScheduleResult.getClass3PlanQty() == null && ApsConstant.PLAN_DELAY_AUTO_SUPPLE.equals(lhScheduleResult.getClass3Analysis())) {
            lhScheduleResult.setClass3Analysis(null);
        }
        if (lhScheduleResult.getClass4PlanQty() == null && ApsConstant.PLAN_DELAY_AUTO_SUPPLE.equals(lhScheduleResult.getClass4Analysis())) {
            lhScheduleResult.setClass4Analysis(null);
        }
        if (lhScheduleResult.getClass5PlanQty() == null && ApsConstant.PLAN_DELAY_AUTO_SUPPLE.equals(lhScheduleResult.getClass5Analysis())) {
            lhScheduleResult.setClass5Analysis(null);
        }
        if (lhScheduleResult.getClass6PlanQty() == null && ApsConstant.PLAN_DELAY_AUTO_SUPPLE.equals(lhScheduleResult.getClass6Analysis())) {
            lhScheduleResult.setClass6Analysis(null);
        }
    }

    /**
     * 根据日期删除排程结果(真删除)
     *
     * @param scheduleTime
     */
    private void deleteLhScheduleResult(String factoryCode, Date scheduleTime) {
        lhScheduleResultEntityMapper.deleteByFactoryCodeAndScheduleDate(factoryCode, scheduleTime);
    }

    /**
     * 根据批次号删除未排结果
     *
     * @param factoryCode 分厂
     * @param batchNo     批次号
     */
    private void deleteLhUnScheduleResult(String factoryCode, String batchNo, String specCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("FACTORY_CODE", factoryCode);
        params.put("BATCH_NO", batchNo);
        if (StringUtils.isNotEmpty(specCode)) {
            params.put("SPEC_CODE", specCode);
        }
        lhUnscheduledResultEntityMapper.deleteByMap(params);
    }

    /**
     * 硫化排程结果合并维度:外胎代码+规格代码+生胎代码+机台+左右模
     *
     * @param resultVo
     * @return
     */
    private String lhCombineKey(LhScheduleResultVo resultVo) {
        StringBuilder key = new StringBuilder();
        key.append(resultVo.getFactoryCode())
                .append(resultVo.getProductCode())
                .append(resultVo.getSpecCode())
                .append(resultVo.getLhMachineCode())
                .append(resultVo.getEmbryoCode())
                .append(resultVo.getLeftRightMold());
        return key.toString();
    }

    /**
     * 按照日期获取硫化排程计划（成型自动排程使用）
     * 修改需要注意标识和批注
     *
     * @param currentDay  排程日期 （最大可查T+1）
     * @param scheduleLog 日志
     * @return 排程结果列表
     */
    @Override
    public List<LhScheduleResult> getScheduleLhScheduleResults(Date currentDay, StringBuilder scheduleLog) {
        // 记录查询开始
        scheduleLog.append("[硫化排程] 开始处理排程日期：")
                .append(DateUtils.parseDateToStr("yyyy-MM-dd", currentDay))
                .append("\n");

        // 1. 查询当日排程计划
        scheduleLog.append("正在查询当日硫化计划...\n");
        List<LhScheduleResult> dailyTasks = queryDailyTasks(currentDay);

        // 记录查询结果
        scheduleLog.append("查询到当日计划数量：").append(dailyTasks.size()).append(" 条\n");

        if (!dailyTasks.isEmpty()) {
            scheduleLog.append("成功获取当日排程计划，直接返回结果\n");
            return initScheduleResult(dailyTasks);
        }

        // 2. 无数据时触发追溯逻辑
        scheduleLog.append("当日无排程计划，启动追溯机制\n");
        int remainingTraceDays = ApsConstant.ONE_DAY;
        scheduleLog.append("正在追溯最近 ")
                .append(remainingTraceDays)
                .append(" 个工作日的数据（当前版本暂未处理节假日）\n");

        // 3. 执行追溯处理
        scheduleLog.append("开始生成追溯排程结果...\n");
        List<LhScheduleResult> tracedResults = handleEmptyDailyTasks(currentDay, remainingTraceDays, scheduleLog);

        // 记录追溯结果
        scheduleLog.append("追溯生成排程结果数量：")
                .append(tracedResults.size())
                .append(" 条\n");

        return initScheduleResult(tracedResults);
    }

    /**
     * 将硫化计划计划量补0
     */
    private List<LhScheduleResult> initScheduleResult(List<LhScheduleResult> lhScheduleResultList) {

        // 计划量没有值时，需要补0
        if (lhScheduleResultList != null) {
            for (LhScheduleResult lhScheduleResult : lhScheduleResultList) {
                if (lhScheduleResult.getClass1PlanQty() == null) {
                    lhScheduleResult.setClass1PlanQty(0);
                }
                if (lhScheduleResult.getClass2PlanQty() == null) {
                    lhScheduleResult.setClass2PlanQty(0);
                }
                if (lhScheduleResult.getClass3PlanQty() == null) {
                    lhScheduleResult.setClass3PlanQty(0);
                }
                if (lhScheduleResult.getClass4PlanQty() == null) {
                    lhScheduleResult.setClass4PlanQty(0);
                }
                if (lhScheduleResult.getClass5PlanQty() == null) {
                    lhScheduleResult.setClass5PlanQty(0);
                }
                if (lhScheduleResult.getClass6PlanQty() == null) {
                    lhScheduleResult.setClass6PlanQty(0);
                }
            }
        }
        if (lhScheduleResultList != null) {
            return lhScheduleResultList.stream().filter(item -> StringUtils.isNotEmpty(item.getEmbryoCode())).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 查询指定日期排程任务
     */
    private List<LhScheduleResult> queryDailyTasks(Date date) {
        QueryWrapper<LhScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("schedule_date", date);
        return lhScheduleResultEntityMapper.selectList(queryWrapper);
    }

    /**
     * 处理无当日任务的情况
     */
    private List<LhScheduleResult> handleEmptyDailyTasks(Date currentDay, int remainingTraceDays, StringBuilder scheduleLog) {
        log.warn("[硫化排程] 无当日[{}]任务，开始追溯上一次数据...", DateUtils.parseDateToStr("yyyy-MM-dd", currentDay));

        Date previousDay = CxLhEngineUtils.calculateDate(currentDay, -1 * remainingTraceDays);
        log.debug("[硫化排程] 尝试获取上一次[{}]数据...", DateUtils.parseDateToStr("yyyy-MM-dd", previousDay));

        List<LhScheduleResult> previousTask = queryDailyTasks(previousDay);
        if (previousTask.isEmpty()) {
            String errorMsg = String.format("[硫化排程] 连续两日无数据：当前日[%s] 前日[%s], 停止[成型算法排程]",
                    DateUtils.parseDateToStr("yyyy-MM-dd", currentDay), DateUtils.parseDateToStr("yyyy-MM-dd", previousDay));
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 4. 从追溯日生成硫化计划
        List<LhScheduleResult> inheritedTasks = generateInheritedTasks(previousTask, currentDay);

        // 5. 从月度计划补充T+2 新增的规格[只要规格即可]
        return supplementMonthlyPlan(currentDay, inheritedTasks);
    }

    /**
     * 生成继承计划（前日末班次 -> 当日首班次）
     */
    private List<LhScheduleResult> generateInheritedTasks(List<LhScheduleResult> previousTasks, Date currentDay) {
        log.info("[硫化排程] 从上一次硫化计划生成继承计划，原始任务数量：{}", previousTasks.size());
        for (LhScheduleResult task : previousTasks) {
            task.setScheduleDate(currentDay);
            task.setRealScheduleDate(currentDay);
            // 继承前日4/5/6班数据到当日1/2/3班
            task.setClass1PlanQty(task.getClass4PlanQty());
            task.setClass2PlanQty(task.getClass5PlanQty());
            task.setClass3PlanQty(task.getClass6PlanQty());
            // 重置次日计划
            task.setClass4PlanQty(0);
            task.setClass5PlanQty(0);
            task.setClass6PlanQty(0);
        }
        log.info("[硫化排程] 成功从上一次生成继承计划数量：{}", previousTasks.size());
        return previousTasks;
    }

    /**
     * 补充月度计划任务
     */
    private List<LhScheduleResult> supplementMonthlyPlan(Date currentDay, List<LhScheduleResult> existingTasks) {
        log.info("[硫化排程] 开始补充月度计划任务...");
        // 1. 获取月度计划
        FactoryMonthPlanProdFinalQueryDto queryParams = buildMonthQueryParams(currentDay);
        List<FactoryMonthPlanProdFinalVo> monthPlans = factoryMonthPlanProdFinalRemoteService.getProdResult(queryParams);

        // 2. 不存在月度计划【结束】
        if (PubUtil.isEmpty(monthPlans)) {
            String errorKey = queryParams.getCrossMonth() ?
                    "ui.data.column.lhUnScheduleResult.nextMonthlyPlanNotFound" :
                    "ui.data.column.lhUnScheduleResult.monthlyPlanNotFound";
            throw new BusinessException(I18nUtil.getMessage(errorKey));
        }

        // 3. 过滤需要在T+1日生产的计划
        List<LhScheduleResult> newTasks = filterValidMonthPlans(monthPlans, currentDay);
        existingTasks.addAll(newTasks);

        // 4. 初始化T+2日计划[限定成T+2日最大量]
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDay);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        for (LhScheduleResult task : existingTasks) {
            for (FactoryMonthPlanProdFinalVo monthPlan : monthPlans) {
                if ((task.getSpecCode() + task.getProductCode()).equals(monthPlan.getProductCode() + monthPlan.getSpecCode())) {
                    int t2dayPlan = monthPlan.getFieldValueByFieldName("day" + dayOfMonth) == null ? 0 : (int) monthPlan.getFieldValueByFieldName("day" + dayOfMonth);
                    task.setClass4PlanQty(t2dayPlan);
                }
            }
        }

        // 5. 合并结果
        log.info("[硫化排程] 最终任务数量：继承{}条 + 新增{}条 = 总计{}条",
                existingTasks.size(), newTasks.size(), existingTasks.size());
        return existingTasks;
    }

    /**
     * 过滤有效月度计划
     */
    private List<LhScheduleResult> filterValidMonthPlans(List<FactoryMonthPlanProdFinalVo> plans, Date currentDay) {
        return new ArrayList<>();
    }

    /**
     * 构建月度查询参数（包含跨月处理）
     */
    public FactoryMonthPlanProdFinalQueryDto buildMonthQueryParams(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        FactoryMonthPlanProdFinalQueryDto dto = new FactoryMonthPlanProdFinalQueryDto();
        dto.setFactoryCode(SecurityUtils.getUserCurrentFactory());
        dto.setYear(cal.get(Calendar.YEAR));
        dto.setMonth(cal.get(Calendar.MONTH) + 1);
        dto.setCrossMonth(Boolean.FALSE);

        // 处理月末跨月情况
        if (cal.get(Calendar.DAY_OF_MONTH) == cal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            log.info("[硫化排程] 检测到月末，自动切换下个月查询");
            cal.add(Calendar.MONTH, 1);
            dto.setYear(cal.get(Calendar.YEAR));
            dto.setMonth(cal.get(Calendar.MONTH) + 1);
            dto.setCrossMonth(Boolean.TRUE);
        }
        return dto;
    }

    /**
     * 初始化无排程任务
     *
     * @param plan 月度计划
     * @param id   任务ID
     * @return 硫化排程对象
     */
    private LhScheduleResult initializeT1scheduledTask(FactoryMonthPlanProdFinalVo plan, long id) {
        LhScheduleResult task = new LhScheduleResult();
        task.setId(id);
        task.setRemark("T+2月度计划预排");
        task.setBatchNo("预排任务无批次号");
        task.setDailyPlanQty(0);
        task.setSpecDesc(plan.getSpecifications());
        task.setLhMachineCode("预排任务无硫化机台");
        task.setIsDelivery(String.valueOf(plan.getIsDeliveryDate() == null ? 0 : 1));
        task.setFactoryCode(plan.getFactoryCode());
        task.setProductCode(plan.getProductCode());
        task.setSpecCode(plan.getSpecCode());
        task.setMpMoldQty(plan.getMouldQty());
        task.setMoldQty(plan.getMouldQty());
        // 设置任务1/2/3 班计划量 = 0
        task.setClass1PlanQty(0);
        task.setClass2PlanQty(0);
        task.setClass3PlanQty(0);
        // 设置任务4/5/6 班计划量 = 1
        task.setClass4PlanQty(0);
        task.setClass5PlanQty(0);
        task.setClass6PlanQty(0);
        // 标识为月度计划来源
        task.setDataSource("0");
        return task;
    }

    /**
     * 获取T日的排程结果
     *
     * @return
     */
    @Override
    public List<LhScheduleResultVo> getTDayLhScheduleResult(Date scheduleTime, String factoryCode) {
        List<LhScheduleResultVo> VOList = new ArrayList<>();
        //构造T-1日查询条件
        LambdaQueryWrapper<LhScheduleResult> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(LhScheduleResult::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(StringUtils.isNotEmpty(factoryCode), LhScheduleResult::getFactoryCode, factoryCode);
        wrapper.eq(scheduleTime != null, LhScheduleResult::getScheduleDate, scheduleTime);
        //后续是否需要过滤只查询已发布的数据呢
        List<LhScheduleResult> lhScheduleResults = lhScheduleResultEntityMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(lhScheduleResults)) {
            for (LhScheduleResult lhScheduleResult : lhScheduleResults) {
                LhScheduleResultVo vo = new LhScheduleResultVo();
                BeanUtils.copyProperties(lhScheduleResult, vo);
                VOList.add(vo);
            }
        }
        return VOList;
    }

    /**
     * 获取上一天的排程结果
     *
     * @return
     */
    @Override
    public List<LhScheduleResultVo> getLastDayLhScheduleResult(Date scheduleTime, String factoryCode, Integer traceDays) {
        //往前推的开始天数
        Date startDate = DateUtils.addDays(scheduleTime, -traceDays);
        List<LhScheduleResultVo> VOList = new ArrayList<>();
        //构造T-1日查询条件
        LambdaQueryWrapper<LhScheduleResult> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(LhScheduleResult::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(StringUtils.isNotEmpty(factoryCode), LhScheduleResult::getFactoryCode, factoryCode);
        wrapper.between(LhScheduleResult::getScheduleDate, startDate, scheduleTime);
        wrapper.last(" AND STR_TO_DATE(SCHEDULE_DATE, '%Y-%m-%d' ) = STR_TO_DATE(REAL_SCHEDULE_DATE, '%Y-%m-%d' ) ORDER BY MACHINE_ORDER ASC " );
        //后续是否需要过滤只查询已发布的数据呢
        List<LhScheduleResult> lhScheduleResults = lhScheduleResultEntityMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(lhScheduleResults)) {
            for (LhScheduleResult lhScheduleResult : lhScheduleResults) {
                LhScheduleResultVo vo = new LhScheduleResultVo();
                BeanUtils.copyProperties(lhScheduleResult, vo);
                VOList.add(vo);
            }
        }
        return VOList;
    }


    /**
     * 查询List
     *
     * @param lhScheduleResult
     * @return
     */
    @Override
    public List<LhScheduleResult> selectList(QueryWrapper<LhScheduleResult> lhScheduleResult) {
        return lhScheduleResultEntityMapper.selectList(lhScheduleResult);
    }

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    @Override
    public LhScheduleResult selectById(Long id) {
        return lhScheduleResultEntityMapper.selectById(id);
    }

    /**
     * 导入数据
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    @Override
    public AjaxResult importData(List<LhScheduleResult> list, boolean updateSupport, Long importLogId) {
        //0.初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhScheduleResult> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        //1.进行非空校验,Excel中数据重复校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhScheduleResult docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        //2.进行数据库唯一性校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhScheduleResult docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else {
                //todo 如果是存在则更新,则需要自行实现
                failureNum++;
                //数据库已经存在,不允许插入
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (PubUtil.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = baseDao.saveBatch(importList);

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 导入数据2
     *
     * @param list         导入集合
     * @param importLogId  日志ID
     * @param scheduleDate 导入日期
     * @return 返回结果
     */
    @Override
    public AjaxResult importData2(List<LhScheduleResult> list, Long importLogId, Date scheduleDate) {
        //0.初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhScheduleResult> importList = new ArrayList<>();
        List<LhScheduleResult> updateList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        //对所有list设置排程日期，让下面可以做唯一校验
        list.forEach(item -> item.setScheduleDate(scheduleDate));
        //1.进行非空校验,Excel中数据重复校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhScheduleResult docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", scheduleDate);
        List<LhScheduleResult> dbLhScheduleResultList = getScheduleResultList(scheduleDate);
        String batchNo = PubUtil.isNotEmpty(dbLhScheduleResultList) ? dbLhScheduleResultList.get(0).getBatchNo() : "";
        // 赋值批次号
        //String batchNo = lhScheduleResultEntityMapper.selectBatchNoByScheduleDateAndFactoryCode(scheduleDate, list.get(0).getFactoryCode());
        if (StringUtils.isEmpty(batchNo)) {
            batchNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_BATCH_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_BATCH_NO_PREFIX + scheduleDateStr);
        }

        Map<String, LhScheduleResult> dbScheduleResultMap = new HashMap<>();
        if (PubUtil.isNotEmpty(dbLhScheduleResultList)) {
            //5.1 删除旧的未排结果
            deleteLhUnScheduleResult(list.get(0).getFactoryCode(), batchNo, null);
            //6.2 删除旧的排程结果(真删除)
            deleteLhScheduleResult(list.get(0).getFactoryCode(), scheduleDate);

            for (LhScheduleResult scheduleResult : dbLhScheduleResultList) {
                dbScheduleResultMap.put(scheduleResult.getLhMachineCode() + scheduleResult.getSpecCode() + scheduleResult.getLeftRightMold(), scheduleResult);
            }
        }

        //2.进行数据库唯一性校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhScheduleResult docEntity = list.get(i);
            //赋值硫化时间
            docEntity.setScheduleDate(scheduleDate);
            docEntity.setRealScheduleDate(scheduleDate);
            docEntity.setBatchNo(batchNo);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            docEntity.setMoldQty(StringUtils.isEmpty(docEntity.getLeftRightMold()) ? 2 : 1);
            //拆分左右模
            List<LhScheduleResult> splitList = new ArrayList<>();
            splitLeftRightMold(docEntity, splitList);
            LhScheduleResult dbScheduleResult;
            for (LhScheduleResult scheduleResult : splitList) {
                dbScheduleResult = dbScheduleResultMap.get(scheduleResult.getLhMachineCode() + scheduleResult.getSpecCode() + scheduleResult.getLeftRightMold());
                if (dbScheduleResult == null) {
                    scheduleResult.setRowState(RowStateEnum.ADDED);
                    String lhOrderNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_ORDER_NO_PREFIX + scheduleDateStr);
                    scheduleResult.setOrderNo(lhOrderNo);
                    importList.add(scheduleResult);
                } else {
                    //scheduleResult.setId(dbScheduleResult.getId());
                    //BeanUtils.copyProperties(scheduleResult,dbScheduleResult);
                    copyToDbScheduleResult(scheduleResult, dbScheduleResult);
                    dbScheduleResult.setRowState(RowStateEnum.MODIFIED);
                    importList.add(dbScheduleResult);
                }
               /* if (checkUnique(scheduleResult).equals(UserConstants.UNIQUE)) {
                    scheduleResult.setRowState(RowStateEnum.ADDED);
                    importList.add(scheduleResult);
                } else {
                    //如果是存在则更新,则需要自行实现
                    LhScheduleResult oneLhScheduleResult = this.getOneLhScheduleResult(scheduleResult);
                    if(oneLhScheduleResult != null){
                        scheduleResult.setId(oneLhScheduleResult.getId());
                        BeanUtils.copyProperties(scheduleResult,oneLhScheduleResult);
                        importList.add(oneLhScheduleResult);
                    }
                }*/
            }
        }

        if (CollectionUtils.isNotEmpty(importList)) {
            //更新施工信息
            updateConstructionInfo(list.get(0).getFactoryCode(), importList);

            successNum = baseDao.saveBatch(importList);
            // 更新日完成量，完成量汇总
            lhDayFinishQtyService.updateMonthPlanSurplus(list.get(0).getFactoryCode(), scheduleDate);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }



    @Override
    public void copyToDbScheduleResult(LhScheduleResult srcScheduleResult, LhScheduleResult destScheduleResult) {
        if (srcScheduleResult == null || destScheduleResult == null) {
            return;
        }
        destScheduleResult.setEmbryoCode(srcScheduleResult.getEmbryoCode());
        destScheduleResult.setClass1PlanQty(srcScheduleResult.getClass1PlanQty());
        destScheduleResult.setClass1FinishQty(srcScheduleResult.getClass1FinishQty());
        destScheduleResult.setClass1Analysis(srcScheduleResult.getClass1Analysis());
        destScheduleResult.setClass2PlanQty(srcScheduleResult.getClass2PlanQty());
        destScheduleResult.setClass2FinishQty(srcScheduleResult.getClass2FinishQty());
        destScheduleResult.setClass2Analysis(srcScheduleResult.getClass2Analysis());
    }

    /**
     * 更新施工相关信息
     *
     * @param factoryCode
     * @param importList
     */
    @Override
    public void updateConstructionInfo(String factoryCode, List<LhScheduleResult> importList) {
        //1. 获取施工信息
        List<String> specCodes = importList.stream().map(x -> x.getSpecCode()).collect(Collectors.toList());
        List<MdmProductConstructionVO> constructionList = productConstructionEntityMapper.queryByFactoryCodeAndSpecCodes2(factoryCode, specCodes);
        Map<String, MdmProductConstructionVO> specToConstructionMap = new HashMap<>();
        for (MdmProductConstructionVO cons : constructionList) {
            specToConstructionMap.put(cons.getSpecCode(), cons);
        }

        //2. 获取冬季、夏季信息
        Map<String, String> paramsMap = lhParamsService.listLhParams(factoryCode);
        boolean bSummerSeason = CommonUtils.isSummerSeason(paramsMap.get(LhParamCodeEnums.START_CURING_SUMMER_DAY.getCode()), paramsMap.get(LhParamCodeEnums.START_CURING_WINTER_DAY.getCode()));
        String trialPrefix = paramsMap.get(LhParamCodeEnums.TRIAL_PRODUCTION_PRE_FIX.getCode());
        // 3) 机械机台操作时长，用于计算单班硫化量
        String mechanicalMachineOperTime = paramsMap.get(LhParamCodeEnums.MECHANICAL_MACHINE_OPER_TIME.getCode());
        int iMechanicalMachineOperTime = StringUtils.isNotEmpty(mechanicalMachineOperTime) ? Integer.valueOf(mechanicalMachineOperTime) : 0;
        // 4) 液压机台操作时长，用于计算单班硫化量
        String hydraulicMachineOperTime = paramsMap.get(LhParamCodeEnums.HYDRAULIC_MACHINE_OPER_TIME.getCode());
        int iHydraulicMachineOperTime = StringUtils.isNotEmpty(hydraulicMachineOperTime) ? Integer.valueOf(hydraulicMachineOperTime) : 0;

        //3. 获取机台信息
        LhMachineInfo queryLhMachineInfo = new LhMachineInfo();
        queryLhMachineInfo.setFactoryCode(factoryCode);
        List<LhMachineInfo> allMachineList = lhMachineInfoService.selectList(queryLhMachineInfo);
        Map<String, List<LhMachineInfo>> machineMap = allMachineList.stream().collect(Collectors.groupingBy(item -> item.getMachineCode()));
        LhMachineInfo machineInfo;
        for (LhScheduleResult lhScheduleResult : importList) {
            if (PubUtil.isEmpty(machineMap.get(lhScheduleResult.getLhMachineCode()))) {
                continue;
            }
            machineInfo = machineMap.get(lhScheduleResult.getLhMachineCode()).get(0);
            lhScheduleResult.setMachineOrder(machineInfo.getMachineOrder());

            // 获取规格对应的施工
            MdmProductConstructionVO cons = specToConstructionMap.get(lhScheduleResult.getSpecCode());
            if (cons != null) {
                if (StringUtils.isNotEmpty(trialPrefix) && StringUtils.isNotEmpty(cons.getConstructionCode())) {
                    lhScheduleResult.setIsTrial(trialPrefix.indexOf(cons.getConstructionCode().substring(0, 1)) >= 0 ? ApsConstant.TRUE : ApsConstant.FALSE);
                }
                lhScheduleResult.setBomVersion(cons.getBomVersion());
                lhScheduleResult.setMouldMethod(cons.getMouldMethod());
                lhScheduleResult.setProductCode(cons.getProductCode());

                //根据机台类型取硫化时长
                Integer curingTime = 0;
                if (MachineTypeEnum.MACHINERY.getCode().equals(machineInfo.getMachineType())) {
                    curingTime = bSummerSeason ? cons.getCuringTime() : cons.getCuringTime2();
                    curingTime = curingTime != null ? curingTime : 0;
                    curingTime = curingTime + iMechanicalMachineOperTime;
                    lhScheduleResult.setLhTime(new BigDecimal(curingTime));
                } else if (MachineTypeEnum.HYDRAULIC_PRESSURE.getCode().equals(machineInfo.getMachineType())) {
                    curingTime = bSummerSeason ? cons.getHydraulicPressureCuringTime() : cons.getHydraulicPressureCuringTime2();
                    curingTime = curingTime != null ? curingTime : 0;
                    curingTime = curingTime + iHydraulicMachineOperTime;
                    lhScheduleResult.setLhTime(new BigDecimal(curingTime));
                }
                //计算单班硫化量
                calcShiftCapacity(paramsMap, lhScheduleResult);

                /*if (StringUtils.isEmpty(lhScheduleResult.getProductCode())){
                    lhScheduleResult.setProductCode(cons.getProductCode());
                }*/
                //重算日计划量
                int dailyQty = (lhScheduleResult.getClass1PlanQty() != null ? lhScheduleResult.getClass1PlanQty() : 0) +
                        (lhScheduleResult.getClass2PlanQty() != null ? lhScheduleResult.getClass2PlanQty() : 0) +
                        (lhScheduleResult.getClass3PlanQty() != null ? lhScheduleResult.getClass3PlanQty() : 0);
                lhScheduleResult.setDailyPlanQty(dailyQty == 0 ? null : dailyQty);
            }
        }
    }


    /**
     * 拆分左右模
     *
     * @param scheduleResult
     * @param splitList
     */
    @Override
    public void splitLeftRightMold(LhScheduleResult scheduleResult, List<LhScheduleResult> splitList) {
        if (scheduleResult.getSpecCode().indexOf("*") < 0) {
            splitList.add(scheduleResult);
            return;
        }
//        String[] split = scheduleResult.getSpecCode().split("\\*");
        String[] specArr = new String[]{scheduleResult.getSpecCode().substring(0,4),scheduleResult.getSpecCode().substring(5,9)};
        String[] embryoArr,productArr,specDescArr;
        if (scheduleResult.getEmbryoCode().indexOf("*")>=0){
            embryoArr = new String[]{scheduleResult.getEmbryoCode().substring(0,4),scheduleResult.getEmbryoCode().substring(5,9)};
        }else{
            embryoArr = new String[]{scheduleResult.getEmbryoCode()};
        };
        if (scheduleResult.getProductCode().indexOf("*") >= 0) {
            productArr = new String[]{scheduleResult.getProductCode().substring(0, 8), scheduleResult.getProductCode().substring(9, 17)};
        } else {
            productArr = new String[]{scheduleResult.getProductCode()};
        };
        int xinIndex = scheduleResult.getSpecDesc().indexOf("*");
        int totalLen = scheduleResult.getSpecDesc().length();
        if (xinIndex >=0){
            specDescArr = new String[]{scheduleResult.getSpecDesc().substring(0,xinIndex),scheduleResult.getSpecDesc().substring(xinIndex+1,totalLen)};
        }else{
            specDescArr = new String[]{scheduleResult.getSpecDesc()};
        };
        scheduleResult.setMoldQty(2);
        for (int i = 0; i < specArr.length; i++) {
            LhScheduleResult newScheduleResult = new LhScheduleResult();
            BeanUtils.copyProperties(scheduleResult, newScheduleResult);
            newScheduleResult.setSpecCode(specArr[i]);
            if (embryoArr.length == 2) {
                newScheduleResult.setEmbryoCode(embryoArr[i]);
            }
            if (productArr.length == 2) {
                newScheduleResult.setProductCode(productArr[i]);
            }
            if (specDescArr.length ==2){
                newScheduleResult.setSpecDesc(specDescArr[i]);
            }
            if (newScheduleResult.getClass1PlanQty() != null){
                newScheduleResult.setClass1PlanQty(newScheduleResult.getClass1PlanQty()/2);
            }
            if (newScheduleResult.getClass2PlanQty() != null) {
                newScheduleResult.setClass2PlanQty(newScheduleResult.getClass2PlanQty() / 2);
            }
            if (newScheduleResult.getClass1FinishQty() != null) {
                newScheduleResult.setClass1FinishQty(newScheduleResult.getClass1FinishQty() / 2);
            }
            if (newScheduleResult.getClass2FinishQty() != null) {
                newScheduleResult.setClass2FinishQty(newScheduleResult.getClass2FinishQty() / 2);
            }
            if (newScheduleResult.getDailyPlanQty() != null) {
                newScheduleResult.setDailyPlanQty(newScheduleResult.getDailyPlanQty() / 2);
            }
            if (i == 0) {
                newScheduleResult.setLeftRightMold("L");
            } else {
                newScheduleResult.setLeftRightMold("R");
            }
            scheduleResult.setMoldQty(1);
            splitList.add(newScheduleResult);
        }
    }

    @Override
    public List<LhScheduleResult> getScheduleResultList(Date scheduleDate) {
        QueryWrapper<LhScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("schedule_date", scheduleDate);
        return lhScheduleResultEntityMapper.selectList(queryWrapper);
    }

    /**
     * 唯一性校验
     *
     * @param docEntityVO
     * @return
     */
    @Override
    public String checkUnique(LhScheduleResult docEntityVO) {
        // 唯一性判断依据: 根据业务修改
        QueryWrapper<LhScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getFieldValueByFieldName("id")), "ID", docEntityVO.getFieldValueByFieldName("id"));
        //校验维度 1.排程日期 硫化机台编号 物料号 规格号 是否交期
        queryWrapper.eq("schedule_date", docEntityVO.getScheduleDate());
        queryWrapper.eq("LH_MACHINE_CODE", docEntityVO.getLhMachineCode());
        //queryWrapper.eq("PRODUCT_CODE", docEntityVO.getProductCode());
        queryWrapper.eq("spec_code", docEntityVO.getSpecCode());
        //queryWrapper.eq("IS_DELIVERY", docEntityVO.getIsDelivery());

        if (lhScheduleResultEntityMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }


    private LhScheduleResult getOneLhScheduleResult(LhScheduleResult docEntityVO) {
        QueryWrapper<LhScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getFieldValueByFieldName("id")), "ID", docEntityVO.getFieldValueByFieldName("id"));
        //校验维度 1.排程日期 硫化机台编号 物料号 规格号 是否交期
        queryWrapper.eq("schedule_date", docEntityVO.getScheduleDate());
        queryWrapper.eq("LH_MACHINE_CODE", docEntityVO.getLhMachineCode());
        //queryWrapper.eq("PRODUCT_CODE", docEntityVO.getProductCode());
        queryWrapper.eq("spec_code", docEntityVO.getSpecCode());
        //queryWrapper.eq("IS_DELIVERY", docEntityVO.getIsDelivery());
        List<LhScheduleResult> resList = lhScheduleResultEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(resList)) {
            return resList.get(0);
        }
        return null;
    }

    /**
     * 排程发布
     *
     * @param ids
     * @param scheduleDate 排程日期
     * @param dataVersion  接口数据版本
     * @param factoryCode  分厂代号
     * @param companyCode  分公司代号
     */
    @Override
    public AjaxResult publish(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode) {
        //数据同步
        lhScheduleResultEntityMapper.deployScheduleToMes(dataVersion, ids, factoryCode, companyCode);

        //保存发布记录，更新发布状态
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_LH);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        //Joran 2022-03-09记录发布对应的数据版本号
        record.setDataVersion(dataVersion);
        lhScheduleResultEntityMapper.insertPublishRecord(record);
        lhScheduleResultEntityMapper.batchUpdate(ArrayUtils.toObject(ids), ApsConstant.RELEASING);
        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));

    }

    /**
     * 更新指定相关数据记录的发布状态
     *
     * @param dataVersion 数据版本
     * @param ids         排程ID列表
     * @param status      更新的状态
     */
    @Override
    public void updateReleaseStatus(String dataVersion, long[] ids, String status) {
        lhScheduleResultEntityMapper.batchUpdate(ArrayUtils.toObject(ids), status);
        lhScheduleResultEntityMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_LH);
        record.setScheduleDate(scheduleDate);
        return lhScheduleResultEntityMapper.isPublish(record) > 0;
    }

    /**
     * 查询硫化机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @Override
    public AjaxResult selectMachineGantt(LhGanttVo queryVO) {
        List<LhGanttVo> newGanteList = new ArrayList<>();
        List<LhGanttVo> lhScheduleResultList = lhScheduleResultEntityMapper.getLhGanttData(queryVO);
        if (CollectionUtils.isNotEmpty(lhScheduleResultList)) {
            for (LhGanttVo lhGanttVo : lhScheduleResultList) {
                //构造开始日、结束日、开始时刻、结束时刻、起点位置、时差;
                String scheduleDay = DateUtils.getDay(lhGanttVo.getScheduleDate()) + "";
                String startDay = DateUtils.getDay(lhGanttVo.getStartDate()) + "";
                String endDay = DateUtils.getDay(lhGanttVo.getEndDate()) + "";
                int startHours = DateUtils.getHour(lhGanttVo.getStartDate());
                int endHours = DateUtils.getHour(lhGanttVo.getEndDate());
                int dayInterval = DateUtils.getDayInterval(lhGanttVo.getEndDate(), lhGanttVo.getStartDate());
                int dayInterval2 = DateUtils.getDayInterval(lhGanttVo.getScheduleDate(), lhGanttVo.getStartDate());

                //计算以下三个值，用户画甘特图
                //算起点位置：后端给24小时制的起始时刻
                //算长条宽度：小时差*25：(endHour-startHour+1)*25，后端给时差;
                //算margin-left宽度：固定值*天数，不用后端给

                if (dayInterval2 > 0) {
                    //起始日期在排程日期前
                    lhGanttVo.setHourStart(startHours);
                } else if (dayInterval2 == 0) {
                    //起始日期就是排程日期
                    lhGanttVo.setHourStart(startHours + 24);
                } else {
                    //起始日期在排程日期后
                    lhGanttVo.setHourStart(startHours + 48);
                }

                //跨天存在前一天数据
                if (!startDay.equals(endDay) && scheduleDay.equals(endDay)) {
                    lhGanttVo.setHourInterval(24 - startHours + endHours);
                    //跨多天
                    if (dayInterval > 1) {
                        lhGanttVo.setHourInterval(24 - startHours + 24 * (dayInterval - 1) + endHours);
                    }
                } else if (!startDay.equals(endDay)) {
                    lhGanttVo.setHourInterval(24 - startHours + endHours);
                    //跨多天
                    if (dayInterval > 1) {
                        lhGanttVo.setHourInterval(24 - startHours + 24 * (dayInterval - 1) + endHours);
                    }
                } else {
                    lhGanttVo.setHourInterval(endHours - startHours);
                }

                lhGanttVo.setStartDay(startDay);
                lhGanttVo.setEndDay(endDay);
                lhGanttVo.setStartHour(startHours + "");
                lhGanttVo.setEndHour(endHours + "");
                newGanteList.add(lhGanttVo);
            }
        }
        return AjaxResult.success(newGanteList);
    }

    @Override
    public int saveBatchByImport(List<LhScheduleResult> importList) {
        return baseDao.saveBatch(importList);
    }

    @Override
    public List<SpecCodeAndProductCodeVO> getConstructionList(String factoryCode, List<String> specCodes){
        return  productConstructionEntityMapper.queryBySpecCodeAndProductCode(factoryCode, specCodes);
    }


    @Override
    public List<LhMonthFinishQtyVo> monthFinishQtyList(LhMonthPlanSurplusDetail queryVO) {
        //获取月度计划
        List<FactoryMonthPlanProdFinalVo> currentMonthPlanList = lhScheduleResultHandle.getFactoryMonthPlanProdFinalByDate(queryVO.getScheduleTime(), queryVO.getFactoryCode());
        if (PubUtil.isEmpty(currentMonthPlanList)) {
            return Collections.emptyList();
        }
        FactoryMonthPlanProdFinalVo factoryMonthPlanProdFinalVo = currentMonthPlanList.get(0);
        //根据月度计划获取硫化计划
        List<LhScheduleResultTotalVo> lhScheduleResults = lhScheduleResultEntityMapper.selectLhScheduleResultTotal(factoryMonthPlanProdFinalVo);
        Map<String, List<LhScheduleResultTotalVo>> lhScheduledMap = lhScheduleResults.stream()
                .filter(item -> StringUtil.isNotEmpty(item.getSpecCode()) && StringUtil.isNotEmpty(item.getProductCode()))
                .collect(Collectors.groupingBy(item -> item.getSpecCode()+"-"+item.getProductCode()));
        // 获取月度开始跟结束时间
        LocalDate startDate = factoryMonthPlanProdFinalVo.getProductionStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = factoryMonthPlanProdFinalVo.getProductionEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("起始日期不能晚于结束日期");
        }
        // 当前月的每一天
        List<LocalDate> dateRange = getDateRange(startDate, endDate);

        // 全部月计划硫化剩余量
        LambdaQueryWrapper<LhMonthPlanSurplus> surplus = new LambdaQueryWrapper<>();
        surplus.eq(LhMonthPlanSurplus::getFactoryCode, factoryMonthPlanProdFinalVo.getFactoryCode())
                .eq(LhMonthPlanSurplus::getMonth, factoryMonthPlanProdFinalVo.getMonth())
                .eq(LhMonthPlanSurplus::getYear, factoryMonthPlanProdFinalVo.getYear())
                .eq(LhMonthPlanSurplus::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<LhMonthPlanSurplus> lhMonthPlanSurpluses = lhMonthPlanSurplusEntityMapper.selectList(surplus);
        Map<String, List<LhMonthPlanSurplus>> surplusesdMap = lhMonthPlanSurpluses.stream()
                .filter(item -> StringUtil.isNotEmpty(item.getSpecCode()) && StringUtil.isNotEmpty(item.getProductCode()))
                .collect(Collectors.groupingBy(item -> item.getSpecCode()+"-"+item.getProductCode()));
        // 获取规格匹配的spa跟施工关系
        List<MdmProductConstruction> mdmProductConstructionVOS = mdmProductConstructionService.selectListByFactoryCodeAndSpecCode(queryVO.getFactoryCode(), lhMonthPlanSurpluses.stream().map(LhMonthPlanSurplus::getSpecCode).distinct().collect(Collectors.toList()));

        // 查询月度库存
        ProductStockMonth stockParams = new ProductStockMonth();
        stockParams.setIsDelete(Integer.valueOf(ApsConstant.DEL_FLAG_NORMAL));
        stockParams.setYear(DateUtils.getYear(queryVO.getScheduleTime()));
        stockParams.setMonth(DateUtils.getMonth(queryVO.getScheduleTime()) - 1);
        stockParams.setFactoryCode(queryVO.getFactoryCode());
        List<ProductStockMonth> productStockMonthList = iProductStockMonthRemoteService.selectList(stockParams);
        log.debug("monthFinishQtyList ==> productStockMonthList={}",JSONObject.toJSONString(productStockMonthList));
        Map<String, Integer> stockMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(productStockMonthList)) {
            stockMap = productStockMonthList.stream().collect(Collectors.toMap(ProductStockMonth::getProductCode, ProductStockMonth::getStockQty,(entity1, entity2) -> entity1));
        }
        Map<String, Integer> finalStockMap = stockMap;

        // 查询可用模具
        Map<String, Object> mouldStatusParams = new HashMap<>(16);
        mouldStatusParams.put("YEAR", DateUtils.getYear(queryVO.getScheduleTime()));
        mouldStatusParams.put("MONTH", DateUtils.getMonth(queryVO.getScheduleTime()));
        mouldStatusParams.put("FACTORY_CODE", queryVO.getFactoryCode());
        List<MdmMouldUseStatus> mdmMouldUseStatusList = baseDao.selectByMap(MdmMouldUseStatus.class, mouldStatusParams);
        Map<String, Long> mouldStatusMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(mdmMouldUseStatusList)) {
            mouldStatusMap = mdmMouldUseStatusList.stream().collect(Collectors.groupingBy(
                    item -> {
                        if (StringUtils.isBlank(item.getMouldCode())) {
                            return "";
                        } else if (item.getMouldCode().contains("-")) {
                            return item.getMouldCode().split("-")[1];
                        } else {
                            return item.getMouldCode().substring(item.getMouldCode().length() - 5, item.getMouldCode().length() - 1);
                        }
                    },
                    Collectors.counting()));
        }
        Map<String, Long> finalMouldStatusMap = mouldStatusMap;



        //全部月计划硫化剩余量 循环 每个规格
        List<LhMonthFinishQtyVo> list = new ArrayList<>();
        lhScheduledMap.forEach((item,values) -> {
            String specCode = item.split("-")[0];
            String productCode = item.split("-")[1];
            LhMonthFinishQtyVo qtyVo = new LhMonthFinishQtyVo();
            qtyVo.setFactoryCode(factoryMonthPlanProdFinalVo.getFactoryCode());
            qtyVo.setProductCode(productCode);
            qtyVo.setSpecCode(specCode);
            qtyVo.setPlanStartDate(factoryMonthPlanProdFinalVo.getProductionStartDate());
            qtyVo.setPlanEndDate(factoryMonthPlanProdFinalVo.getProductionEndDate());

            // 期初库存
            if (finalStockMap.containsKey(productCode)) {
                Integer stockQty = finalStockMap.get(productCode);
                qtyVo.setInitQty(stockQty);
            }
            //根据productCode跟specCode查找当前的月计划最终定稿
            List<FactoryMonthPlanProdFinalVo> vos = currentMonthPlanList.stream().filter(it -> it.getProductCode().equals(productCode) && it.getSpecCode().equals(specCode)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(vos)) {
                qtyVo.setSpecDesc(vos.get(0).getProductDesc());
                qtyVo.setBrand(vos.get(0).getBrand());
                qtyVo.setUsedMouldQty(vos.get(0).getMouldQty());
                qtyVo.setMouldNo(vos.get(0).getMouldNo());
                // 月计划排产量
                qtyVo.setMonthPlanQty(calculateTotalSum(vos));//月计划量
            }
            // 可用模具数
            if (finalMouldStatusMap.containsKey(qtyVo.getMouldNo())) {
                Long mouldNum = finalMouldStatusMap.get(qtyVo.getMouldNo());
                qtyVo.setMouldQty(mouldNum.intValue());
            }
            MdmProductConstruction mdmProductConstructionVO = mdmProductConstructionVOS.stream().filter(it -> it.getProductCode().equals(productCode) && it.getSpecCode().equals(specCode)).findFirst().orElse(null);
            if (mdmProductConstructionVO != null) {
                qtyVo.setConstructionCode(mdmProductConstructionVO.getConstructionCode());
                qtyVo.setCuringTime(mdmProductConstructionVO.getCuringTime());
                qtyVo.setCuringTime2(mdmProductConstructionVO.getCuringTime2());
                qtyVo.setEmbryoCode(mdmProductConstructionVO.getEmbryoCode());
            }
            // 获取月计划剩余量
            List<LhMonthPlanSurplus> monthPlanSurpluses = surplusesdMap.get(specCode+"-"+productCode);
            if (PubUtil.isEmpty(monthPlanSurpluses)) {
                qtyVo.setMonthRemainQty(0);
            } else {
                qtyVo.setMonthRemainQty(monthPlanSurpluses.stream().mapToInt(LhMonthPlanSurplus::getMonthRemainQty).sum());//剩余数量
            }
            List<LhMonthDayFinishQtyVo> dayFinishQtyList = new ArrayList<>();

            qtyVo.setMonthFinishQty(values.stream().mapToInt(LhScheduleResultTotalVo::getTotalFinishQty).sum());//本月完成数量
//            System.out.println("前循specCode："+ specCode+"------productCode："+productCode);
            dateRange.forEach(dr -> {
                LhMonthDayFinishQtyVo dayFinishQtyVo = new LhMonthDayFinishQtyVo();
                List<LocalDate> collect = values.stream().map(rsd ->rsd.getRealScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).collect(Collectors.toList());
                if (collect.contains( dr)) {
                    LhScheduleResultTotalVo result = values.stream().filter(re -> dr.equals(re.getRealScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())).findFirst().get();
//                    System.out.println("当前循环日期："+ dr+"------记录："+JSON.toJSONString(result));
                    dayFinishQtyVo.setFinishDay(dr.toString());
                    dayFinishQtyVo.setFinishQty(result.getTotalFinishQty());
                } else {
                    dayFinishQtyVo.setFinishDay(dr.toString());
                    dayFinishQtyVo.setFinishQty(0);
                }
                dayFinishQtyList.add(dayFinishQtyVo);
            });
            qtyVo.setDayFinishQtyList(dayFinishQtyList);
            list.add(qtyVo);
        });


        //依据月度计划补充没有完成量的规格
        currentMonthPlanList.forEach(item -> {
            String specCode = item.getSpecCode();
            String productCode = item.getProductCode();

            if (!lhScheduledMap.containsKey(specCode+"-"+productCode)) {
            LhMonthFinishQtyVo qtyVo = new LhMonthFinishQtyVo();
            qtyVo.setFactoryCode(factoryMonthPlanProdFinalVo.getFactoryCode());
            qtyVo.setProductCode(productCode);
            qtyVo.setSpecCode(specCode);
            qtyVo.setPlanStartDate(factoryMonthPlanProdFinalVo.getProductionStartDate());
            qtyVo.setPlanEndDate(factoryMonthPlanProdFinalVo.getProductionEndDate());

            // 期初库存
            if (finalStockMap.containsKey(productCode)) {
                Integer stockQty = finalStockMap.get(productCode);
                qtyVo.setInitQty(stockQty);
            }
            //根据productCode跟specCode查找当前的月计划最终定稿
            List<FactoryMonthPlanProdFinalVo> vos = currentMonthPlanList.stream().filter(it -> it.getProductCode().equals(productCode) && it.getSpecCode().equals(specCode)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(vos)) {
                qtyVo.setSpecDesc(vos.get(0).getProductDesc());
                qtyVo.setBrand(vos.get(0).getBrand());
                qtyVo.setUsedMouldQty(vos.get(0).getMouldQty());
                qtyVo.setMouldNo(vos.get(0).getMouldNo());
                // 月计划排产量
                qtyVo.setMonthPlanQty(calculateTotalSum(vos));//月计划量
            }
            // 可用模具数
            if (finalMouldStatusMap.containsKey(qtyVo.getMouldNo())) {
                Long mouldNum = finalMouldStatusMap.get(qtyVo.getMouldNo());
                qtyVo.setMouldQty(mouldNum.intValue());
            }
            MdmProductConstruction mdmProductConstructionVO = mdmProductConstructionVOS.stream().filter(it -> it.getProductCode().equals(productCode) && it.getSpecCode().equals(specCode)).findFirst().orElse(null);
            if (mdmProductConstructionVO != null) {
                qtyVo.setConstructionCode(mdmProductConstructionVO.getConstructionCode());
                qtyVo.setCuringTime(mdmProductConstructionVO.getCuringTime());
                qtyVo.setCuringTime2(mdmProductConstructionVO.getCuringTime2());
                qtyVo.setEmbryoCode(mdmProductConstructionVO.getEmbryoCode());
            }
            // 获取月计划剩余量
            List<LhMonthPlanSurplus> monthPlanSurpluses = surplusesdMap.get(specCode+"-"+productCode);
            if (PubUtil.isEmpty(monthPlanSurpluses)) {
                qtyVo.setMonthRemainQty(0);
            } else {
                qtyVo.setMonthRemainQty(monthPlanSurpluses.stream().mapToInt(LhMonthPlanSurplus::getMonthRemainQty).sum());//剩余数量
            }
            List<LhMonthDayFinishQtyVo> dayFinishQtyList = new ArrayList<>();

            qtyVo.setMonthFinishQty(0);//本月完成数量
//            System.out.println("前循specCode："+ specCode+"------productCode："+productCode);
            dateRange.forEach(dr -> {
                LhMonthDayFinishQtyVo dayFinishQtyVo = new LhMonthDayFinishQtyVo();
                dayFinishQtyVo.setFinishDay(dr.toString());
                dayFinishQtyVo.setFinishQty(0);
                dayFinishQtyList.add(dayFinishQtyVo);
            });
            qtyVo.setDayFinishQtyList(dayFinishQtyList);
            list.add(qtyVo);
            }
        });
        return buildQueryCondition(list,queryVO);
    }

    private List buildQueryCondition(List<LhMonthFinishQtyVo> list,LhMonthPlanSurplusDetail queryVO) {
        if (PubUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 新增条件过滤（规格代码、sapid、生胎代码、品牌、规格、模具号）
        return list.stream()
                .filter(item -> StringUtils.isEmpty(queryVO.getProductCode()) || queryVO.getProductCode().equals(item.getProductCode()))
                .filter(item -> StringUtils.isEmpty(queryVO.getSpecCode()) || queryVO.getSpecCode().equals(item.getSpecCode()))
                .filter(item -> StringUtils.isEmpty(queryVO.getEmbryoCode()) || queryVO.getEmbryoCode().equals(item.getEmbryoCode()))
                .filter(item -> StringUtils.isEmpty(queryVO.getMouldNo()) || queryVO.getMouldNo().equals(item.getMouldNo()))
                .filter(item -> StringUtils.isEmpty(queryVO.getSpecDesc()) || StringUtils.contains(item.getSpecDesc(),queryVO.getSpecDesc()))
                .filter(item -> StringUtils.isEmpty(queryVO.getBrand()) || queryVO.getBrand().equals(item.getBrand()))
                .collect(Collectors.toList());
    }


    public int calculateTotal(LhScheduleResult item) {
        return Optional.ofNullable(item.getClass1FinishQty()).orElse(0)
                + Optional.ofNullable(item.getClass2FinishQty()).orElse(0)
                + Optional.ofNullable(item.getClass3FinishQty()).orElse(0)
                + Optional.ofNullable(item.getClass4FinishQty()).orElse(0)
                + Optional.ofNullable(item.getClass5FinishQty()).orElse(0)
                + Optional.ofNullable(item.getClass6FinishQty()).orElse(0);
    }

    public int calculateTotalSum(List<FactoryMonthPlanProdFinalVo> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (FactoryMonthPlanProdFinalVo record : records) {
            // total += record.sumDays(); // 累加每个对象的总和
        }
        return total;
    }


    public static List<LocalDate> getDateRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dateRange = new ArrayList<>();
        LocalDate current = startDate;

        // 循环直到当前日期超过结束日期
        while (!current.isAfter(endDate)) {
            dateRange.add(current);
            current = current.plusDays(1); // 逐天递增
        }
        return dateRange;
    }

    @Override
    public LhMonthFinishStatisticsDayQtyVo getStatisticsDay(LhMonthPlanSurplusDetail param) {
        List<LhMonthFinishQtyVo> lhMonthFinishQtyVoList = this.monthFinishQtyList(param);
        LhMonthFinishStatisticsDayQtyVo dayQtyVo = new LhMonthFinishStatisticsDayQtyVo();
        // 保持顺序
        Map<String, LhMonthDayFinishQtyVo> map = new LinkedHashMap<>(32);
        for (LhMonthFinishQtyVo lhMonthFinishQtyVo : lhMonthFinishQtyVoList) {
            Integer monthFinishQty = lhMonthFinishQtyVo.getMonthFinishQty();
            Integer monthRemainQty = lhMonthFinishQtyVo.getMonthRemainQty();
            Integer monthPlanQty = lhMonthFinishQtyVo.getMonthPlanQty();

            List<LhMonthDayFinishQtyVo> dayFinishQtyList = lhMonthFinishQtyVo.getDayFinishQtyList();
            for (LhMonthDayFinishQtyVo lhMonthDayFinishQtyVo : dayFinishQtyList) {
                String finishDay = lhMonthDayFinishQtyVo.getFinishDay();
                Integer finishQty = lhMonthDayFinishQtyVo.getFinishQty();
                if (map.containsKey(finishDay)) {
                    LhMonthDayFinishQtyVo vo = map.get(finishDay);
                    vo.setFinishQty(vo.getFinishQty() + finishQty);
                } else {
                    map.put(finishDay, lhMonthDayFinishQtyVo);
                }
            }
            dayQtyVo.setMonthFinishQtyTotal(dayQtyVo.getMonthFinishQtyTotal() + monthFinishQty);
            dayQtyVo.setMonthRemainQtyTotal(dayQtyVo.getMonthRemainQtyTotal() + monthRemainQty);
            dayQtyVo.setMonthPlanQtyTotal(dayQtyVo.getMonthPlanQtyTotal() + monthPlanQty);
        }
        List<LhMonthDayFinishQtyVo> list = new ArrayList<>();
        list.addAll(map.values());
        // 按照对应的时间排序
        list.sort(Comparator.comparing(LhMonthDayFinishQtyVo::getFinishDay));
        dayQtyVo.setDayFinishQtyList(list);
        /*LocalDate scheduleTime = param.getScheduleTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LambdaQueryWrapper<LhMonthPlanSurplus> surplus = new LambdaQueryWrapper<>();
        surplus.eq(LhMonthPlanSurplus::getFactoryCode, param.getFactoryCode())
                .eq(LhMonthPlanSurplus::getMonth, scheduleTime.getMonth())
                .eq(LhMonthPlanSurplus::getYear, scheduleTime.getYear())
                .eq(LhMonthPlanSurplus::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<LhMonthPlanSurplus> lhMonthPlanSurpluses = lhMonthPlanSurplusEntityMapper.selectList(surplus);
        dayQtyVo.setMonthFinishQtyTotal(lhMonthPlanSurpluses.stream().mapToInt(LhMonthPlanSurplus::getMonthFinishQty).sum());
        dayQtyVo.setMonthRemainQtyTotal(lhMonthPlanSurpluses.stream().mapToInt(LhMonthPlanSurplus::getMonthRemainQty).sum());
        dayQtyVo.setMonthPlanQtyTotal(lhMonthPlanSurpluses.stream().mapToInt(LhMonthPlanSurplus::getMonthPlanQty).sum());

        Date start = statisticsDay.get("statisticsDayStart");
        Date end = statisticsDay.get("statisticsDayEnd");
        //根据月度计划获取硫化计划
        LambdaQueryWrapper<LhScheduleResult> query = new LambdaQueryWrapper<>();
        query.eq(LhScheduleResult::getFactoryCode, param.getFactoryCode())
                .ge(LhScheduleResult::getRealScheduleDate,start)
                .le(LhScheduleResult::getRealScheduleDate,end)
                .eq(LhScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<LhScheduleResult> lhScheduleResults = lhScheduleResultEntityMapper.selectList(query);

        // 获取月度开始跟结束时间
        LocalDate startDate = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // 当前月的每一天
        List<LocalDate> dateRange = getDateRange(startDate, endDate);
        List<LhMonthDayFinishQtyVo> list = new ArrayList<>();
        dateRange.forEach(dr ->{
            LhMonthDayFinishQtyVo dayFinishQtyVo = new LhMonthDayFinishQtyVo();
            dayFinishQtyVo.setFinishDay(dr.toString());
            dayFinishQtyVo.setFinishQty(lhScheduleResults.stream().filter(item -> dr.equals(item.getRealScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())).mapToInt(this::calculateTotal).sum());
            list.add(dayFinishQtyVo);
        });
        dayQtyVo.setDayFinishQtyList(list);*/
        return dayQtyVo;
    }
}
