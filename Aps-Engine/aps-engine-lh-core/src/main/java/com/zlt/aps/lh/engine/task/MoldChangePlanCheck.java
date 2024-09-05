package com.zlt.aps.lh.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.engine.common.LhCommonService;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.domain.LhEngineMoldChangePlan;
import com.zlt.aps.lh.engine.domain.MoldEngineAutoGenerageRecord;
import com.zlt.aps.lh.engine.enums.TaskTypeEnum;
import com.zlt.aps.lh.engine.mapper.CommonCxEngineMapper;
import com.zlt.aps.lh.engine.service.LhEngineMoldChangePlanService;
import com.zlt.aps.lh.engine.service.MoldEngineAutoGenerageRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 模具变动单检查
 */
@Component("moldChangePlanCheck")
@Slf4j
public class MoldChangePlanCheck {

    @Autowired
    private CommonCxEngineMapper  commonCxEngineMapper;

    @Autowired
    private MoldEngineAutoGenerageRecordService moldEngineAutoGenerageRecordService;

    @Autowired
    private LhEngineMoldChangePlanService lhEngineMoldChangePlanService;
    @Autowired
    private IncrementService incrementService;

    @Autowired
    private LhCommonService lhCommonService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * 根据排程日期获取成型抓取记录中对应的生产版本信息
     * @param scheduleDate
     * @return
     */
    public String getCxScheduleRecordCxBatchNo(String scheduleDate,StringBuilder errorMsg){
        String cxBatchNo=commonCxEngineMapper.selectCxBatchNoByScheduleDate(scheduleDate);
        if(StringUtils.isEmpty(cxBatchNo)){
            //自动排程尚未抓取无法获取成型批次号
            errorMsg.append(I18nUtil.getMessage("lh.engine.mold.plan.cx.record.empty.error"));
        }
        return cxBatchNo;
    }

    /**
     * 根据排程日期获取成型排程结果任务状态为待换模的数据列表
     * @param scheduleDate
     * @param errorMsg
     * @return
     */
    public List<CxScheduleResult> selectCxScheduleResultList(String scheduleDate,StringBuilder errorMsg){
        List<CxScheduleResult> cxScheduleResultList=null;
        CxScheduleResult condition=new CxScheduleResult();
        condition.setTaskType(TaskTypeEnum.MOLD.getTaskType());//待换模
        condition.setScheduleDateStr(scheduleDate);//排程日期
        cxScheduleResultList=this.commonCxEngineMapper.selectCxScheduleResultList(condition);
        if(StringUtils.isEmpty(cxScheduleResultList)){
            //自动排程尚未抓取无法获取成型批次号
            //errorMsg.append(I18nUtil.getMessage("lh.engine.cx.scheduleResult.empty.error"));
        }
        return cxScheduleResultList;
    }

    /**
     * 根据条件查询成型排程结果列表
     * @param scheduleDate
     * @param taskType
     * @param productStatus
     * @return
     */
    public List<CxScheduleResult> selectBeforeCxScheduleResultList(String scheduleDate,String taskType,String productStatus){
        List<CxScheduleResult> cxScheduleResultList=null;
        CxScheduleResult condition=new CxScheduleResult();
        if(StringUtils.isNotEmpty(taskType)){
            condition.setTaskType(taskType);//投产中
        }
        if(StringUtils.isNotEmpty(scheduleDate)){
            condition.setScheduleDateStr(scheduleDate);//排程日期
        }

        if(StringUtils.isNotEmpty(productStatus)){
            condition.setProductionStatus(productStatus);//生产状态
        }

        cxScheduleResultList=this.commonCxEngineMapper.selectCxScheduleResultList(condition);
        return cxScheduleResultList;
    }

