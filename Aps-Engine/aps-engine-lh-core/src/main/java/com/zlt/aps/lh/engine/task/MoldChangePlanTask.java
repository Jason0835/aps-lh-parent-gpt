package com.zlt.aps.lh.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.CxEngineChangeLhMachineService;
import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.engine.common.LhCommonService;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.domain.LhEngineMoldChangePlan;
import com.zlt.aps.lh.engine.exception.LhEngineException;
import com.zlt.aps.lh.engine.service.LhEngineMoldChangePlanService;
import com.zlt.aps.lh.engine.service.LhEngineMoldChangePlanTempService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模具变动单引擎
 */
@Component("moldChangePlanTask")
@Slf4j
public class MoldChangePlanTask {

    @Autowired
    private MoldChangePlanCheck moldChangePlanCheck;

    @Autowired
    private LhEngineMoldChangePlanService lhEngineMoldChangePlanService;

    @Autowired
    private LhEngineMoldChangePlanTempService lhEngineMoldChangePlanTempService;

    @Autowired
    private LhCommonService lhCommonService;

    @Autowired
    private CxEngineChangeLhMachineService cxEngineChangeLhMachineService;


    /**
     * 模具变动单前验证
     * @param scheduleDate
     * @return
     */
    public String preChangePlanCheck(String scheduleDate){
        StringBuilder errorMsg=new StringBuilder();
        //1.验证当前排程是否已经存在成型自动排程抓取记录
       String cxBatchNo=moldChangePlanCheck.getCxScheduleRecordCxBatchNo(scheduleDate,errorMsg);
        //2.加载成型排程结果列表数据
        //toChangeMoldPlanList=moldChangePlanCheck.selectCxScheduleResultList(scheduleDate,errorMsg);
        if(StringUtils.isEmpty(errorMsg)){
            moldChangePlanCheck.validateRedisMark(SecurityUtils.getUsername()+":"+ LhEngineConstants.MOLD_CX_BATCH_NO_CACHE,cxBatchNo);
        }
        return errorMsg.toString();
    }

