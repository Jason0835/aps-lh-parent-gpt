package com.zlt.aps.lh.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.MoldChangeTypeEnums;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.lh.api.domain.dto.LhApsMoldAdjustPlanDto;
import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.common.constant.LhConstants;
import com.zlt.aps.lh.engine.common.LhCommonService;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.util.LhEngineScheduleUtils;
import com.zlt.aps.lh.mapper.LhApsMoldAdjustPlanMapper;
import com.zlt.aps.lh.service.LhApsMoldAdjustPlanService;
import com.zlt.aps.lh.service.LhMachineInfoService;
import com.zlt.aps.lh.vo.MoldPlanPublishRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 硫化工序模具变动单APSService业务层处理
 * 
 * @author Joran.zhang
 * @date 2022-06-07
 */
@Service
@Slf4j
public class LhApsMoldAdjustPlanServiceImpl implements LhApsMoldAdjustPlanService
{
    @Autowired
    private LhApsMoldAdjustPlanMapper lhApsMoldAdjustPlanMapper;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    @Autowired
    private LhCommonService lhCommonService;

    @Autowired
    private LhMachineInfoService lhMachineInfoService;

    //成型参数加载
    private  Map<String,String> cxParamsMap;

    /**
     * 查询硫化工序模具变动单APS
     * 
     * @param id 硫化工序模具变动单APSID
     * @return 硫化工序模具变动单APS
     */
    @Override
    public LhApsMoldAdjustPlan selectLhApsMoldAdjustPlanById(Long id)
    {
        return lhApsMoldAdjustPlanMapper.selectLhApsMoldAdjustPlanById(id);
    }

    /**
     * 查询硫化工序模具变动单APS列表
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 硫化工序模具变动单APS
     */
    @Override
    public List<LhApsMoldAdjustPlan> selectLhApsMoldAdjustPlanList(LhApsMoldAdjustPlan lhApsMoldAdjustPlan)
    {
        return lhApsMoldAdjustPlanMapper.selectLhApsMoldAdjustPlanList(lhApsMoldAdjustPlan);
    }