    /**
     * 获取排程日期及之前的收尾成型排程列表
     * @param lhMachineChangeMoldPlanMap
     * @param scheduleDate
     * @return
     */
    public List<CxScheduleResult> selectBeforeCloseOutCxScheduleList(Map<String, LhEngineMoldChangePlan> lhMachineChangeMoldPlanMap,String scheduleDate){
        List<CxScheduleResult> beforeCloseOutList =new ArrayList<>();
        if(StringUtils.isNotEmpty(lhMachineChangeMoldPlanMap)){
            //遍历所有的硫化机台进行收尾规格查询
            CxScheduleResult condition=null;
            for(Map.Entry<String, LhEngineMoldChangePlan> entry:lhMachineChangeMoldPlanMap.entrySet()){
                //获取成型排程选择的硫化机台
                String lhMachineCode=entry.getKey();
                condition=new CxScheduleResult();
                condition.setLhMachineCode(lhMachineCode);
                condition.setEndTime(scheduleDate);
                condition.setLimit(10);//限制最多查询10条最近的记录
                condition.setSortByScheduleDate("desc");
                condition.setProductionStatus(LhEngineConstants.CX_PRODUCT_STATUS_CLOSE_OUT);
                List<CxScheduleResult> cxScheduleResultList=this.commonCxEngineMapper.selectCxScheduleResultList(condition);
                if(StringUtils.isNotEmpty(cxScheduleResultList)){
                    CxScheduleResult lastCloseOutResult=cxScheduleResultList.get(0);
                    beforeCloseOutList.add(lastCloseOutResult);
                }
            }
        }
        return beforeCloseOutList;
    }



    /**
     * 模具变动单数据校验
     * @param lhEngineMoldChangePlan
     * @return
     */
    public void validateMoldPlanData(LhMoldChangePlan lhEngineMoldChangePlan, StringBuilder errorMsg, StringBuilder tipMsg) {
        if(StringUtils.isEmpty(lhEngineMoldChangePlan.getLhMachineCode())){
            errorMsg.append(I18nUtil.getMessage("lh.engine.auto.lhMachineCode.empty.error"));
        }
        //验证后规格sap品号是否为空
        if(StringUtils.isEmpty(lhEngineMoldChangePlan.getAfterSapCode())){
            errorMsg.append(I18nUtil.getMessage("lh.engine.auto.afterSapCode.empty.error"));
        }
        //TODO 预留校验SAP品号是否有效
        String msg=validateSapCode(lhEngineMoldChangePlan.getAfterSapCode());
        if(StringUtils.isNotEmpty(msg)){
            errorMsg.append(msg);
        }
        //验证更换类型
        if(StringUtils.isEmpty(lhEngineMoldChangePlan.getChangeType())){
            errorMsg.append(I18nUtil.getMessage("lh.engine.auto.changeType.empty.error"));
        }
        //查询模具变动单是否已经生成
        MoldEngineAutoGenerageRecord recordCondition=new MoldEngineAutoGenerageRecord();
        recordCondition.setAutoScheduleDate(DateUtils.getDate());//当前日期模具变动单生成记录
        recordCondition.setStatus(LhEngineConstants.LH_AUTO_RECORD_STATUS_SUCCESS);
        List<MoldEngineAutoGenerageRecord> list =moldEngineAutoGenerageRecordService.selectMoldEngineAutoGenerageRecordList(recordCondition);
        if(StringUtils.isEmpty(list)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.auto.record.empty.error"));
        }else{
            MoldEngineAutoGenerageRecord record =list.get(0);
            lhEngineMoldChangePlan.setMoldBatchNo(record.getMoldBatchNo());//重新赋值模具变动批次号
        }
        //校验通过进行根据硫化机台编号查询模具变动单
        if(StringUtils.isEmpty(errorMsg)){
            LhEngineMoldChangePlan condition=new LhEngineMoldChangePlan();
            condition.setLhMachineCode(lhEngineMoldChangePlan.getLhMachineCode());
            List<LhEngineMoldChangePlan> planList=this.lhEngineMoldChangePlanService.selectLhEngineMoldChangePlanList(condition);
            if(StringUtils.isNotEmpty(planList)){
                tipMsg.append(I18nUtil.getMessage("lh.engine.auto.lhMachineCode.exist.tip"));
            }
        }

    }