    /**
     * 根据选择的排程日期进行模具变动单生成
     * @param scheduleDate
     */
    @Transactional
    public void moldChangePlanTask(String scheduleDate) throws LhEngineException{
        String key=SecurityUtils.getUsername()+":"+ LhEngineConstants.MOLD_CX_BATCH_NO_CACHE;
        if(!moldChangePlanCheck.isValidateSuccess(key)){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.uncheck.error"));
        }
        String cxBatchNo=moldChangePlanCheck.getRedisMark(key);
        //Joran 2021-09-03 调整为单条成型确定硫化机生成模具变动单，此处只进行数据拼接删除多余数据处理start
        //根据成型批次号进行模具变动单删除
        if(StringUtils.isNotEmpty(cxBatchNo)){
            //lhEngineMoldChangePlanService.deleteLhEngineMoldChangePlanByParams(null,null,null,cxBatchNo);
            lhEngineMoldChangePlanService.deleteLhEngineMoldChangePlanByScheduleDate(scheduleDate);
        }
        //获取模具变动单批次号，如果有自动生成记录则获取已经存在的批次号，如果没有则自动生成
         String moldBatchNo=moldChangePlanCheck.getMoldBatchNo(cxBatchNo,scheduleDate);
        //加载已经生成的模具变动单信息
        LhEngineMoldChangePlan condition = new LhEngineMoldChangePlan();
        condition.setScheduleDate(DateUtils.parseDate(scheduleDate));
        //从临时表获取单规格生成记录
        List<LhEngineMoldChangePlan> moldChangePlanList=lhEngineMoldChangePlanTempService.selectLhEngineMoldChangePlanList(condition);
        if(StringUtils.isNotEmpty(moldChangePlanList)){
            //2.删除现有数据模具变动单数据集合
            Map<String,LhEngineMoldChangePlan> lhEngineMoldChangePlanMap=new HashMap<>();
            for(LhEngineMoldChangePlan lhEngineMoldChangePlan:moldChangePlanList){
                String lhMachineCode=lhEngineMoldChangePlan.getLhMachineCode();
                if(lhEngineMoldChangePlanMap.containsKey(lhMachineCode)){
                    LhEngineMoldChangePlan existPlan=lhEngineMoldChangePlanMap.get(lhMachineCode);
                    String existBeforeSapCode=existPlan.getBeforeSapCode();
                    if(StringUtils.isNotEmpty(existBeforeSapCode)&&!existBeforeSapCode.equals(lhEngineMoldChangePlan.getBeforeSapCode())){
                        existBeforeSapCode+=","+lhEngineMoldChangePlan.getBeforeSapCode();
                        existPlan.setBeforeSapCode(existBeforeSapCode);
                    }else{
                        existPlan.setBeforeSapCode(lhEngineMoldChangePlan.getBeforeSapCode());
                    }
                    String existBeforeSpecDesc=existPlan.getBeforeSpecDesc();
                    if(StringUtils.isNotEmpty(existBeforeSpecDesc)&&!existBeforeSpecDesc.equals(lhEngineMoldChangePlan.getBeforeSpecDesc())){
                        existBeforeSpecDesc+=","+lhEngineMoldChangePlan.getBeforeSpecDesc();
                        existPlan.setBeforeSpecDesc(existBeforeSpecDesc);
                    }else{
                        existPlan.setBeforeSpecDesc(lhEngineMoldChangePlan.getBeforeSpecDesc());
                    }
                    String existChangeType=existPlan.getChangeType();
                    if(StringUtils.isNotEmpty(existChangeType)&&!existChangeType.equals(lhEngineMoldChangePlan.getChangeType())){
                        existChangeType+=","+lhEngineMoldChangePlan.getChangeType();
                        existPlan.setChangeType(existChangeType);
                    }else{
                        existPlan.setChangeType(lhEngineMoldChangePlan.getChangeType());
                    }
                    String existAfterSapCode=existPlan.getAfterSapCode();
                    if(StringUtils.isNotEmpty(existAfterSapCode)&&!existAfterSapCode.equals(lhEngineMoldChangePlan.getAfterSapCode())){
                        existAfterSapCode+=","+lhEngineMoldChangePlan.getAfterSapCode();
                        existPlan.setAfterSapCode(existAfterSapCode);
                    }else{
                        existPlan.setAfterSapCode(lhEngineMoldChangePlan.getAfterSapCode());
                    }
                    String existAfterSpecDesc=existPlan.getAfterSpecDesc();
                    if(StringUtils.isNotEmpty(existAfterSapCode)&&!existAfterSpecDesc.equals(lhEngineMoldChangePlan.getAfterSpecDesc())){
                        existAfterSpecDesc+=","+lhEngineMoldChangePlan.getAfterSpecDesc();
                        existPlan.setAfterSpecDesc(existAfterSpecDesc);
                    }else{
                        existPlan.setAfterSpecDesc(lhEngineMoldChangePlan.getAfterSpecDesc());
                    }
                }else{
                    lhEngineMoldChangePlan.setMoldBatchNo(moldBatchNo);//设置批次号
                    lhEngineMoldChangePlanMap.put(lhMachineCode,lhEngineMoldChangePlan);
                }
            }

            //新生成合并模具变动单数据
            if(StringUtils.isNotEmpty(lhEngineMoldChangePlanMap)){
                List<LhEngineMoldChangePlan> insertList=lhEngineMoldChangePlanMap.values().stream().collect(Collectors.toList());
                if(StringUtils.isNotEmpty(insertList)){
                    lhEngineMoldChangePlanService.batchCreateMoldChangePlan(insertList);
                }

            }

        }
        //Joran 2021-11-05 移除验证缓存
        moldChangePlanCheck.delRedisMark(key);
        //Joran 2021-09-03 调整为单条成型确定硫化机生成模具变动单，此处只进行数据拼接删除多余数据处理end
        /* List<LhEngineMoldChangePlan> lhEngineMoldChangePlanList=new ArrayList<>();
        //获取批次号
        String moldBatchNo=moldChangePlanCheck.createBatchNo(scheduleDate);
        //根据成型批次号进行模具变动单删除
        if(StringUtils.isNotEmpty(cxBatchNo)){
            lhEngineMoldChangePlanService.deleteLhEngineMoldChangePlanByCxBatchNo(cxBatchNo);
        }
        try {
            //解析后规格数据
            Map<String,LhEngineMoldChangePlan> lhMachineChangeMoldPlanMap=new HashMap<>();
            changeAfterAnalysis(moldBatchNo,lhMachineChangeMoldPlanMap);
            if(StringUtils.isEmpty(lhMachineChangeMoldPlanMap)){
                log.debug("未找到符合生成的模具变动单数据，模具变动单不进行生成！");
                moldEngineAutoGenerageRecordService.reGenerageRecord(cxBatchNo,moldBatchNo,scheduleDate, LhEngineConstants.LH_AUTO_RECORD_STATUS_SUCCESS);
                return;
            }
            //查找前规格信息
            lhEngineMoldChangePlanList=bulidBeforeAnalysis(lhMachineChangeMoldPlanMap,scheduleDate);
            if(StringUtils.isNotEmpty(lhEngineMoldChangePlanList)){
                lhEngineMoldChangePlanService.batchCreateMoldChangePlan(lhEngineMoldChangePlanList);
            }
            moldEngineAutoGenerageRecordService.reGenerageRecord(cxBatchNo,moldBatchNo,scheduleDate, LhEngineConstants.LH_AUTO_RECORD_STATUS_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage());
            moldEngineAutoGenerageRecordService.reGenerageRecord(cxBatchNo,moldBatchNo,scheduleDate, LhEngineConstants.LH_AUTO_RECORD_STATUS_FAIL);
            e.printStackTrace();
            throw e;
        }*/
    }