    /**
     * 新增硫化工序模具变动单APS
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 结果
     */
    @Override
    public int insertLhApsMoldAdjustPlan(LhApsMoldAdjustPlan lhApsMoldAdjustPlan)
    {
        lhApsMoldAdjustPlan.setBaseVale(null);
        lhApsMoldAdjustPlan.setDataSource(LhConstants.LH_APS_MOLD_PLAN_SOURCE_ADD);//设置数据来源为新增
        // 工单号生成
        String orderNo = lhCommonService.createOrderNo(LhConstants.MOLD_CHANG_ORDER_NO_PREFIX, DateUtils.parseDateToStr("yyyy-MM-dd", lhApsMoldAdjustPlan.getPlanDate()));
        lhApsMoldAdjustPlan.setMoldOrderNo(orderNo);
        //硫化外胎施工信息start
        String beforeSapCode = lhApsMoldAdjustPlan.getBeforeSapCode();
        List<String> sapCodeList = Collections.singletonList(StringUtils.isBlank(beforeSapCode) ? lhApsMoldAdjustPlan.getAfterSapCode() : beforeSapCode);
        LhEngineTireConstructionInfo condition = new LhEngineTireConstructionInfo();
        List<LhEngineTireConstructionInfo> constructionInfoList = new ArrayList<>();
        if(StringUtils.isNotEmpty(sapCodeList)) {
            condition.setSapCodeList(sapCodeList);
            constructionInfoList = lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(condition);
        }
        Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap = new HashMap<>();
        if(StringUtils.isNotEmpty(constructionInfoList)) {
            sapTireConstructionListMap = constructionInfoList.stream().collect(Collectors.groupingBy(LhEngineTireConstructionInfo::getSapCode));
        }

        //Joran 2022-07-06 加载工序参数
        loadParams();
        //硫化外胎施工信息end
        String changeType = lhApsMoldAdjustPlan.getChangeType();
        Integer tireRoughStock = null;
        Integer useMoldNumber = null;
        if (!(MoldChangeTypeEnums.LEFT_MOLD_MERGE.getValue().equals(changeType) || MoldChangeTypeEnums.RIGHT_MOLD_MERGE.getValue().equals(changeType)
            || MoldChangeTypeEnums.SPLIT_OUT_CHANGE.getValue().equals(changeType) || MoldChangeTypeEnums.SPLIT_MOLD_MEGER.getValue().equals(changeType))) {
            // 左模合并、右模合并、拆模换、拆模合并不用计算换模时间
            tireRoughStock = lhApsMoldAdjustPlan.getTireRoughStock();
            useMoldNumber = lhApsMoldAdjustPlan.getUseMoldNumber();
            if (tireRoughStock == null || useMoldNumber == null) {
                String message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.useMoldAndStockEmpty"),
                        lhApsMoldAdjustPlan.getLhMachineName(), MoldChangeTypeEnums.getMoldChangeTypeByName(changeType, ServletUtils.getUserLang().toString()));
                throw new RuntimeException(message);
            }else {
                Date changeMoldTime = calcChangeMoldTime(lhApsMoldAdjustPlan, sapTireConstructionListMap, tireRoughStock, useMoldNumber);
                lhApsMoldAdjustPlan.setChangeMoldTime(changeMoldTime);
            }
        }
        //Joran 2022-07-06 清空工序参数
        clearLoadParams();
        return lhApsMoldAdjustPlanMapper.insertLhApsMoldAdjustPlan(lhApsMoldAdjustPlan);
    }

    /**
     * 修改硫化工序模具变动单APS
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 结果
     */
    @Override
    public int updateLhApsMoldAdjustPlan(LhApsMoldAdjustPlan lhApsMoldAdjustPlan)
    {
        lhApsMoldAdjustPlan.setBaseVale(lhApsMoldAdjustPlan.getId());
        return lhApsMoldAdjustPlanMapper.updateLhApsMoldAdjustPlan(lhApsMoldAdjustPlan);
    }

    /**
     * 批量删除硫化工序模具变动单APS
     * 
     * @param ids 需要删除的硫化工序模具变动单APSID
     * @return 结果
     */
    @Override
    public int deleteLhApsMoldAdjustPlanByIds(Long[] ids)
    {
        return lhApsMoldAdjustPlanMapper.deleteLhApsMoldAdjustPlanByIds(ids);
    }

    /**
     * 删除硫化工序模具变动单APS信息
     * 
     * @param id 硫化工序模具变动单APSID
     * @return 结果
     */
    @Override
    public int deleteLhApsMoldAdjustPlanById(Long id)
    {
        return lhApsMoldAdjustPlanMapper.deleteLhApsMoldAdjustPlanById(id);
    }

    /**
     * 校验硫化工序模具变动单APS唯一性
     */
    @Override
    public String checkLhApsMoldAdjustPlanUnique(LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        if (lhApsMoldAdjustPlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
      /*  List<LhApsMoldAdjustPlan> list = lhApsMoldAdjustPlanMapper.selectLhApsMoldAdjustPlanList(lhApsMoldAdjustPlan);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }*/
        return UserConstants.UNIQUE;
    }

    /**
     * 导入硫化工序模具变动单APS数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<LhApsMoldAdjustPlan> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhApsMoldAdjustPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        String planDate="";
        List<String> sapCodeList=new ArrayList<>();
        List<LhMachineInfo> machineInfoList = lhMachineInfoService.selectMachineInfoList(new LhMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {

            //根据机台名称去重
            TreeSet<LhMachineInfo> treeSet = new TreeSet<LhMachineInfo>(new Comparator<LhMachineInfo>() {
                @Override
                public int compare(LhMachineInfo o1, LhMachineInfo o2) {
                    return o1.getMachineName().compareTo(o2.getMachineName());
                }
            });
            treeSet.addAll(machineInfoList);
            machineInfoList =new ArrayList<>(treeSet);

            machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(LhMachineInfo::getMachineName, LhMachineInfo::getMachineCode));
        }

        if(StringUtils.isNotEmpty(list)) {
            LhApsMoldAdjustPlan first=list.get(0);
            planDate= DateUtils.parseDateToStr("yyyy-MM-dd",first.getPlanDate());

            //添加如果存在发布成功的数据不允许进行导入start
            int publishCount=lhApsMoldAdjustPlanMapper.isPublishSuccessValidate(planDate);
            if(publishCount>0){
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.publish.success.error"),planDate), importErrorLogs);
                return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + list.size(), importErrorLogs);
            }
            //添加如果存在发布成功的数据不允许进行导入end
            lhApsMoldAdjustPlanMapper.deleteLhApsMoldAdjustPlanByPlanDate(planDate);
            for(LhApsMoldAdjustPlan lhApsMoldAdjustPlan:list){
                String beforeSapCode=lhApsMoldAdjustPlan.getBeforeSapCode();
                if(StringUtils.isNotBlank(beforeSapCode)){
                    sapCodeList.add(beforeSapCode);
                }
                String afterSapCode = lhApsMoldAdjustPlan.getAfterSapCode();
                if (StringUtils.isNotBlank(afterSapCode)) {
                    sapCodeList.add(afterSapCode);
                }
            }
        }

        //硫化外胎施工信息start
        LhEngineTireConstructionInfo condition=new LhEngineTireConstructionInfo();
        List<LhEngineTireConstructionInfo> constructionInfoList=new ArrayList<>();
        if(StringUtils.isNotEmpty(sapCodeList)) {
            condition.setSapCodeList(sapCodeList);
            constructionInfoList=lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(condition);
        }
        Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap=new HashMap<>();
        if(StringUtils.isNotEmpty(constructionInfoList)){
            sapTireConstructionListMap=constructionInfoList.stream().collect(Collectors.groupingBy(lhEngineScheduleResult -> lhEngineScheduleResult.getSapCode()));
        }
        //硫化外胎施工信息end

        //公共校验（非空校验、长度校验等）
        for (int i = 0,len=list.size(); i < len; i++) {
            int errorNum = i + 2;
            LhApsMoldAdjustPlan lhApsMoldAdjustPlan = list.get(i);
            MoldChangeTypeEnums moldChangeTypeEnums = MoldChangeTypeEnums.getMoldChangeTypeByValue(lhApsMoldAdjustPlan.getChangeType());
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, lhApsMoldAdjustPlan);
            if(moldChangeTypeEnums == null) {
                String message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeTypeRequired"), errorNum);
                addImportErrorLog(importLogId, errorNum, message, validated);
            }
            // 日期格式是否错误校验,如果日期为1970-01-01时，表示不符合日期表达式
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(-28800000);
            if(calendar.getTime().equals(lhApsMoldAdjustPlan.getChangeMoldTime()) && lhApsMoldAdjustPlan.getChangeMoldTime() != null) {
                String message = String.format(I18nUtil.getMessage("import.validated.date"), errorNum, I18nUtil.getMessage("ui.data.column.lhApsMoldAdjustPlan.changeMoldTime"));
                addImportErrorLog(importLogId, errorNum, message, validated);
            }
            // 机台名称转code start
            String lhMachineName = lhApsMoldAdjustPlan.getLhMachineName();
            String lhMachineCode = machineCodeMap.get(lhMachineName);
            if (lhMachineCode == null) {
                addImportErrorLog(importLogId, errorNum, I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }else {
                lhApsMoldAdjustPlan.setLhMachineCode(lhMachineCode);
            }
            // 机台名称转code end

            // sap和胎胚对应关系校验 start
            validateSapAndEmbryoRela(importLogId, sapTireConstructionListMap, errorNum, lhApsMoldAdjustPlan, moldChangeTypeEnums, validated);
            // sap和胎胚对应关系校验 end

            if (CollectionUtils.isNotEmpty(validated)) {
                lhApsMoldAdjustPlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                lhApsMoldAdjustPlan.setBaseVale(null);
            }
            // 需要一起加到导入集合中，因为后面还需要做校验，提示信息需要用到错误行数
            importList.add(lhApsMoldAdjustPlan);
        }
        // 最大换模时间分组：Key(机台名称 + 左右模), Value(分组后的最大换模时间) 将相同左右模的替换为A，方便分组
        Map<String, Date> machineNameMaxChangeMoldTime = new HashMap<>();
        // 是否要取最大换模时间分组：Key(机台名称 + 左右模), Value(分组后根据换模类型判断是否有计算过换模时间，计算过则true，否则false)
        Map<String, Boolean> isMaxChangeMoldTimeMap = new HashMap<>();

        //合法数据进行自定义校验
        if(StringUtils.isNotEmpty(importList)){
            //Joran 2022-07-06 加载工序参数
            loadParams();
            Map<String, List<LhApsMoldAdjustPlan>> machineSapListMap = importList.stream().collect(Collectors.groupingBy(item -> item.getLhMachineCode() + item.getBeforeSapCode()));
            for(int i = 0; i < importList.size(); i++){
                int errorNum = i + 2;
                LhApsMoldAdjustPlan lhApsMoldAdjustPlan = importList.get(i);
                if (lhApsMoldAdjustPlan.getId() != null && lhApsMoldAdjustPlan.getId().equals(-999L)) {
                    continue;
                }
               boolean flag = customValidate(lhApsMoldAdjustPlan,sapTireConstructionListMap,importLogId,importErrorLogs, machineSapListMap, errorNum);
               if(!flag){
                   lhApsMoldAdjustPlan.setId(-999L);
                   failureNum++;
               }
               // 根据分组条件填充 map
                fillMapWithCondition(machineNameMaxChangeMoldTime, isMaxChangeMoldTimeMap, lhApsMoldAdjustPlan);
            }
            /*
            使用较后收尾的规格需要硫化的胎胚总数，换模时间
            不能挪到上面的for循环里，需要计算完所有换模时间后取出最大的换模时间
             */
            getMaxChangeMoldTime(importList, machineNameMaxChangeMoldTime, isMaxChangeMoldTimeMap);

            //验证换模时间是否在班次范围内start
            /*for(int i = 0; i < importList.size(); i++){
                int errorNum = i + 2;
                LhApsMoldAdjustPlan lhApsMoldAdjustPlan = importList.get(i);
                if (lhApsMoldAdjustPlan.getId() != null && lhApsMoldAdjustPlan.getId().equals(-999L)) {
                    continue;
                }
                //换模时间验证
                boolean flag = changeMoldTimeValidate(lhApsMoldAdjustPlan,importLogId,importErrorLogs, errorNum);
                if(!flag){
                    lhApsMoldAdjustPlan.setId(-999L);
                    failureNum++;
                }
            }*/
            //验证换模时间是否在班次范围内end
        }

        try {
//            //勾选更新记录，调用mergeOrInsert
//            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
//                successNum = importList.size();
//                    lhApsMoldAdjustPlanMapper.mergeSql(importList);
//            } else {
                //唯一则新增
                for (int i = 0; i < importList.size(); i++) {
                    LhApsMoldAdjustPlan lhApsMoldAdjustPlan = importList.get(i);
                    // 错误记录跳过
                    if (lhApsMoldAdjustPlan.getId() != null && lhApsMoldAdjustPlan.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkLhApsMoldAdjustPlanUnique(lhApsMoldAdjustPlan);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        lhApsMoldAdjustPlan.setDataSource(LhConstants.LH_APS_MOLD_PLAN_SOURCE_IMPORT);//设置数据来源为导入
                        String orderNo=lhCommonService.createOrderNo(LhConstants.MOLD_CHANG_ORDER_NO_PREFIX,planDate);
                        lhApsMoldAdjustPlan.setMoldOrderNo(orderNo);
                        successNum++;
                        lhApsMoldAdjustPlanMapper.insertLhApsMoldAdjustPlan(lhApsMoldAdjustPlan);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.datebaseUnique"), importErrorLogs);
                    }
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //Joran 2022-07-06 清空工序参数缓存
        clearLoadParams();
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     *  换模时间验证
     * @param lhApsMoldAdjustPlan
     * @param importLogId
     * @param importErrorLogs
     * @param errorNum
     * @return
     */
    private boolean changeMoldTimeValidate(LhApsMoldAdjustPlan lhApsMoldAdjustPlan, Long importLogId, List<ImportErrorLog> importErrorLogs, int errorNum) {
        boolean isSuccess=true;
        //换模时间
        Date changeMoldTime=lhApsMoldAdjustPlan.getChangeMoldTime();
        //计划日期
        Date planDate=lhApsMoldAdjustPlan.getPlanDate();
        //时间范围要在班次范围内即对应的计划日期的 中、夜、白班的时间范围
        if(planDate!=null && changeMoldTime!=null){
            planDate=LhEngineScheduleUtils.formatDateByZero(planDate);
            //中班开始时间
            Date classOneBeginTime=DateUtils.addHours(planDate, LhEngineConstants.CLASS_SHIFT_HOUR * 2);
            //白班对应的日期时间
            Date classThreeTimeDay=DateUtils.addDays(classOneBeginTime,1);
            //白班的结束时间
            Date classThreeEndTime=DateUtils.addSeconds(classThreeTimeDay,-1);
            if(changeMoldTime.getTime() < classOneBeginTime.getTime()){
                addImportErrorLog(importLogId, errorNum, I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeMoldTime.earlyBeginTime"), importErrorLogs);
                isSuccess = false;
            }
            if(changeMoldTime.getTime() > classThreeEndTime.getTime()){
                addImportErrorLog(importLogId, errorNum, I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeMoldTime.latterEndTime"), importErrorLogs);
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    /**
     * 根据 机台名称+左右模替换后字符串 为 Key,填充最大换模时间、是否需要换模
     * @param machineNameMaxChangeMoldTime 最大换模时间map (key:机台名称+左右模替换后字符串,value:分组内最大的换模时间)
     * @param isMaxChangeMoldTimeMap 是否需要换模map (key:机台名称+左右模替换后字符串,value:分组内的类型是否有计算过换模时间)
     * @param lhApsMoldAdjustPlan 单条记录
     */
    private void fillMapWithCondition(Map<String, Date> machineNameMaxChangeMoldTime, Map<String, Boolean> isMaxChangeMoldTimeMap, LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        String leftRightMold = lhApsMoldAdjustPlan.getLeftRightMold();
        if (StringUtils.isBlank(leftRightMold)) {
            return;
        }
        if (leftRightMold.contains("L")) {
            leftRightMold = leftRightMold.replace("L", "A");
        }
        if (leftRightMold.contains("R")) {
            leftRightMold = leftRightMold.replace("R", "A");
        }
        String key = lhApsMoldAdjustPlan.getLhMachineName() + leftRightMold;
        Date maxChangeMoldTime = lhApsMoldAdjustPlan.getChangeMoldTime();
        boolean isMaxChangeMoldTime = false;
        if (machineNameMaxChangeMoldTime.containsKey(key)) {
            Date changeMoldTime = machineNameMaxChangeMoldTime.get(key);
            if (maxChangeMoldTime == null) {
                maxChangeMoldTime = changeMoldTime;
            }else {
                maxChangeMoldTime = changeMoldTime.after(maxChangeMoldTime) ? changeMoldTime : maxChangeMoldTime;
            }
        }
        String changeType = lhApsMoldAdjustPlan.getChangeType();
        // 除了换模类型为左模合并、右模合并、拆模换、拆模合并，都有计算换模时间，有计算则要取最大换模时间
        if (!(MoldChangeTypeEnums.LEFT_MOLD_MERGE.getValue().equals(changeType) || MoldChangeTypeEnums.RIGHT_MOLD_MERGE.getValue().equals(changeType)
            || MoldChangeTypeEnums.SPLIT_OUT_CHANGE.getValue().equals(changeType) || MoldChangeTypeEnums.SPLIT_MOLD_MEGER.getValue().equals(changeType))) {
            isMaxChangeMoldTime = true;
        }
        if (maxChangeMoldTime != null) {
            machineNameMaxChangeMoldTime.put(key, maxChangeMoldTime);
        }
        if (!isMaxChangeMoldTimeMap.containsKey(key)) {
            isMaxChangeMoldTimeMap.put(key, isMaxChangeMoldTime);
        }
    }

    /**
     * 校验记录前后sap和胎胚关系是否对应
     */
    private void validateSapAndEmbryoRela(Long importLogId, Map<String, List<LhEngineTireConstructionInfo>> sapTireConstructionListMap, int errorNum, LhApsMoldAdjustPlan lhApsMoldAdjustPlan, MoldChangeTypeEnums moldChangeTypeEnums, List<ImportErrorLog> validated) {
        String beforeSapCode = lhApsMoldAdjustPlan.getBeforeSapCode();
        String beforeEmbryoCode = lhApsMoldAdjustPlan.getBeforeEmbryoCode();
        if (StringUtils.isNotBlank(beforeSapCode) && StringUtils.isNotBlank(beforeEmbryoCode)) {
            List<LhEngineTireConstructionInfo> beforeConstructionInfos = sapTireConstructionListMap.get(beforeSapCode);
            if (CollectionUtils.isEmpty(beforeConstructionInfos)) {
                String message = I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.beforeSapAndEmbryoCodeConstructionInfoNull");
                message = StringUtils.format(message, lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName(), beforeSapCode);
                addImportErrorLog(importLogId, errorNum, message, validated);
            }else {
                boolean flag = false;
                for (LhEngineTireConstructionInfo constructionInfo : beforeConstructionInfos) {
                    String embryoCode = constructionInfo.getEmbryoCode();
                    if (beforeEmbryoCode.equals(embryoCode)) {
                        lhApsMoldAdjustPlan.setBeforeSpecDesc(constructionInfo.getSpecDesc());
                        flag = true;
                    }
                }
                if (!flag) {
                    String message = I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.beforeSapAndEmbryoCodeCannotCorrespond");
                    message = StringUtils.format(message, lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName(), beforeSapCode);
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }
            }
        }
        String afterSapCode = lhApsMoldAdjustPlan.getAfterSapCode();
        String afterEmbryoCode = lhApsMoldAdjustPlan.getAfterEmbryoCode();
        if (StringUtils.isNotBlank(afterSapCode) && StringUtils.isNotBlank(afterEmbryoCode)) {
            List<LhEngineTireConstructionInfo> afterConstructionInfos = sapTireConstructionListMap.get(afterSapCode);
            if (CollectionUtils.isEmpty(afterConstructionInfos)) {
                String message = I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.afterSapAndEmbryoCodeConstructionInfoNull");
                message = StringUtils.format(message, lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName(), afterSapCode);
                addImportErrorLog(importLogId, errorNum, message, validated);
            }else {
                boolean flag = false;
                for (LhEngineTireConstructionInfo constructionInfo : afterConstructionInfos) {
                    String embryoCode = constructionInfo.getEmbryoCode();
                    if (afterEmbryoCode.equals(embryoCode)) {
                        lhApsMoldAdjustPlan.setAfterSpecDesc(constructionInfo.getSpecDesc());
                        flag = true;
                    }
                }
                if (!flag) {
                    String message = I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.afterSapAndEmbryoCodeCannotCorrespond");
                    message = StringUtils.format(message, lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName(), afterSapCode);
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }
            }
        }
    }

    /**
     * 收尾合并类型，使用较后收尾的规格需要硫化的胎胚总数，换模时间
     * 计算完所有换模时间后取出最大的换模时间
     * @param importList 要导入的数据
     */
    private void getMaxChangeMoldTime(List<LhApsMoldAdjustPlan> importList, Map<String, Date> machineNameMaxChangeMoldTime, Map<String, Boolean> isMaxChangeMoldTimeMap) {
        // 修改换模时间为分组内的最大换模时间
        for (LhApsMoldAdjustPlan apsMoldAdjustPlan : importList) {
            String leftRightMold = apsMoldAdjustPlan.getLeftRightMold();
            if (StringUtils.isBlank(leftRightMold)) {
                continue;
            }
            if (leftRightMold.contains("L")) {
                leftRightMold = leftRightMold.replace("L", "A");
            }
            if (leftRightMold.contains("R")) {
                leftRightMold = leftRightMold.replace("R", "A");
            }
            String key = apsMoldAdjustPlan.getLhMachineName() + leftRightMold;
            Boolean isNeedSetMax = isMaxChangeMoldTimeMap.getOrDefault(key, false);
            if (isNeedSetMax) {
                Date maxChangeMoldTime = machineNameMaxChangeMoldTime.get(key);
                if (maxChangeMoldTime != null) {
                    apsMoldAdjustPlan.setChangeMoldTime(maxChangeMoldTime);
                }
            }
        }
    }

    /**
     * 自定义验证
     * @param lhApsMoldAdjustPlan
     * @param importErrorLogs
     */
    private boolean customValidate(LhApsMoldAdjustPlan lhApsMoldAdjustPlan,Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap,Long importLogId, List<ImportErrorLog> importErrorLogs, Map<String, List<LhApsMoldAdjustPlan>> machineChangeTypeListMap, int errorNum) {
        String changeType= lhApsMoldAdjustPlan.getChangeType();
        Boolean flag=true;
        if(StringUtils.isNotEmpty(changeType)){
            MoldChangeTypeEnums moldChangeTypeEnums=MoldChangeTypeEnums.getMoldChangeTypeByValue(changeType);
            if(moldChangeTypeEnums==null){
                return false;
            }
            Integer useMoldNumber=null;
            Integer tireStock=null;
            Date changeMoldTime=null;
            String leftRightMold=null;
            String message="";
            // 用于判断规格数量，拆模合并类型判断换模时间是否保持一致
            List<LhApsMoldAdjustPlan> lhApsMoldAdjustPlans = machineChangeTypeListMap.get(lhApsMoldAdjustPlan.getLhMachineCode() + lhApsMoldAdjustPlan.getBeforeSapCode());
            int beforeSpecNum = lhApsMoldAdjustPlans.size();
            // 前sap和胎胚不能同时为空校验，后sap和胎胚不能同时为空校验
            if (StringUtils.isAllBlank(lhApsMoldAdjustPlan.getBeforeSapCode(), lhApsMoldAdjustPlan.getBeforeEmbryoCode(),
                    lhApsMoldAdjustPlan.getAfterSapCode(), lhApsMoldAdjustPlan.getAfterEmbryoCode())) {
                message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.sapAndEmbryoCodeCannotEmptyAtTheSameTime"),
                        lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                return false;
            }
            // 未填写使用模数默认为 2
            if (lhApsMoldAdjustPlan.getUseMoldNumber() == null) {
                lhApsMoldAdjustPlan.setUseMoldNumber(2);
            }
            switch (moldChangeTypeEnums){
                case LEFT_CLOSE_MERGE:
                case RIGHT_CLOSE_MERGE:
                case LEFT_POINT_MERGE:
                case RIGHT_POINT_MERGE:
                    //确认左模收尾合并、右模收尾合并、左模点数合并、右模点数合并类型导入时录入使用模数和需要硫化的胎胚总数量
                     useMoldNumber=lhApsMoldAdjustPlan.getUseMoldNumber();
                     tireStock=lhApsMoldAdjustPlan.getTireRoughStock();
                     leftRightMold = lhApsMoldAdjustPlan.getLeftRightMold();
                    if(useMoldNumber==null||tireStock==null){
                       message=StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.useMoldAndStockEmpty"),lhApsMoldAdjustPlan.getLhMachineName(),moldChangeTypeEnums.getZhName());
                       addImportErrorLog(importLogId,errorNum, message, importErrorLogs);
                       flag=false;
                    }else if(leftRightMold == null) {
                        message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.leftRightMoldEmpty"),
                                lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag = false;
                    }else{
                        //计算换模时间
                        Date moldTime = calcChangeMoldTime(lhApsMoldAdjustPlan, sapTireConstructionListMap, tireStock, useMoldNumber);
                        lhApsMoldAdjustPlan.setChangeMoldTime(moldTime);
                    }
                    break;
                case LEFT_MOLD_MERGE:
                case RIGHT_MOLD_MERGE:
                    //确认左模合并、右模合并类型导入时录入使用模数，换模时间为必输项
                     useMoldNumber=lhApsMoldAdjustPlan.getUseMoldNumber();
                     changeMoldTime=lhApsMoldAdjustPlan.getChangeMoldTime();
                    leftRightMold = lhApsMoldAdjustPlan.getLeftRightMold();
                    if(useMoldNumber==null||changeMoldTime==null){
                         message=StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.useMoldAndChangeTimeEmpty"),lhApsMoldAdjustPlan.getLhMachineName(),moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag=false;
                    }else if(leftRightMold == null) {
                        message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.leftRightMoldEmpty"),
                                lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag = false;
                    }
                    break;
                case CLOSE_OUT_CHANGE:
                case POINT_OUT_CHANGE:
                case CHANGE_ORDER_NO:
                    //确认收尾换、点数换、换工单号类型导入时，需要录入硫化的胎胚总数量
                    tireStock=lhApsMoldAdjustPlan.getTireRoughStock();
                    useMoldNumber = lhApsMoldAdjustPlan.getUseMoldNumber();
                    if(tireStock==null){
                        message=StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.stockEmpty"),lhApsMoldAdjustPlan.getLhMachineName(),moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag=false;
                    }else{
                        //计算换模时间
                        Date moldTime = calcChangeMoldTime(lhApsMoldAdjustPlan, sapTireConstructionListMap, tireStock, useMoldNumber);
                        lhApsMoldAdjustPlan.setChangeMoldTime(moldTime);
                    }
                    break;
                case SPLIT_OUT_CHANGE:
                    //拆模换类型导入时，需录入换模时间
                    changeMoldTime=lhApsMoldAdjustPlan.getChangeMoldTime();
                    if(changeMoldTime==null){
                        message=StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.changeTimeEmpty"),lhApsMoldAdjustPlan.getLhMachineName(),moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag=false;
                    }
                    break;
                case CLOSE_OUT_MEGER:
                    //TODO
                    // 1、收尾合并类型，导入时需录入使用模数，一个规格时，直接录入需要硫化的胎胚总数量
                    // 2、收尾合并类型，导入时需录入使用模数，两个规格时，需要录入两个前规格，并录入较后收尾的规格需要硫化的胎胚总数
                    useMoldNumber = lhApsMoldAdjustPlan.getUseMoldNumber();
                    tireStock = lhApsMoldAdjustPlan.getTireRoughStock();
                    leftRightMold = lhApsMoldAdjustPlan.getLeftRightMold();
                    if(useMoldNumber == null || tireStock == null) {
                        message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.useMoldAndStockEmpty"),
                                lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag = false;
                    }else if(leftRightMold == null) {
                        message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.leftRightMoldEmpty"),
                                lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag = false;
                    }else {
                        Date moldTime = calcChangeMoldTime(lhApsMoldAdjustPlan, sapTireConstructionListMap, tireStock, useMoldNumber);
                        lhApsMoldAdjustPlan.setChangeMoldTime(moldTime);
                    }
                    break;
                case SPLIT_MOLD_MEGER:
                    //TODO
                    // 3、拆模合并类型导入时，前规格为一个规格时直接录入换模时间，后规格需录入使用模数
                    // 4、拆模合并类型导入时，前规格为两个规格时直接录入的换模时间需保持一致，后规格需录入使用模数
                    boolean changeMoldTimeFlag = true;
                    useMoldNumber = lhApsMoldAdjustPlan.getUseMoldNumber();
                    leftRightMold = lhApsMoldAdjustPlan.getLeftRightMold();
                    if (beforeSpecNum == 1) {
                        changeMoldTime = lhApsMoldAdjustPlan.getChangeMoldTime();
                    }else if (beforeSpecNum == 2) {
                        for (LhApsMoldAdjustPlan apsMoldAdjustPlan : lhApsMoldAdjustPlans) {
                            if (changeMoldTime == null) {
                                changeMoldTime = apsMoldAdjustPlan.getChangeMoldTime();
                            }else {
                                changeMoldTimeFlag = changeMoldTime.equals(apsMoldAdjustPlan.getChangeMoldTime());
                            }
                        }
                    }
                    if(useMoldNumber == null || changeMoldTime == null) {
                        message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.useMoldAndChangeTimeEmpty"),
                                lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag = false;
                    }else if(leftRightMold == null) {
                        message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.leftRightMoldEmpty"),
                                lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag = false;
                    }
                    if (!changeMoldTimeFlag) {
                        message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeMoldTimeInconsistent"),
                                lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName(), lhApsMoldAdjustPlan.getBeforeSapCode());
                        addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                        flag = false;
                    }
                    break;
                default:
                    break;
            }

        }
        return flag;
    }

    /**
     * 计算换模时间
     * @param lhApsMoldAdjustPlan
     * @param sapTireConstructionListMap
     * @param tireStock
     * @param moldNum
     */
    private Date calcChangeMoldTime(LhApsMoldAdjustPlan lhApsMoldAdjustPlan, Map<String, List<LhEngineTireConstructionInfo>> sapTireConstructionListMap, Integer tireStock, Integer moldNum) {
        Date planDate=lhApsMoldAdjustPlan.getPlanDate();//下达日期
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(planDate);//设置日期

        calendar.set(Calendar.HOUR_OF_DAY,8);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        //Joran 2022-07-06 调整需求编号【20220705-03】,如果时间在16点之前的直接设定为16点
        Date shiftBeginTime=calendar.getTime();
        shiftBeginTime=DateUtils.addHours(shiftBeginTime,8);
        //获取单胎硫化时间
        Double singleTireTime=lhEngineTireConstructionInfoService.getSingleTireTimeBySap(lhApsMoldAdjustPlan.getBeforeSapCode(),lhApsMoldAdjustPlan.getBeforeEmbryoCode(),sapTireConstructionListMap);
        //获取胎胚库存数
        if(tireStock==null||tireStock<=0){
            log.debug("前规格没有库存数，是闲置机台更换模具时间从16点开始");
            lhApsMoldAdjustPlan.setChangeMoldTime(shiftBeginTime);
            return shiftBeginTime;
        }
        if(moldNum==null){
            log.debug("成型任务没有设置使用模数，更换模具时间从16点开始");
            lhApsMoldAdjustPlan.setChangeMoldTime(shiftBeginTime);
            return shiftBeginTime;
        }
        int singleShiftLhQty=calcSingleShiftLhQtyByMoldNum(singleTireTime,Double.valueOf(moldNum));
        while(tireStock >= singleShiftLhQty){
            //扣掉单班硫化量
            tireStock -= singleShiftLhQty;
            calendar.add(Calendar.HOUR,8);
        }
        //消耗总时间长度
        Double totalTime=0D;
        if(tireStock>0){
            //按单班硫化计算单胎时间+2分钟
            singleTireTime += lhCommonService.getBrushBagTime(cxParamsMap);
            if(tireStock % moldNum == 0){
                totalTime=singleTireTime * (tireStock / moldNum);
            }else{
                Integer remainStock=tireStock % moldNum;
                tireStock-=remainStock;
                //计算整除部分的时间+剩余部分的时间
                totalTime=singleTireTime * (tireStock / moldNum);
                //剩余时间
                Double remainQtyTime=remainStock * singleTireTime;
                totalTime+=remainQtyTime;
            }
        }
        //换算成秒
        Integer totalSecond=BigDecimal.valueOf(totalTime * 60D ).setScale(1, RoundingMode.UP).intValue();
        //加上总时间
        calendar.add(Calendar.SECOND,totalSecond);

        //计算结果小于16点的直接设置为16点
        Date changeMoldTime=calendar.getTime();
        if(changeMoldTime.getTime()<shiftBeginTime.getTime()){
            changeMoldTime=shiftBeginTime;
        }
        return changeMoldTime;
    }

    /**
     * 计算单班硫化量
     * @param singleLhTime
     * @param moldNum
     * @return
     */
    public Integer calcSingleShiftLhQtyByMoldNum(Double singleLhTime,Double moldNum) {
        //加载成型参数
        Map<String,String> cxParams=lhCommonService.loadCxParams();
        Integer shiftTime=lhCommonService.getShiftTime(cxParams);
        Integer brushBagTime=lhCommonService.getBrushBagTime(cxParams);
        BigDecimal moldNumDecimal=BigDecimal.valueOf(moldNum);
        BigDecimal singleShiftLhQtyDecimal=BigDecimal.valueOf((double)(shiftTime/(singleLhTime + brushBagTime))).setScale(0, BigDecimal.ROUND_DOWN).multiply(moldNumDecimal);
        return singleShiftLhQtyDecimal.intValue();
    }

    /**
     * 查询发布失败的记录数
     * @param ids id
     * @return
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return lhApsMoldAdjustPlanMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    @Override
    public int isPublishByIds(Long[] ids) {
        return lhApsMoldAdjustPlanMapper.isPublishByIds(ids);
    }

    @Override
    @Transactional
    public AjaxResult publish(long[] ids, Date planDate, String dataVersion, String factoryCode, String companyCode) {
        //数据同步
        lhApsMoldAdjustPlanMapper.deployMoldPlanToMes(dataVersion, ids, factoryCode, companyCode);

        //保存发布记录，更新发布状态
        MoldPlanPublishRecordVo record = new MoldPlanPublishRecordVo();
        record.setBaseVale(null);
        record.setPublishDate(planDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        lhApsMoldAdjustPlanMapper.insertPublishRecord(record);
        lhApsMoldAdjustPlanMapper.batchUpdate(ids, ApsConstant.RELEASING);
        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
    }

    /**
     * 更新发布记录数据
     * @param dataVersion 数据版本
     * @param ids         排程ID列表
     * @param status      更新的状态
     */
    @Override
    @Transactional
    public void updateRelaseStatus(String dataVersion, long[] ids, String status) {
        lhApsMoldAdjustPlanMapper.batchUpdate(ids, status);
        lhApsMoldAdjustPlanMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 根据ids更改执行状态
     *
     * @param lhApsMoldAdjustPlan ids、要更改的状态
     * @return 结果
     */
    @Override
    public AjaxResult changeExecute(LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        lhApsMoldAdjustPlanMapper.changeExecute(lhApsMoldAdjustPlan);
        return AjaxResult.success();
    }

    /**
     * 新增硫化工序模具变动单APS主子表
     *
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS主子表
     * @return 结果
     */
    @Override
    public AjaxResult addSubData(LhApsMoldAdjustPlanDto dto) {
        Date planDate = dto.getPlanDate();
        String lhMachineCode = dto.getLhMachineCode();
        String lhMachineName = dto.getLhMachineName();
        List<LhApsMoldAdjustPlan> apsMoldAdjustPlanList = dto.getApsMoldAdjustPlanList();
        Map<String, List<LhApsMoldAdjustPlan>> machineSapListMap = apsMoldAdjustPlanList.stream().peek(
                (item) -> {
                    item.setPlanDate(planDate);
                    item.setLhMachineCode(lhMachineCode);
                }
        ).collect(Collectors.groupingBy(item -> item.getLhMachineCode() + item.getBeforeSapCode()));
        // 最大换模时间分组：Key(机台名称 + 左右模), Value(分组后的最大换模时间) 将相同左右模的替换为A，方便分组
        Map<String, Date> machineNameMaxChangeMoldTime = new HashMap<>();
        // 是否要取最大换模时间分组：Key(机台名称 + 左右模), Value(分组后根据换模类型判断是否有计算过换模时间，计算过则true，否则false)
        Map<String, Boolean> isMaxChangeMoldTimeMap = new HashMap<>();
        //Joran 2022-07-06 加载工序参数
        loadParams();
        for (LhApsMoldAdjustPlan lhApsMoldAdjustPlan : apsMoldAdjustPlanList) {
            String changeType = lhApsMoldAdjustPlan.getChangeType();
            MoldChangeTypeEnums moldChangeTypeEnums = MoldChangeTypeEnums.getMoldChangeTypeByValue(changeType);
            if (moldChangeTypeEnums == null) {
                // 前端下拉选择，一般情况不可能为空
                String format = StringUtils.format("当前换模类型：{},为空或找不到对应的换模类型", changeType);
                log.error(format);
                throw new RuntimeException(format);
            }
            lhApsMoldAdjustPlan.setBaseVale(null);
            lhApsMoldAdjustPlan.setDataSource(LhConstants.LH_APS_MOLD_PLAN_SOURCE_ADD);//设置数据来源为新增
            lhApsMoldAdjustPlan.setIsExecute("0");
            // 工单号生成
            String orderNo = lhCommonService.createOrderNo(LhConstants.MOLD_CHANG_ORDER_NO_PREFIX, DateUtils.parseDateToStr("yyyy-MM-dd", lhApsMoldAdjustPlan.getPlanDate()));
            lhApsMoldAdjustPlan.setMoldOrderNo(orderNo);
            //硫化外胎施工信息start
            String beforeSapCode = lhApsMoldAdjustPlan.getBeforeSapCode();
            List<String> sapCodeList = new ArrayList<>();
            if (StringUtils.isNotBlank(beforeSapCode)) {
                sapCodeList.add(beforeSapCode);
            }
            String afterSapCode = lhApsMoldAdjustPlan.getAfterSapCode();
            sapCodeList.add(afterSapCode);
            LhEngineTireConstructionInfo condition = new LhEngineTireConstructionInfo();
            List<LhEngineTireConstructionInfo> constructionInfoList = new ArrayList<>();
            if(StringUtils.isNotEmpty(sapCodeList)) {
                condition.setSapCodeList(sapCodeList);
                constructionInfoList = lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(condition);
            }
            Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap = new HashMap<>();
            if(StringUtils.isNotEmpty(constructionInfoList)) {
                sapTireConstructionListMap = constructionInfoList.stream().collect(Collectors.groupingBy(LhEngineTireConstructionInfo::getSapCode));
            }
            //硫化外胎施工信息end
            // 回填规格信息 start
            List<LhEngineTireConstructionInfo> tireConstructionInfos = sapTireConstructionListMap.get(afterSapCode);
            if (CollectionUtils.isEmpty(tireConstructionInfos)) {
                String message = I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.afterSapAndEmbryoCodeConstructionInfoNull");
                message = StringUtils.format(message, lhMachineName, moldChangeTypeEnums.getZhName(), afterSapCode);
                throw new RuntimeException(message);
            }
            for (LhEngineTireConstructionInfo tireConstructionInfo : tireConstructionInfos) {
                String embryoCode = tireConstructionInfo.getEmbryoCode();
                if (embryoCode.equals(lhApsMoldAdjustPlan.getAfterEmbryoCode())) {
                    lhApsMoldAdjustPlan.setAfterSpecDesc(tireConstructionInfo.getSpecDesc());
                }
            }
            if (StringUtils.isBlank(lhApsMoldAdjustPlan.getAfterSpecDesc())) {
                String message = I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.afterSapAndEmbryoCodeCannotCorrespond");
                message = StringUtils.format(message, lhMachineName, moldChangeTypeEnums.getZhName(), afterSapCode);
                throw new RuntimeException(message);
            }
            if (CollectionUtils.isEmpty(tireConstructionInfos)) {
                String message = I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.afterSapAndEmbryoCodeConstructionInfoNull");
                message = StringUtils.format(message, lhMachineName, moldChangeTypeEnums.getZhName(), afterSapCode);
                throw new RuntimeException(message);
            }
            if (StringUtils.isNotBlank(beforeSapCode)) {
                tireConstructionInfos = sapTireConstructionListMap.get(beforeSapCode);
                for (LhEngineTireConstructionInfo tireConstructionInfo : tireConstructionInfos) {
                    String embryoCode = tireConstructionInfo.getEmbryoCode();
                    if (embryoCode.equals(lhApsMoldAdjustPlan.getBeforeEmbryoCode())) {
                        lhApsMoldAdjustPlan.setBeforeSpecDesc(tireConstructionInfo.getSpecDesc());
                    }
                }
                if (StringUtils.isBlank(lhApsMoldAdjustPlan.getBeforeSpecDesc())) {
                    String message = I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.beforeSapAndEmbryoCodeCannotCorrespond");
                    message = StringUtils.format(message, lhMachineName, moldChangeTypeEnums.getZhName(), beforeSapCode);
                    throw new RuntimeException(message);
                }
            }
            // 回填规格信息 end
            List<ImportErrorLog> importErrorLogs = new ArrayList<>();
            boolean flag = customValidate(lhApsMoldAdjustPlan,sapTireConstructionListMap, null, importErrorLogs, machineSapListMap, 0);
            if(!flag){
                String errorDetail = importErrorLogs.get(0).getErrorDetail();
                return AjaxResult.error(errorDetail);
            }
            // 根据分组条件填充 map
            fillMapWithCondition(machineNameMaxChangeMoldTime, isMaxChangeMoldTimeMap, lhApsMoldAdjustPlan);
        }
        //Joran 2022-07-06 清空工序参数缓存
        clearLoadParams();
        // 计算完所有换模时间后取出最大的换模时间
        getMaxChangeMoldTime(apsMoldAdjustPlanList, machineNameMaxChangeMoldTime, isMaxChangeMoldTimeMap);
        for (LhApsMoldAdjustPlan lhApsMoldAdjustPlan : apsMoldAdjustPlanList) {
            lhApsMoldAdjustPlanMapper.insertLhApsMoldAdjustPlan(lhApsMoldAdjustPlan);
        }
        return AjaxResult.success();
    }

    /**
     * 加载成型工序参数
     */
    private void loadParams(){
        cxParamsMap=new HashMap<>();
        cxParamsMap=lhCommonService.loadCxParams();
    }

    /**
     * 清空成型工序参数缓存
     */
    public void clearLoadParams(){
        cxParamsMap=null;
    }
}
