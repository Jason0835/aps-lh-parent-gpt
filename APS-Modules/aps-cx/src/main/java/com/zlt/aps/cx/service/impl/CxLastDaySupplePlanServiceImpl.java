package com.zlt.aps.cx.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import com.zlt.aps.cx.api.domain.entity.CxDispatcherLog;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.LhMachineChangeTpye;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.service.CxScheduleEngineService;
import com.zlt.aps.cx.mapper.CxLastDaySupplePlanMapper;
import com.zlt.aps.cx.mapper.CxMachineInfoMapper;
import com.zlt.aps.cx.service.CxDispatcherLogService;
import com.zlt.aps.cx.service.CxLastDaySupplePlanService;
import com.zlt.aps.lh.engine.enums.TaskTypeEnum;
import com.zlt.aps.lh.engine.service.LhEngineService;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型前日计划增补Service业务层处理
 *
 * @author chen
 * @date 2022-02-09
 */
@Service
public class CxLastDaySupplePlanServiceImpl implements CxLastDaySupplePlanService {
    @Autowired
    private CxLastDaySupplePlanMapper cxLastDaySupplePlanMapper;

    @Autowired
    private CxMachineInfoMapper cxMachineInfoMapper;

    @Autowired
    private CommonCacheService commonCacheService;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;
    @Resource
    private CxDispatcherLogService cxDispatcherLogService;
    @Autowired
    private LhEngineService lhEngineService;
    @Resource(name = "cxScheduleEngineService")
    private CxScheduleEngineService cxScheduleEngineService;

    /**
     * 查询成型前日计划增补列表
     *
     * @param cxLastDaySupplePlan 成型前日计划增补
     * @return 成型前日计划增补
     */
    @Override
    public List<CxLastDaySupplePlanDto> selectCxLastDaySupplePlanList(CxLastDaySupplePlanDto cxLastDaySupplePlan) {
        return cxLastDaySupplePlanMapper.selectCxLastDaySupplePlanList(cxLastDaySupplePlan);
    }

    /**
     * 根据id查询成型前日计划增补
     */
    @Override
    public CxLastDaySupplePlanDto getInfo(Long id) {
        return cxLastDaySupplePlanMapper.getInfo(id);
    }

    /**
     * 修改成型前日计划增补
     *
     * @param cxLastDaySupplePlan 成型前日计划增补
     * @return 结果
     */
    @Override
    public int updateCxLastDaySupplePlan(CxLastDaySupplePlanDto cxLastDaySupplePlan) {
        cxLastDaySupplePlan.setBaseVale(cxLastDaySupplePlan.getId());
        return cxLastDaySupplePlanMapper.updateCxLastDaySupplePlan(cxLastDaySupplePlan);
    }

    /**
     * 批量删除成型前日计划增补
     *
     * @param ids 需要删除的成型前日计划增补ID
     * @return 结果
     */
    @Override
    public int deleteCxLastDaySupplePlanByIds(Long[] ids) {
        return cxLastDaySupplePlanMapper.deleteCxLastDaySupplePlanByIds(ids);
    }

    /**
     * 根据id查询对应批次号对应的状态，以字符串的形式返回，逗号分隔
     *
     * @param ids 需要查询的数据ID
     * @return 结果
     */
    @Override
    public String selectStatusByIds(Long[] ids) {
        return cxLastDaySupplePlanMapper.selectStatusByIds(ids);
    }