    /**
     * 解析填充后规格相关数据
     */
   /* private void  changeAfterAnalysis(Map<String,LhEngineMoldChangePlan> lhMachineChangeMoldPlanMap) {
        Map<String,String> changeTypeMap=null;
        if(StringUtils.isEmpty(toChangeMoldPlanList)){
            log.debug("当前成型排程没有待换模计划，无需生成模具变动单");
            return;
        }
        //遍历获取后规格相关信息填充start
        for(CxScheduleResult cxScheduleResult:toChangeMoldPlanList){
            singleChangeAfterAnalysis(cxScheduleResult,changeTypeMap,lhMachineChangeMoldPlanMap);
        }
        //遍历获取后规格相关信息填充end
    }
*/
    /**
     * 单个成型排程后规格信息解析
     * @param cxScheduleResult
     * @param changeTypeMap
     * @param lhMachineChangeMoldPlanMap
     */
    public void singleChangeAfterAnalysis(CxScheduleResult cxScheduleResult, Map<String,String> changeTypeMap,Map<String,LhEngineMoldChangePlan> lhMachineChangeMoldPlanMap){
        changeTypeMap=new HashMap<>();
        LhEngineMoldChangePlan lhEngineMoldChangePlan=null;
        LhEngineMoldChangePlan lhMachineCodePlan=null;
        String lhMachineCodes=cxScheduleResult.getLhMachineCode();//硫化机台信息，多台硫化机用逗号分隔
        if(StringUtils.isEmpty(lhMachineCodes)){//没设置硫化机
            log.debug("当前排程计划尚未确定硫化机，无法进行模具变动单生成，工单号："+cxScheduleResult.getOrderNo());
            return;
        }
        //对硫化机拆分
        String[] lhMachineCodeArray=StringUtils.split(lhMachineCodes,",");
        //拆分硫化机对应更换类型
        Map<String,Integer> lhMachineMoldMap = moldChangePlanCheck.splitChangeTypeMap(changeTypeMap,cxScheduleResult.getLhMachineChangeMoldDesc());
        if(StringUtils.isEmpty(changeTypeMap)){
            log.debug("确定硫化机及更换类型数据获取失败，无需进行模具变动单生成，工单号："+cxScheduleResult.getOrderNo());
            return;
        }
        for(String lhMachineCode:lhMachineCodeArray) {
            if (StringUtils.isEmpty(lhMachineCode)) {
                continue;
            }
            //拆分获取更换类型
            if(!changeTypeMap.containsKey(lhMachineCode)){
                log.debug("模具更换类型为：尚未确定硫化机与更换模类型，不进行模具变动单生成");
                continue;
            }

            //使用模数
            Integer useMoldNum=0;
            if(lhMachineMoldMap.containsKey(lhMachineCode)){
                useMoldNum=lhMachineMoldMap.get(lhMachineCode);
            }
            String moldChangeType=changeTypeMap.get(lhMachineCode);
            if (LhEngineConstants.UN_CHANGE_MOLD.equals(moldChangeType)) {
                log.debug("模具更换类型为：无，不需要进行模具变动");
                continue;
            }
            if (lhMachineChangeMoldPlanMap.containsKey(lhMachineCode)) {
                lhMachineCodePlan = lhMachineChangeMoldPlanMap.get(lhMachineCode);
                String afterSapCode = lhMachineCodePlan.getAfterSapCode();
                String afterSpecDesc = lhMachineCodePlan.getAfterSpecDesc();
                String stockArea = lhMachineCodePlan.getStockArea();
                lhEngineMoldChangePlan.setAfterSapCode(afterSapCode + ";" + cxScheduleResult.getSapCode()); //硫化机相同规格拼接
                lhEngineMoldChangePlan.setAfterSpecDesc(afterSpecDesc + ";" + cxScheduleResult.getSpecDesc());
                lhEngineMoldChangePlan.setStockArea(stockArea + ";" + cxScheduleResult.getStorageLocation());
            } else {
                lhEngineMoldChangePlan = new LhEngineMoldChangePlan();
                lhEngineMoldChangePlan.setScheduleDate(cxScheduleResult.getScheduleDate());//成型排程日期记录
                //lhEngineMoldChangePlan.setMoldBatchNo(moldBatchNo);//模具变动单批次号
                lhEngineMoldChangePlan.setAfterSapCode(cxScheduleResult.getSapCode());//后规格SAP品号
                lhEngineMoldChangePlan.setAfterSpecDesc(cxScheduleResult.getSpecDesc());//后规格描述
                lhEngineMoldChangePlan.setStockArea(cxScheduleResult.getStorageLocation());//库存地点
                lhEngineMoldChangePlan.setLhMachineCode(lhMachineCode);//硫化机台编码
                lhEngineMoldChangePlan.setCreateBy(SecurityUtils.getUsername());
                lhEngineMoldChangePlan.setCreateTime(DateUtils.getNowDate());//创建时间
                //拆分获取更换类型
                lhEngineMoldChangePlan.setChangeType(moldChangeType);//更换类型
                //设置使用的模数
                lhEngineMoldChangePlan.setUseMoldNum(useMoldNum);
            }
            //拼接原始工单号
            moldChangePlanCheck.setSourceCxOrder(lhEngineMoldChangePlan, cxScheduleResult.getOrderNo());
            lhMachineChangeMoldPlanMap.put(lhMachineCode, lhEngineMoldChangePlan);
        }
    }


