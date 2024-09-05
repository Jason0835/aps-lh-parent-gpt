package com.zlt.aps.lh.engine.task;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.engine.common.LhCommonService;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.domain.LhEngineAutoScheduleRecord;
import com.zlt.aps.lh.engine.domain.LhEngineScheduleResult;
import com.zlt.aps.lh.engine.mapper.CommonCxEngineMapper;
import com.zlt.aps.lh.engine.mapper.CommonLhEngineMapper;
import com.zlt.aps.lh.engine.service.LhEngineAutoScheduleRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
  * 硫化引擎中校验方法
  * @ClassName LhScheduleTaskCheck
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/8/9 11:04
  * @Version 1.0
**/
@Component("lhScheduleTaskCheck")
@Slf4j
public class LhScheduleTaskCheck {

    @Autowired
    private CommonCxEngineMapper commonCxEngineMapper;
    @Autowired
    private LhCommonService lhCommonService;
    @Autowired
    private LhEngineAutoScheduleRecordService lhEngineAutoScheduleRecordService;

    @Autowired
    private CommonLhEngineMapper commonLhEngineMapper;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;
    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    /**
     *  验证硫化入参
     * @param lhScheduleResultDto
     * @return
     */
    public void validateInsertParams(LhScheduleResultDto lhScheduleResultDto,StringBuilder errorMsg){
        if(lhScheduleResultDto==null){
            //【参数验证】入参为空！
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.params.empty.error"));
            return;
        }

        Date scheduleDate=lhScheduleResultDto.getScheduleDate();
        if(scheduleDate==null){
            //【参数验证】排程日期为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.scheduleDate.empty.error"));
        }

        String sapCode=lhScheduleResultDto.getSapCode();
        if(StringUtils.isEmpty(sapCode)){
            //【参数验证】SAP品号为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.sapCode.empty.error"));
        }
        String machineCode=lhScheduleResultDto.getLhMachineCode();
        if(StringUtils.isEmpty(machineCode)){
            //【参数验证】硫化机台编号为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.machineCode.empty.error"));
        }

       /* String stockArea=lhScheduleResultDto.getStockArea();
        if(StringUtils.isEmpty(stockArea)){
            //【参数验证】库存地点为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.stockArea.empty.error"));
        }
*/
        //计划量格式化，若为空值则默认转为0
        Integer totalPlanQty=formatterPlanQty(lhScheduleResultDto);
        if(totalPlanQty<=0){
            //【参数验证】库存地点为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.insert.planQty.limit.error"));
        }
        //日计划总量
        lhScheduleResultDto.setDailyPlanQty(totalPlanQty);

    }

    /**
     * 格式化并计算总计划量
     * @param lhScheduleResultDto
     * @return
     */
    private Integer formatterPlanQty(LhScheduleResultDto lhScheduleResultDto) {
        Integer totalPlanQty=0;
        if(lhScheduleResultDto.getClass1PlanQty()==null){
            lhScheduleResultDto.setClass1PlanQty(0);
        }
        totalPlanQty+=lhScheduleResultDto.getClass1PlanQty();
        if(lhScheduleResultDto.getClass2PlanQty()==null){
            lhScheduleResultDto.setClass2PlanQty(0);
        }
        totalPlanQty+=lhScheduleResultDto.getClass2PlanQty();
        if(lhScheduleResultDto.getClass3PlanQty()==null){
            lhScheduleResultDto.setClass3PlanQty(0);
        }
        totalPlanQty+=lhScheduleResultDto.getClass3PlanQty();
        return  totalPlanQty;
    }

    /**
     * 验证自动排程抓取记录
     * @param lhScheduleResultDto
     * @param scheduleDate
     * @param errorMsg
     */
    public void checkLhAutoScheduleRecord(LhScheduleResultDto lhScheduleResultDto,String scheduleDate, StringBuilder errorMsg,StringBuilder tipMsg) {
        //判断成型自动排程抓取记录
        CxScheduleResult condition=new CxScheduleResult();
        condition.setScheduleDateStr(scheduleDate);
        List<CxScheduleResult> cxScheduleResultList=this.commonCxEngineMapper.selectCxScheduleResultList(condition);
        if(StringUtils.isEmpty(cxScheduleResultList)){
            //没有相应的成型排程数据
            errorMsg.append(I18nUtil.getMessage("lh.engine.cxSchedule.result.empty.error"));
            return;
        }
        //获取成型批次号
        CxScheduleResult cxScheduleResult=cxScheduleResultList.get(0);
        String cxBatchNo=cxScheduleResult.getCxBatchNo();
        lhScheduleResultDto.setCxBatchNo(cxBatchNo);//验证通过后获取成型批次号
        LhEngineAutoScheduleRecord recordCondition =new LhEngineAutoScheduleRecord();
        recordCondition.setCxBatchNo(cxBatchNo);
        List<LhEngineAutoScheduleRecord> recordList =lhEngineAutoScheduleRecordService.selectLhEngineAutoScheduleRecordList(recordCondition);
        //没有硫化自动排程抓取的记录，则进行自动排程抓取记录生成
        if(StringUtils.isEmpty(recordList)){
            tipMsg.append(I18nUtil.getMessage("lh.engine.record.empty.tip"));
        }else{
            LhEngineAutoScheduleRecord record=recordList.get(0);
            //对批次号进行赋值
            lhScheduleResultDto.setBatchNo(record.getLhBatchNo());
        }
    }