    /**
     * 新增成型前日增补计划
     *
     * @param cxLastDaySupplePlan 前日增补计划
     * @return 结果
     */
    @Override
    public AjaxResult insertCxLastDaySupplePlan(CxLastDaySupplePlanDto cxLastDaySupplePlan) {
        // 校验是否有生产增补计划
        String scheduleDateStr = DateFormatUtils.format(cxLastDaySupplePlan.getScheduleDate(), "yyyyMMdd");
        CxLastDaySupplePlanDto batchNoAndCxBatchNoByScheduleDate = cxLastDaySupplePlanMapper.selectSuppleBatchNoAndCxBatchNoByScheduleDate(scheduleDateStr);
        if (batchNoAndCxBatchNoByScheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.notGenerateSupplePlan"));
        }
        String suppleBatchNo = batchNoAndCxBatchNoByScheduleDate.getSuppleBatchNo();
        String cxBatchNo = batchNoAndCxBatchNoByScheduleDate.getCxBatchNo();
        if (StringUtil.isBlank(suppleBatchNo) || StringUtil.isBlank(cxBatchNo)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.notGenerateSupplePlan"));
        }


        //检查同机台同胎胚存在同时投产的外胎品号
        AjaxResult error = checkSameMachine2Embryo(cxLastDaySupplePlan, suppleBatchNo, cxBatchNo);
        if (error != null) {
            return error;
        }


        cxLastDaySupplePlan.setSuppleBatchNo(suppleBatchNo);
        cxLastDaySupplePlan.setCxBatchNo(cxBatchNo);
        cxLastDaySupplePlan.setTaskType(TaskTypeEnum.TODO.getTaskType());
        cxLastDaySupplePlan.setProductionStatus(ApsConstant.NO_PRODUNTION);
        cxLastDaySupplePlan.setIsRelease(ApsConstant.NO_RELEASE);
        cxLastDaySupplePlan.setCxMachineType(cxLastDaySupplePlan.getEmbryoCode().startsWith("Y") ? "1":"2");

        //设置班制
        setWorkShifts(cxLastDaySupplePlan);

        //获取工单
        String scheduleOrderNo = commonCacheService.getCxSequence(CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleDateStr, CxPrefixConstants.CX_ORDER_NO_PREFIX + scheduleDateStr);
        cxLastDaySupplePlan.setOrderNo(scheduleOrderNo);
        return cxLastDaySupplePlanMapper.insertCxLastDaySupplePlan(cxLastDaySupplePlan) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 检查同机台同胎胚存在同时投产的外胎品号
     * @param cxLastDaySupplePlan
     * @param suppleBatchNo
     * @param cxBatchNo
     * @return
     */
    private AjaxResult checkSameMachine2Embryo(CxLastDaySupplePlanDto cxLastDaySupplePlan, String suppleBatchNo, String cxBatchNo) {
        CxLastDaySupplePlanDto CxLastDaySupplePlanQry = new CxLastDaySupplePlanDto();
        CxLastDaySupplePlanQry.setSuppleBatchNo(suppleBatchNo);
        CxLastDaySupplePlanQry.setCxBatchNo(cxBatchNo);
        CxLastDaySupplePlanQry.setCxMachineCode(cxLastDaySupplePlan.getCxMachineCode());
        CxLastDaySupplePlanQry.setEmbryoCode(cxLastDaySupplePlan.getEmbryoCode());
        List<CxLastDaySupplePlanDto> lastDaySupplePlanList = cxLastDaySupplePlanMapper.selectCxLastDaySupplePlanList2(CxLastDaySupplePlanQry);
        if (StringUtils.isNotEmpty(lastDaySupplePlanList)){
            for (CxLastDaySupplePlanDto lastDaySupplePlanDto:lastDaySupplePlanList){
                if (lastDaySupplePlanDto.getToProduct().equals(CxEngineConstants.TO_PRODUCT_YES)){
                    return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.sameMachine2Embryo2ProductStatus"));
                }
            }
        }
        return null;
    }

    /**
     * 设置班制
     * @param cxLastDaySupplePlan
     */
    private void setWorkShifts(CxLastDaySupplePlanDto cxLastDaySupplePlan) {
        CxMachineInfo machineInfoQry = new CxMachineInfo();
        machineInfoQry.setMachineCode(cxLastDaySupplePlan.getCxMachineCode());
        machineInfoQry.setStatus("0");
        List<CxMachineInfo> machineInfos = cxMachineInfoMapper.listOrderByName(machineInfoQry);
        if (StringUtils.isNotEmpty(machineInfos)){
            cxLastDaySupplePlan.setWorkShifts(Integer.valueOf(machineInfos.get(0).getClassShift()));
        }
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return cxLastDaySupplePlanMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 校验-使用模数
     */
    public AjaxResult modifyMoldsValidate(CxLastDaySupplePlanDto lastDaySupplePlanDto) {

        String msg = "";
        String msg1 = "";
        String notWork = ""; //不可作业机台名称集合
        String errorLimitWork = ""; //错误的限制作业机台名称集合
        String limitMachineName = ""; //已配置限制作业的机台名称集合
        List<LhMachineChangeTpye> lhMachineChangeTpyeList = lastDaySupplePlanDto.getLhMachineChangeTpyeList();
        if (CollectionUtils.isNotEmpty(lhMachineChangeTpyeList)) {
            CxLastDaySupplePlanDto dto = cxLastDaySupplePlanMapper.getInfo(lastDaySupplePlanDto.getId());

            //初始值-机台
            CxMachineInfo machineInfo = new CxMachineInfo();
            machineInfo.setId(6L);
            List<CxMachineInfo> list3 = cxMachineInfoMapper.selectCxMachineInfoList2(machineInfo);
            Map<String, String> machineNameMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(list3)) {
                machineNameMap = list3.stream().collect(Collectors.toMap(CxMachineInfo::getMachineCode, CxMachineInfo::getMachineName));
            }

            //原硫化机台数组
            String osLhMachineCode = dto.getLhMachineCode();
//            List<String> osLhMachineCodeArr = null;
//            if (StringUtils.isNotEmpty(osLhMachineCode)) {
//                osLhMachineCodeArr = Arrays.asList(osLhMachineCode.split(","));
//            }

            //重复记录校验
            TreeSet<LhMachineChangeTpye> treeSet = new TreeSet<>(Comparator.comparing(LhMachineChangeTpye::getMachineCode));
            treeSet.addAll(lhMachineChangeTpyeList);
            if (treeSet.size() != lhMachineChangeTpyeList.size()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.alreadyExists"));
            }

            //校验-相同sap硫化机不能重复，警告提示：使用模数不能大于剩余模数
            for (LhMachineChangeTpye item : lhMachineChangeTpyeList) {

                //相同sap硫化机不能重复
                CxLastDaySupplePlanDto query = new CxLastDaySupplePlanDto();
                query.setScheduleDate(dto.getScheduleDate());
                query.setSapCode(dto.getSapCode());
                query.setId(dto.getId());
                query.setLhMachineCode(item.getMachineCode());
                List<CxLastDaySupplePlanDto> list = cxLastDaySupplePlanMapper.getListByLhMachineCode(query);
                if (CollectionUtils.isNotEmpty(list)) {
                    String machineName = machineNameMap.get(item.getMachineCode());
                    return AjaxResult.error(StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.sapCodeAlreadyExists"), machineName));
                }

                //警告提示：使用模数不能大于剩余模数
                Long maxMolds = 0L;
                Long usedMolds = 0L;
                Long usableMolds = 0L;
                CxMachineInfo query1 = new CxMachineInfo();
                query1.setMachineCode(item.getMachineCode());
                List<CxMachineInfo> lhMachine = cxMachineInfoMapper.getMaxMoldsByLhMachineCode(query1);
                if (CollectionUtils.isNotEmpty(lhMachine)) {
                    maxMolds = lhMachine.get(0).getQuata() == null ? 0L : lhMachine.get(0).getQuata();
                }

                //统计已使用模数
                CxLastDaySupplePlanDto query2 = new CxLastDaySupplePlanDto();
                query2.setScheduleDate(dto.getScheduleDate());
                query2.setId(dto.getId());
                query2.setLhMachineCode(item.getMachineCode());
                List<CxLastDaySupplePlanDto> list2 = cxLastDaySupplePlanMapper.getListByLhMachineCode(query2);
                for (CxLastDaySupplePlanDto item1 : list2) {
                    String desc = item1.getLhMachineChangeMoldDesc();
                    if (StringUtils.isNotEmpty(desc)) {
                        String[] descs = desc.split(";");
                        for (String str : descs) {
                            if (str.contains(item.getMachineCode() + ":")) {
                                String[] detail = str.split(":");
                                usedMolds = usedMolds + Long.parseLong(detail[2]);
                            }
                        }
                    }
                }

                //计算可用模数
                usableMolds = maxMolds - usedMolds;
                String machineName = machineNameMap.get(item.getMachineCode());
                if (usableMolds <= 0L) {
                    if (StringUtils.isEmpty(msg)) {
                        msg = machineName;
                    } else {
                        msg = msg + "，" + machineName;
                    }
                } else {
                    long de = usableMolds - item.getMolds();
                    if (de < 0L) {
                        if (StringUtils.isEmpty(msg1)) {
                            msg1 = StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.usableMoldsValidate"), machineName, usableMolds, item.getMolds());
                        } else {
                            msg1 = msg1 + "<br/>" + StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.usableMoldsValidate"), machineName, usableMolds, item.getMolds());
                        }
                    }
                }
            }

            //若配置了定点机台那么做不可/限制作业校验
            CxMachineInfo cxMachineInfo = new CxMachineInfo();
            cxMachineInfo.setMachineName(dto.getSapCode());
            List<CxMachineInfo> specimalMachineList = cxMachineInfoMapper.getLhSpecimalMachine(cxMachineInfo);
            if (CollectionUtils.isNotEmpty(specimalMachineList)) {
                List<CxMachineInfo> notWorkMachineList = specimalMachineList.stream().filter(a -> "1".equals(a.getType())).collect(Collectors.toList());
                Map<String, String> notWorkMachineMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(notWorkMachineList)) {
                    notWorkMachineMap = notWorkMachineList.stream().collect(Collectors.toMap(CxMachineInfo::getMachineCode, CxMachineInfo::getMachineName));
                }

                List<CxMachineInfo> limitWorkMachineList = specimalMachineList.stream().filter(a -> "0".equals(a.getType())).collect(Collectors.toList());
                Map<String, String> limitWorkMachineMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(limitWorkMachineList)) {
                    limitWorkMachineMap = limitWorkMachineList.stream().collect(Collectors.toMap(CxMachineInfo::getMachineCode, CxMachineInfo::getMachineName));
                    List<String> machineNameList = limitWorkMachineList.stream().map(CxMachineInfo::getMachineName).collect(Collectors.toList());
                    limitMachineName = StringUtils.join(machineNameList, "、");
                }

                for (LhMachineChangeTpye item : lhMachineChangeTpyeList) {
                    if (StringUtils.isNotBlank(notWorkMachineMap.get(item.getMachineCode()))) {
                        if (StringUtils.isNotBlank(notWork)) {
                            notWork = notWork + "、" + notWorkMachineMap.get(item.getMachineCode());
                        } else {
                            notWork = notWorkMachineMap.get(item.getMachineCode());
                        }
                        continue;
                    }
                    if (CollectionUtils.isNotEmpty(limitWorkMachineList) && StringUtils.isBlank(limitWorkMachineMap.get(item.getMachineCode()))) {
                        if (StringUtils.isNotBlank(errorLimitWork)) {
                            errorLimitWork = errorLimitWork + "、" + machineNameMap.get(item.getMachineCode());
                        } else {
                            errorLimitWork = machineNameMap.get(item.getMachineCode());
                        }
                    }
                }
            }
        }

        //提示信息
        String resultMsg = "";
        if (StringUtils.isNotEmpty(msg)) {
            resultMsg = StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.moreThanThreeUsed"), msg);
        }
        if (StringUtils.isNotEmpty(msg1)) {
            if (StringUtils.isNotEmpty(resultMsg)) {
                resultMsg = resultMsg + "<br/>" + msg1;
            } else {
                resultMsg = msg1;
            }
        }

        if (StringUtils.isNotEmpty(resultMsg)) {
            resultMsg = resultMsg + " " + I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.makesureAdd");
        }

        if (StringUtils.isNotBlank(notWork)) {
            String notWorkMsg = StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.notWorkMsg"), notWork);
            if (StringUtils.isNotBlank(resultMsg)) {
                resultMsg = resultMsg + "<br/>" + notWorkMsg;
            } else {
                resultMsg = notWorkMsg;
            }
        }

        if (StringUtils.isNotBlank(errorLimitWork)) {
            String errorLimitWorkMsg = StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.errorLimitWork"), errorLimitWork, limitMachineName);
            if (StringUtils.isNotBlank(resultMsg)) {
                resultMsg = resultMsg + "<br/>" + errorLimitWorkMsg;
            } else {
                resultMsg = errorLimitWorkMsg;
            }
        }
        return AjaxResult.success(resultMsg);
    }