    /**
     * 查找前规格相关信息
     * @param lhMachineChangeMoldPlanMap
     */
    private List<LhEngineMoldChangePlan> bulidBeforeAnalysis(Map<String, LhEngineMoldChangePlan> lhMachineChangeMoldPlanMap,String scheduleDate) {

        List<LhEngineMoldChangePlan> lhEngineMoldChangePlanList =new ArrayList<>(lhMachineChangeMoldPlanMap.size());
        //获取投产中的前规格相关信息
        List<CxScheduleResult> beforeSpecDoingList=moldChangePlanCheck.selectBeforeCxScheduleResultList(scheduleDate, "", LhEngineConstants.CX_PRODUCTION_STATUS_DOING);
        lhCommonService.updateLastDayTaskStock(beforeSpecDoingList,scheduleDate);//更新库存
        Map<String,CxScheduleResult> doingLhMachineResult=new HashMap<>();
        //构建投产中前规格集合
        moldChangePlanCheck.bulidLhMachineCodeResultMap(beforeSpecDoingList,doingLhMachineResult);
        //Joran 2021-09-28 获取前一天已收尾的前规格相关信息
       // Date lastDate=DateUtils.addDays(DateUtils.parseDate(scheduleDate),-1);
        //String lastDateStr=DateUtils.parseDateToStr("yyyy-MM-dd",lastDate);
       // List<CxScheduleResult> beforeSpecCloseOutList=moldChangePlanCheck.selectBeforeCxScheduleResultList(scheduleDate,"",LhEngineConstants.CX_PRODUCT_STATUS_CLOSE_OUT);
        List<CxScheduleResult> beforeSpecCloseOutList=moldChangePlanCheck.selectBeforeCloseOutCxScheduleList(lhMachineChangeMoldPlanMap,scheduleDate);
        lhCommonService.updateLastDayTaskStock(beforeSpecCloseOutList,scheduleDate);//更新库存
        //构建已收尾前规格集合
        Map<String,CxScheduleResult> closeOutLhMachineResult=new HashMap<>();
        moldChangePlanCheck.bulidLhMachineCodeResultMap(beforeSpecCloseOutList,closeOutLhMachineResult);

        //遍历已经生成的模具变动单相关集合进行遍历查找前规格相关信息start
        Double useTotalMoldNum=0D;//冗余使用总模数
        Integer useTotalStock=0;//冗余使用库存数
        for(Map.Entry<String, LhEngineMoldChangePlan> entry:lhMachineChangeMoldPlanMap.entrySet()){
            String lhMachineCode =entry.getKey();//硫化机台
            LhEngineMoldChangePlan lhMoldChangePlan =entry.getValue();//模具变动单
            CxScheduleResult beforeSpec=null;
            if(doingLhMachineResult.containsKey(lhMachineCode)){ //投产中的规格
                beforeSpec=doingLhMachineResult.get(lhMachineCode);
            } else if(closeOutLhMachineResult.containsKey(lhMachineCode)){ //已经收尾的规格
                beforeSpec=closeOutLhMachineResult.get(lhMachineCode);
            }
            if(beforeSpec!=null){
                Integer tireStockNum=beforeSpec.getCalcTotalStock();//获取到总库存
                if(beforeSpec.getLhMachineQty().equals(Double.valueOf(lhMoldChangePlan.getUseMoldNum()))){
                    lhMoldChangePlan.setTireRoughStock(tireStockNum);//不同成型机占比库存
                }else{
                    Integer currentMoldNum=lhMoldChangePlan.getUseMoldNum();//当前占比模数
                    useTotalMoldNum+=currentMoldNum;
                    Integer useStock=0;
                    if(useTotalMoldNum.equals(beforeSpec.getLhMachineQty())){ //使用模数等于总模数时库存直接用扣减
                        useStock=tireStockNum-useTotalStock>0?tireStockNum-useTotalStock:0;
                    }else{
                        useStock= BigDecimal.valueOf(tireStockNum * (currentMoldNum/beforeSpec.getLhMachineQty())).setScale(1,BigDecimal.ROUND_DOWN).intValue();
                        useTotalStock+=useStock;
                    }

                    lhMoldChangePlan.setTireRoughStock(useStock);
                }
                lhMoldChangePlan.setBeforeSapCode(beforeSpec.getSapCode());//前规格SAP品号
                lhMoldChangePlan.setBeforeSpecDesc(beforeSpec.getSpecDesc());//前规格描述
                lhMoldChangePlan.setLhSingleTireTime(beforeSpec.getLhSingleTireTime());//单胎硫化时长
                //拼接原始工单号
                moldChangePlanCheck.setSourceCxOrder(lhMoldChangePlan,beforeSpec.getOrderNo());
            }
            //Joran 2021-09-15 没有前规格则默认更换时间为早上8点
            moldChangePlanCheck.calcChangeTime(scheduleDate,lhMoldChangePlan);
            lhEngineMoldChangePlanList.add(lhMoldChangePlan);
        }
        //遍历已经生成的模具变动单相关集合进行遍历查找前规格相关信息end
        return  lhEngineMoldChangePlanList;

    }