    /**
     * 计算更换时间
     * 2021-09-15 计算调整
     * 》1.如果库存量超过单班硫化量 》 库存=库存-单班硫化量；时间+8个小时
     *                             不够一个班次的 直接按 硫化时长*库存数量/模数=总时长，从早上8点加上总时长
     *  2.如果没有前规格或者前规格库存为0的更换时间为当前日期早上8点
     * @param lhMoldChangePlan
     */
    public void calcChangeTime(String scheduleDate, LhEngineMoldChangePlan lhMoldChangePlan) {
        //从当天8点开始计算
        Date date= DateUtils.parseDate(scheduleDate);
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(date);//设置日期
        calendar.set(Calendar.HOUR_OF_DAY,8);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        //获取单条胎硫化时长
        Double singleTime= lhMoldChangePlan.getLhSingleTireTime();
        //Joran 2021-11-02 当没有前规格单条硫化时长，则重新获取单胎硫化时长start
        if(singleTime==null){
            singleTime= lhCommonService.getSingleLhTime(lhMoldChangePlan.getBeforeSapCode());
        }
        //Joran 2021-11-02 当没有前规格单条硫化时长，则重新获取单胎硫化时长end
        //前规格胎胚占比库存
        Integer stock =lhMoldChangePlan.getTireRoughStock();
        if(stock==null||stock<=0){
            log.debug("前规格没有库存数，是闲置机台更换模具时间从8点开始");
            lhMoldChangePlan.setChangeTime(calendar.getTime());
            return;
        }
        //计算单班硫化量
       Integer moldNum=lhMoldChangePlan.getUseMoldNum();
       if(moldNum==null){
           log.debug("成型任务没有设置使用模数，更换模具时间从8点开始");
           lhMoldChangePlan.setChangeTime(calendar.getTime());
           return;
       }
       int singleShiftLhQty=calcSingleShiftLhQtyByMoldNum(singleTime,Double.valueOf(moldNum));
       while(stock >= singleShiftLhQty){
           //扣掉单班硫化量
           stock -= singleShiftLhQty;
           calendar.add(Calendar.HOUR,8);
       }
        //消耗总时间长度
        Double totalTime=0D;
       if(stock>0){
           //按单班硫化计算单胎时间+2分钟
           singleTime+=2;
           if(stock%moldNum==0){
               totalTime=singleTime * (stock / moldNum);
           }else{
               Integer remainStock=stock%moldNum;
               stock-=remainStock;
               //计算整除部分的时间+剩余部分的时间
               totalTime=singleTime * (stock / moldNum);
               //剩余时间
               Double remainQtyTime=remainStock * singleTime;
               totalTime+=remainQtyTime;
           }
       }
       //换算成秒
        Integer totalSecond=BigDecimal.valueOf(totalTime * 60D ).setScale(1, RoundingMode.UP).intValue();
        //加上总时间
        calendar.add(Calendar.SECOND,totalSecond);
        Date afterDate=calendar.getTime();
        lhMoldChangePlan.setChangeTime(afterDate);
    }

    /**
     * 计算单班硫化量
     * @param singleLhTime
     * @param moldNum
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
     * 构建硫化机对应的成型排程结果集合
     * @param beforeSpecDoingList
     * @param lhMachineResultMap
     */
    public void bulidLhMachineCodeResultMap(List<CxScheduleResult> beforeSpecDoingList, Map<String, CxScheduleResult> lhMachineResultMap) {
        for(CxScheduleResult cxScheduleResult:beforeSpecDoingList){
            if(StringUtils.isEmpty(cxScheduleResult.getLhMachineCode())){
                log.debug("硫化机台编号为空，异常数据，不做处理！");
                continue;
            }
            String [] lhMachineCodes=StringUtils.split(cxScheduleResult.getLhMachineCode(),",");
            for(String lhMachCode:lhMachineCodes){
                if(StringUtils.isEmpty(lhMachCode)){
                    continue;
                }
                lhMachineResultMap.put(lhMachCode,cxScheduleResult);
            }
        }
    }

    /**
     * 根据硫化机硫化机规格进行拆分
     * @param changeTypeMap 硫化机更换类型集合
     * @param lhMachineChangeMoldDesc 确定硫化机选定的更换类型具体描述
     */
    public Map<String,Integer> splitChangeTypeMap(Map<String,String> changeTypeMap,String lhMachineChangeMoldDesc) {
        Map<String,Integer> machineCodeMoldMap=new HashMap<>();
        if(StringUtils.isNotEmpty(lhMachineChangeMoldDesc)){
            //成型排程项选定硫化机台及更换类型和使用模数栏位
            String[] lhMachinechangeMoldDescArr =lhMachineChangeMoldDesc.split(";");
            for(String lhMachineChageMoldDesc:lhMachinechangeMoldDescArr){
                String[] machineAndTypeArr=lhMachineChageMoldDesc.split(":");
                //硫化机台
                String machineCode=machineAndTypeArr[0];
                //模具更换类型
                String changeMoldType=machineAndTypeArr[1];
                //使用模数
                if(machineAndTypeArr.length>2){
                    String moldNumStr=machineAndTypeArr[2];
                    machineCodeMoldMap.put(machineCode,Integer.valueOf(moldNumStr));
                }
                changeTypeMap.put(machineCode,changeMoldType);
            }
        }
        return machineCodeMoldMap;
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    public String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence4(LhEngineConstants.LH_MOLD_BATCH_NO_PREFIX + scheduleDate);
    }