    /**
     * 修改-使用模数
     * 更换类型：硫化机code1:拆模换code:模数molds;硫化机code2:点数换code:模数molds;
     */
    public AjaxResult modifyMolds(CxLastDaySupplePlanDto lastDaySupplePlanDto) {
        //初始值定义
        List<LhMachineChangeTpye> lhMachineChangeTpyeList = lastDaySupplePlanDto.getLhMachineChangeTpyeList();
        CxLastDaySupplePlanDto supplePlanDto = cxLastDaySupplePlanMapper.getInfo(lastDaySupplePlanDto.getId());
        CxLastDaySupplePlanDto oldScheduleResult = (CxLastDaySupplePlanDto) supplePlanDto.clone();
        CxLastDaySupplePlanDto osEntity = new CxLastDaySupplePlanDto();
        BeanUtils.copyProperties(supplePlanDto, osEntity);
        //原硫化机台数组
        String osLhMachineCode = supplePlanDto.getLhMachineCode();
        List<String> osLhMachineCodeArr = null;
        if (StringUtils.isNotEmpty(osLhMachineCode)) {
            osLhMachineCodeArr = Arrays.asList(osLhMachineCode.split(","));
        }

        //硫化机台名称map
        CxMachineInfo machineInfo = new CxMachineInfo();
        machineInfo.setId(6L);
        List<CxMachineInfo> list3 = cxMachineInfoMapper.selectCxMachineInfoList2(machineInfo);
        Map<String, String> machineNameMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(list3)) {
            machineNameMap = list3.stream().collect(Collectors.toMap(CxMachineInfo::getMachineCode, CxMachineInfo::getMachineName));
        }