    /**
     * 新增模具变动单验证
     * @param lhMoldChangePlan
     * @return
     */
    public ValidateResult moldPlanPreCheck(LhMoldChangePlan lhMoldChangePlan){
        StringBuilder errorMsg=new StringBuilder();//错误信息
        StringBuilder tipMsg=new StringBuilder();//提示信息
        moldChangePlanCheck.validateMoldPlanData(lhMoldChangePlan,errorMsg,tipMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }
        //提示信息
        if(StringUtils.isNotEmpty(tipMsg)){
            return ValidateResult.success(tipMsg.toString());
        }
        return ValidateResult.success();
    }


    /**
     * 单条排程确定硫化机台后生成模具变动单
     * @param cxScheduleResult
     * @throws LhEngineException
     */
    @Transactional
    public void singleMoldChangePlanTask(CxScheduleResult cxScheduleResult,List<String> historyMachineCodeList) throws LhEngineException{
        StringBuilder errorMsg=new StringBuilder();
        //单记录模具变动单生成参数验证start
        moldChangePlanCheck.singleMoldChangePlanCheck(cxScheduleResult,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            throw new LhEngineException(errorMsg.toString());
        }
        //单记录模具变动单生成参数验证end
        //成型工单号
        String orderNo=cxScheduleResult.getOrderNo();
        //排程日期
        String scheduleDate=DateUtils.parseDateToStr("yyyy-MM-dd",cxScheduleResult.getScheduleDate());
        //1.获取模具变动单批次号，如果有自动生成记录则获取已经存在的批次号，如果没有则自动生成
       //String moldBatchNo=moldChangePlanCheck.getMoldBatchNo(cxBatchNo,scheduleDate);
        //解析后规格数据
        Map<String,LhEngineMoldChangePlan> lhMachineChangeMoldPlanMap=new HashMap<>();
        Map<String,String> changeTypeMap=new HashMap<>();
        singleChangeAfterAnalysis(cxScheduleResult,changeTypeMap,lhMachineChangeMoldPlanMap);

        if(StringUtils.isEmpty(lhMachineChangeMoldPlanMap)){
            log.debug("未找到符合生成的模具变动单数据，模具变动单不进行生成！");
            return;
        }
        //查找前规格信息
        List<LhEngineMoldChangePlan> lhEngineMoldChangePlanList=bulidBeforeAnalysis(lhMachineChangeMoldPlanMap,scheduleDate);
        if(StringUtils.isNotEmpty(lhEngineMoldChangePlanList)){
            //根据工单号删除模具变动单相关数据
            if(StringUtils.isNotEmpty(historyMachineCodeList)){
                lhEngineMoldChangePlanTempService.deleteLhEngineMoldChangePlanByParams(orderNo,historyMachineCodeList,null,cxScheduleResult.getScheduleDate());
            }
            //保存模具变动单数据
            lhEngineMoldChangePlanTempService.batchCreateMoldChangePlan(lhEngineMoldChangePlanList);
        }
    }

    /**
     * 单成型排程生成模具变动单
     * @param cxScheduleResult
     * @param historyMachineCodeList
     * @param list
     */
    @Transactional
    public void singleMoldChangePlanTaskByChange(CxScheduleResult cxScheduleResult, List<String> historyMachineCodeList, List<CxChangeLhMachine> list) {
        StringBuilder errorMsg=new StringBuilder();
        //单记录模具变动单生成参数验证start
        moldChangePlanCheck.singleMoldChangePlanCheck(cxScheduleResult,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            throw new LhEngineException(errorMsg.toString());
        }
        //单记录模具变动单生成参数验证end
        //成型工单号
        String orderNo=cxScheduleResult.getOrderNo();
        //排程日期
        String scheduleDate=DateUtils.parseDateToStr("yyyy-MM-dd",cxScheduleResult.getScheduleDate());
        /**
         * Joran 2022-04-14 从拆分排程硫化机关系表获取设置信息start
         */
        Boolean isChange=false;
        if(isChange && StringUtils.isNotEmpty(list)){
        }
        /**
         * Joran 2022-04-14 从拆分排程硫化机关系表获取设置信息end
         */
    }
}