    /**
     * 创建排程抓取记录
     * @param cxBatchNo
     * @param lhBatchNo
     * @param scheduleDate
     * @param status
     */
    public void createRecord(String cxBatchNo, String lhBatchNo, String scheduleDate, String status){
        lhEngineAutoScheduleRecordService.reGenerageRecord(cxBatchNo,lhBatchNo,scheduleDate,status);
    }

    /**
     * 重复插单校验
     * @param lhScheduleResultDto
     * @param scheduleDate
     * @param errorMsg
     */
    public void reInsertCheck(LhScheduleResultDto lhScheduleResultDto, String scheduleDate, StringBuilder errorMsg) {
        String lhMachineCode=lhScheduleResultDto.getLhMachineCode();
        String sapCode=lhScheduleResultDto.getSapCode();
        LhEngineScheduleResult condition=new LhEngineScheduleResult();
        condition.setLhScheduleDate(scheduleDate);
        condition.setLhMachineCode(lhMachineCode);
        condition.setSapCode(sapCode);
        List<LhEngineScheduleResult> existList=this.commonLhEngineMapper.selectLhEngineScheduleResultList(condition);
        if(StringUtils.isNotEmpty(existList)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.insert.repeat.check.error"));
        }
    }

    /**
     * 转机台入参验证
     * @param lhScheduleResultDto
     * @param errorMsg
     */
    public void validateChangeMachineParams(LhScheduleResultDto lhScheduleResultDto,StringBuilder errorMsg){
        if(lhScheduleResultDto==null){
            //【参数验证】入参为空！
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.params.empty.error"));
            return;
        }

        String machineCode=lhScheduleResultDto.getLhMachineCode();
        if(StringUtils.isEmpty(machineCode)){
            //【参数验证】硫化机台编号为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.machineCode.empty.error"));
        }
    }

    /**
     * 转机台验证转入机台编码
     * @param changeMachineCode
     * @param errorMsg
     */
    public void changeMachineCodeValidate(String changeMachineCode,StringBuilder errorMsg){
        //验证机台编号是否为空
        if(StringUtils.isEmpty(changeMachineCode)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.changeMachine.changeMachineCode.empty.error"));
            return;
        }
        List<LhMachineInfo> lhMachineInfoList=loadAvailableMachine(changeMachineCode);
        if(StringUtils.isEmpty(lhMachineInfoList)){
          errorMsg.append(I18nUtil.getMessage("lh.engine.changeMachine.machine.info.empty.error"));
        }
    }

    /**
     * 加载可用状态的硫化机
     * @param machineCode
     * @return
     */
    public List<LhMachineInfo> loadAvailableMachine(String machineCode){
        //2.验证机台是否存在
        LhMachineInfo condition=new LhMachineInfo();
        if(StringUtils.isNotEmpty(machineCode)){
            condition.setMachineCode(machineCode);
        }
        condition.setStatus(LhEngineConstants.MACHINE_STATUS_ENABLE);//启用状态
        List<LhMachineInfo> lhMachineInfoList=commonLhEngineMapper.selectMachineInfoList(condition);
        return lhMachineInfoList;
    }

    /**
     * 获取硫化机集合
     * @param machineCode
     * @return
     */
    public Map<String,LhMachineInfo> getLhMachineInfoMap(String machineCode){
        Map<String,LhMachineInfo> lhMachineInfoMap= new HashMap<>();
        List<LhMachineInfo> list =loadAvailableMachine(machineCode);
        if(StringUtils.isNotEmpty(list)){
            //list 转map 如果集合对象有重复的key，会报错Duplicate key，可以用 (lhMachineCode,lhMachineCodeRepeat)->lhMachineCode 来设置，如果有重复的key,则保留lhMachineCode,舍弃lhMachineCodeRepeat
            lhMachineInfoMap= list.stream().collect(Collectors.toMap(LhMachineInfo::getMachineCode, lhMachineInfo -> lhMachineInfo,(lhMachineCode,lhMachineCodeRepeat)->lhMachineCode));
        }
        return lhMachineInfoMap;
    }