    /**
     * 拼接原始成型工单号数据
     * @param lhEngineMoldChangePlan
     * @param orderNo
     */
    public void setSourceCxOrder(LhEngineMoldChangePlan lhEngineMoldChangePlan, String orderNo) {
        String cxOrderNo=lhEngineMoldChangePlan.getSourceCxOrder();
        if(StringUtils.isEmpty(cxOrderNo)){
            cxOrderNo=orderNo;
        }else{
            if(!cxOrderNo.contains(orderNo)){
                cxOrderNo+=";"+orderNo;
            }
        }
        lhEngineMoldChangePlan.setSourceCxOrder(cxOrderNo);
    }

    /**
     * //TODO 预留通过BOM信息获取胎胚到施工获取施工信息
     * @param afterSapCode
     * @return
     */
    private String validateSapCode(String afterSapCode) {
        return "";
    }

    /**
     * 单记录生成模具变动单参数验证
     * @param cxScheduleResult
     * @errorMsg 错误信息
     */
    public void singleMoldChangePlanCheck(CxScheduleResult cxScheduleResult,StringBuilder errorMsg) {
        if(cxScheduleResult==null){
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.params.empty.error"));
            return;
        }
        Date scheduleDate=cxScheduleResult.getScheduleDate();
        if(scheduleDate==null){
            //【参数验证】排程日期为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.scheduleDate.empty.error"));
        }

        String cxBatchNo=cxScheduleResult.getCxBatchNo();
        if(StringUtils.isEmpty(cxBatchNo)){
            //【参数验证】成型批次号为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.cxBatchNo.empty.error"));
        }

        String orderNo=cxScheduleResult.getOrderNo();
        if(StringUtils.isEmpty(orderNo)){
            //【参数验证】工单号为空
            errorMsg.append(I18nUtil.getMessage("lh.engine.input.orderNo.empty.error"));
        }

    }

    /**
     * 获取模具变动单对应的批次号信息
     * @param cxBatchNo 成型批次号
     * @param scheduleDate 成型排程日期
     * @return 模具变动单批次号
     */
    public String getMoldBatchNo(String cxBatchNo,String scheduleDate){
      String moldBatchNo="";
      MoldEngineAutoGenerageRecord condition=new MoldEngineAutoGenerageRecord();
      condition.setAutoScheduleDate(scheduleDate);
      List<MoldEngineAutoGenerageRecord> recordList=moldEngineAutoGenerageRecordService.selectMoldEngineAutoGenerageRecordList(condition);
      if(StringUtils.isEmpty(recordList)){
         moldBatchNo=createBatchNo(scheduleDate);
         moldEngineAutoGenerageRecordService.reGenerageRecord(cxBatchNo,moldBatchNo,scheduleDate, LhEngineConstants.LH_AUTO_RECORD_STATUS_SUCCESS);
      }else{
          MoldEngineAutoGenerageRecord record=recordList.get(0);
          moldBatchNo=record.getMoldBatchNo();
      }
      return moldBatchNo;
    }

    /**
     * 验证通过标记
     * @param key
     * @param value
     * @return
     */
    public void validateRedisMark(String key,String value){
        redisTemplate.opsForValue().setIfAbsent(key,value,5L, TimeUnit.MINUTES);
    }

    /**
     * 删除key
     * @param key
     */
    public void delRedisMark(String key){
        redisTemplate.delete(key);
    }

    /**
     * 验证是否存在
     * @param key
     * @return
     */
    public boolean isValidateSuccess(String key){
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取批次号
     * @param key
     * @return
     */
    public String getRedisMark(String key){
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 根据成型排程结果和硫化机关系构建换模计划列表
     * @param cxScheduleResult
     * @param cxChangeLhMachineList
     * @return
     */
    public List<LhEngineMoldChangePlan> buildLhMachineMoldChangePlan(CxScheduleResult cxScheduleResult,List<CxChangeLhMachine> cxChangeLhMachineList){
        List<LhEngineMoldChangePlan> moldChangePlanList = new ArrayList<>();


        return moldChangePlanList;
    }

}