        //更新硫化机台、使用模数、硫化机变更类型描述（覆盖更新）
        Double lhMachineQty = 0d;
        String newLhMachineCode = "";
        String lhMachineChangeMoldDesc = "";
        for (LhMachineChangeTpye item : lhMachineChangeTpyeList) {
            lhMachineQty = lhMachineQty + item.getMolds();
            if (StringUtils.isEmpty(newLhMachineCode)) {
                newLhMachineCode = item.getMachineCode();
            } else {
                newLhMachineCode = newLhMachineCode + "," + item.getMachineCode();
            }
            if (StringUtils.isEmpty(lhMachineChangeMoldDesc)) {
                lhMachineChangeMoldDesc = item.getMachineCode() + ":" + item.getChangeType() + ":" + item.getMolds() + ";";
            } else {
                lhMachineChangeMoldDesc = lhMachineChangeMoldDesc + item.getMachineCode() + ":" + item.getChangeType() + ":" + item.getMolds() + ";";
            }
        }

        //更新
        boolean flag = false;
        if (StringUtils.isNotEmpty(supplePlanDto.getLhMachineChangeMoldDesc())) {
            List<String> desc1 = Arrays.asList(supplePlanDto.getLhMachineChangeMoldDesc().split(";"));
            List<String> desc2 = Arrays.asList(lhMachineChangeMoldDesc.split(";"));
            flag = compare(desc1, desc2);
        }
        if (!flag && StringUtils.isNotEmpty(lhMachineChangeMoldDesc)) {
            supplePlanDto.setTaskType(TaskTypeEnum.MOLD.getTaskType());
        }
        supplePlanDto.setLhMachineQty(lhMachineQty);
        supplePlanDto.setLhMachineCode(newLhMachineCode);
        supplePlanDto.setLhMachineChangeMoldDesc(lhMachineChangeMoldDesc);
        this.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, oldScheduleResult, supplePlanDto);  //如果是调度员操作，则需要增加操作日志
        int result = updateCxSupplyPlanForMolds(supplePlanDto, osEntity);
        //Joran 2021-09-02 调用单排程模具变动单记录生成