    /**
     * 根据排程日期进行排程转移排程日志表，删除排程表结果
     * @param scheduleDate
     */
    public void syncScheduleToLog(String scheduleDate){
        log.debug("【排程数据迁移日志表】，开始将排程日期【"+scheduleDate+"】,转移到日志表");
        commonLhEngineMapper.syncLhScheduleToLog(scheduleDate);
        commonLhEngineMapper.deleteLhSchedule(scheduleDate);
    }

    /**
     * //通过硫化施工信息获取胎胚到施工获取施工信息
     * @param lhScheduleResultDto
     * @param errorMsg  错误信息
     * @return
     */
    public void validateSapCode(LhScheduleResultDto lhScheduleResultDto,StringBuilder errorMsg) {
        LhEngineTireConstructionInfo condition=new LhEngineTireConstructionInfo();
        condition.setSapCode(lhScheduleResultDto.getSapCode());
        List<LhEngineTireConstructionInfo> constructionInfoList=lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(condition);
        if(StringUtils.isEmpty(constructionInfoList)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.construction.info.empty.error"));
            return;
        }
        //获取胎胚施工信息
        Map<String, EngineProductConstructionInfo> engineConstructionInfoMap=cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
        if(StringUtils.isEmpty(engineConstructionInfoMap)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.embryoCode.construction.info.empty.error"));
            return;
        }
        //Joran 2021-12-08遍历硫化施工进行获取投产施工start
        EngineProductConstructionInfo embryoConstructionInfo=null;
        for (LhEngineTireConstructionInfo lhEngineTireConstructionInfo:constructionInfoList){
            String key = GenerageMapKeyUtils.createMapKey(lhEngineTireConstructionInfo.getEmbryoCode(),lhEngineTireConstructionInfo.getEmbryoVersion());
            if(engineConstructionInfoMap.containsKey(key)){
                embryoConstructionInfo=engineConstructionInfoMap.get(key);
                //Joran 2022-01-03 调整获取规格型号从硫化施工表中进行获取
                lhScheduleResultDto.setSpecDesc(lhEngineTireConstructionInfo.getSpecDesc());
                break;
            }
        }
        //Joran 2021-12-08遍历硫化施工进行获取投产施工end
        if(embryoConstructionInfo==null){
            errorMsg.append(I18nUtil.getMessage("lh.engine.embryoCode.construction.info.empty.error"));
            return;
        }
        //Joran 2021-12-02 Joran 调整获取胎胚施工
        //lhScheduleResultDto.setSpecDesc(embryoConstructionInfo.getSpecDesc());

    }

    /**
     * 优化导入校验
     * @param lhScheduleResultDto
     * @param sapTireConstructionListMap
     * @param engineConstructionInfoMap
     * @param errorMsg
     */
    public void validateSapCodeByConstruction(LhScheduleResultDto lhScheduleResultDto,Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap,Map<String, EngineProductConstructionInfo> engineConstructionInfoMap,StringBuilder errorMsg) {
        String sapCode=lhScheduleResultDto.getSapCode();
        if(StringUtils.isEmpty(sapTireConstructionListMap)&&!sapTireConstructionListMap.containsKey(sapCode)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.construction.info.empty.error"));
            return;
        }
        List<LhEngineTireConstructionInfo> constructionInfoList=sapTireConstructionListMap.get(sapCode);

        if(StringUtils.isEmpty(constructionInfoList)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.embryoCode.construction.info.empty.error"));
            return;
        }
        //Joran 2021-12-08遍历硫化施工进行获取投产施工start
        EngineProductConstructionInfo embryoConstructionInfo=null;
        for (LhEngineTireConstructionInfo lhEngineTireConstructionInfo:constructionInfoList){
            String key = GenerageMapKeyUtils.createMapKey(lhEngineTireConstructionInfo.getEmbryoCode(),lhEngineTireConstructionInfo.getEmbryoVersion());
            if(engineConstructionInfoMap.containsKey(key)){
                embryoConstructionInfo=engineConstructionInfoMap.get(key);
                //Joran 2022-01-03 调整获取规格型号从硫化施工表中进行获取
                lhScheduleResultDto.setSpecDesc(lhEngineTireConstructionInfo.getSpecDesc());
                break;
            }
        }
        //Joran 2021-12-08遍历硫化施工进行获取投产施工end
        if(embryoConstructionInfo==null){
            errorMsg.append(I18nUtil.getMessage("lh.engine.embryoCode.construction.info.empty.error"));
            return;
        }
        //Joran 2021-12-02 Joran 调整获取胎胚施工
        //lhScheduleResultDto.setSpecDesc(embryoConstructionInfo.getSpecDesc());

    }

}