//        lhEngineService.singleMoldChangePlanTask(supplePlanDto, osLhMachineCodeArr);
        return AjaxResult.success();
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType    操作类型：0--转机台、1--调量
     */
    public void insetDispatcherLog(String operType, CxLastDaySupplePlanDto oldSchedule, CxLastDaySupplePlanDto newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        if (oldSchedule == null) {
            oldSchedule = this.cxLastDaySupplePlanMapper.getInfo(newSchedule.getId());  //操作前的排程数据
        }
        CxDispatcherLog log = new CxDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setSapCode(newSchedule.getSapCode());  //sap品号
        log.setEmbryoCode(newSchedule.getEmbryoCode());  //胎胚代码
        log.setEmbryoVersion(newSchedule.getBomDataVersion());  //胎胚版本
        //操作前的信息赋值
        log.setBeforeLhMachineCode(oldSchedule.getLhMachineCode());
        log.setBeforeCxMachineCode(oldSchedule.getCxMachineCode());
        log.setBeforeClass1Plan(oldSchedule.getClass1PlanQty());
        log.setBeforeClass2Plan(oldSchedule.getClass2PlanQty());
        log.setBeforeClass3Plan(oldSchedule.getClass3PlanQty());
        log.setBeforeClass4Plan(oldSchedule.getClass4PlanQty());
        log.setBeforeClass5Plan(oldSchedule.getClass5PlanQty());
        //操作后的信息赋值
        log.setAfterLhMachineCode(newSchedule.getLhMachineCode());
        log.setAfterCxMachineCode(newSchedule.getCxMachineCode());
        log.setAfterClass1Plan(newSchedule.getClass1PlanQty());
        log.setAfterClass2Plan(newSchedule.getClass2PlanQty());
        log.setAfterClass3Plan(newSchedule.getClass3PlanQty());
        log.setAfterClass4Plan(newSchedule.getClass4PlanQty());
        log.setAfterClass5Plan(newSchedule.getClass5PlanQty());
        /* 调用插入日志方法 */
        cxDispatcherLogService.insertCxDispatcherLog(log);
    }

    /**
     * 更新前日增补计划使用模数
     */
    public int updateCxSupplyPlanForMolds(CxLastDaySupplePlanDto lastDaySupplePlanDto, CxLastDaySupplePlanDto osEntity) {

        //硫化状态为投产中且硫化机台数变少了后更改硫化状态为已收尾欠产，
        List<String> str1 = null;
        List<String> str2 = null;
        int newMachines = 0;
        int osMachines = 0;
        if (StringUtils.isNotBlank(lastDaySupplePlanDto.getLhMachineCode())) {
            str1 = Arrays.asList(lastDaySupplePlanDto.getLhMachineCode().split(","));
            newMachines = str1.size();
        }
        if (StringUtils.isNotBlank(osEntity.getLhMachineCode())) {
            str2 = Arrays.asList(osEntity.getLhMachineCode().split(","));
            osMachines = str2.size();
        }

        if (CxEngineConstants.TASK_TYPE_DOING.equals(lastDaySupplePlanDto.getTaskType()) && newMachines < osMachines) {
            lastDaySupplePlanDto.setTaskType(CxEngineConstants.TASK_CLOSE_OUT_DELIN);
        }

        // 校验字段是否修改，修改则改状态为未发布
        if (ApsConstant.IS_RELEASE.equals(lastDaySupplePlanDto.getIsRelease()) || StringUtils.isEmpty(lastDaySupplePlanDto.getIsRelease())) {

            List<String> desc1 = null;
            List<String> desc2 = null;
            if (StringUtils.isNotBlank(lastDaySupplePlanDto.getLhMachineChangeMoldDesc())) {
                desc1 = Arrays.asList(lastDaySupplePlanDto.getLhMachineChangeMoldDesc().split(";"));
            }
            if (StringUtils.isNotBlank(osEntity.getLhMachineChangeMoldDesc())) {
                desc2 = Arrays.asList(osEntity.getLhMachineChangeMoldDesc().split(";"));
            }

            boolean flag = compare(str1, str2) && compare(desc1, desc2);
            if (!flag) {
                lastDaySupplePlanDto.setIsRelease(ApsConstant.NO_RELEASE);
            }
        }
        //Joran 2021-09-16 进行单班硫化量重算
//        cxScheduleEngineService.calcAvaliableClassShift(lastDaySupplePlanDto, AdjustTypeEnums.CHANGE_LH_MACHINE);
        commonCacheService.calcSupplePlanSingleShiftLhQty(lastDaySupplePlanDto);
        int result = cxLastDaySupplePlanMapper.updateCxLastDaySupplePlan(lastDaySupplePlanDto);

        //Joran 2022-03-31 更新完毕后进行同机台同胎胚不同外胎计划量重新合并更新start
        if(result > 0){
            cxScheduleEngineService.reSetSupplePlanSingleLhShiftQty(lastDaySupplePlanDto);
        }
        //Joran 2022-03-31  更新完毕后进行同机台同胎胚不同外胎计划量重新合并更新end

        return result;
    }

    private boolean compare(List<String> a, List<String> b) {
        if (CollectionUtils.isEmpty(a) && CollectionUtils.isEmpty(b)) {
            return true;
        }
        if (CollectionUtils.isNotEmpty(a) && CollectionUtils.isEmpty(b)) {
            return false;
        }
        if (CollectionUtils.isEmpty(a) && CollectionUtils.isNotEmpty(b)) {
            return false;
        }
        if (a.size() != b.size()) {
            return false;
        }
        Collections.sort(a);
        Collections.sort(b);
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
        }
        return true;
    }
}
