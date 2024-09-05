package com.zlt.aps.cx.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineAutoScheduleRecord;
import com.zlt.aps.cx.engine.domain.CxEngineEmbryoMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineSpecifyMachineService;
import com.zlt.aps.cx.engine.service.CxPlanProductStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 成型排程投产规格引擎
 */
@Component("productTaskService")
@Slf4j
public class ProductTaskService {
    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;
    @Autowired
    private CxEngineSpecifyMachineService cxEngineSpecifyMachineService;
    @Autowired
    private CxPlanProductStatusService cxPlanProductStatusService;
    @Autowired
    private ScheduleCheckService scheduleCheckService;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;


    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 待投产列表中数据进行投产验证
     * @param cxPlanProductStatus
     */
    public ValidateResult productTaskPreCheck(CxPlanProductStatus cxPlanProductStatus){
        init();//每一次调用检查都用初始化
        StringBuilder errorMsg=new StringBuilder();
        scheduleCheckService.productValidateParam(cxPlanProductStatus,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }
        String embryoCode=cxPlanProductStatus.getEmbryoCode();//胎胚代码
        String machineCode=cxPlanProductStatus.getCxMachineCode();//成型机台编号
        //各个班次计划量验证格式化
        cxPlanProductStatus.initPlanQty();
        //验证成型机
        CxMachineInfo cxMachineInfo=scheduleCheckService.validateCxMachine(machineCode,errorMsg);
        //获取到施工信息
        EngineProductConstructionInfo engineConstructionInfo=scheduleCheckService.validateConstructionInfo(embryoCode,cxPlanProductStatus.getBomDataVersion(),errorMsg);

        //主线业务校验
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }

        //验证施工信息寸口
        if(engineConstructionInfo.getDimension()==null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.construction.dimension.empty.error")) ;
        }
        StringBuilder tipMsg= new StringBuilder("");
        //获取排程抓取记录取得工单号和生产排程计划版本号
        scheduleCheckService.validateAutoScheduleRecord(cxPlanProductStatus.getScheduleDate(),tipMsg);

        //验证主计划版本主表信息
        MdmMonthPlanMain mdmMonthPlanMain=scheduleCheckService.validateMdmMonthPlanMain(cxPlanProductStatus.getScheduleDate());

        //验证如果来源是主计划的存在，则证明是待投产插单，月度汇总表相关不需要进行操作
        List<CxEngineMonthPlanSurplus> existList=scheduleCheckService.listCxEngineMonthPlanSurplus(mdmMonthPlanMain.getMonthPlanApsVersion(),cxPlanProductStatus.getSapCode());
        if(StringUtils.isEmpty(existList)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.monthRemainQty.empty.error"));
        }

        //验证如果来源是主计划的存在，则证明是待投产插单，月度汇总表相关不需要进行操作
        List<CxEngineEmbryoMonthPlanSurplus> existEmbryoList=scheduleCheckService.listCxEngineEmbryoMonthPlanSurplus(mdmMonthPlanMain.getMonthPlanApsVersion(),cxPlanProductStatus.getEmbryoCode());
        if(StringUtils.isEmpty(existEmbryoList)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.monthRemainQty.embryo.empty.error"));
        }

        //验证排程结果是否存在
        CxEngineScheduleResult existResult=scheduleCheckService.validateScheduleResult(cxPlanProductStatus.getScheduleDate(),cxPlanProductStatus.getSapCode(),cxPlanProductStatus.getEmbryoCode(),machineCode,cxPlanProductStatus.getBomDataVersion(),new StringBuilder());
        if(existResult!=null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.repeat.error"));
        }
        //遍历查找如果找到了则验证是否来源插单，如果来源插单则报错不允许投产，如果是主计划则不处理
        for(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus:existEmbryoList){
            //数据来源为主计划
            if(CxEngineConstants.CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_MPS.equals(cxEngineEmbryoMonthPlanSurplus.getDataSource())){
                //获取插单的计划总量
                int totalPlanQty=cxPlanProductStatus.getTotalPlanQty();
                scheduleCheckService.checkMonthRemainQty(cxEngineEmbryoMonthPlanSurplus,totalPlanQty,cxPlanProductStatus.getSapCode(),cxPlanProductStatus.getEmbryoCode(),cxPlanProductStatus.getScheduleDate(),tipMsg);
                break;//跳出循环
            }else if(CxEngineConstants.CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_INSERT.equals(cxEngineEmbryoMonthPlanSurplus.getDataSource())){
                //如果找到的胎胚月度计划汇总信息是插单类型则不允许进行投产
                errorMsg.append(I18nUtil.getMessage("cx.engine.insert.use.product.error"));//月度下发计划中不存在规格，请进行插单！
            }
        }

        //主线业务校验
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }

        //寸口验证提示语
        scheduleCheckService.checkDimension(cxMachineInfo,engineConstructionInfo,tipMsg);
        //成型机定点相关信息验证
        this.cxEngineSpecifyMachineService.validateSpecifyMachine(cxPlanProductStatus.getSapCode(),cxPlanProductStatus.getEmbryoCode(),machineCode,tipMsg);
        //Joran 2021-11-05 验证通过标记到redis
        scheduleCheckService.validateRedisMark(scheduleCheckService.createKey(SecurityUtils.getUsername(), CxPrefixConstants.CX_PRODUCT_PLAN_PREFIX));
        if(StringUtils.isNotEmpty(tipMsg)){
            return ValidateResult.success(tipMsg.toString());
        }
        return ValidateResult.success();
    }


    /**
     * 投产插单功能
     * @param cxPlanProductStatus
     * @throws CxScheduleEngineException
     */
    @Transactional
    public void productTask(CxPlanProductStatus cxPlanProductStatus) throws CxScheduleEngineException{
        //Joran 2021-11-04验证是否通过没通过是没有设置键
        String key= scheduleCheckService.createKey(SecurityUtils.getUsername(),CxPrefixConstants.CX_PRODUCT_PLAN_PREFIX);
        if(!scheduleCheckService.isValidate(key)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.product.uncheck.error"));
        }
        init();
        //获取排程抓取记录取得工单号和生产排程计划版本号
        CxEngineAutoScheduleRecord autoScheduleRecord=scheduleCheckService.validateAutoScheduleRecord(cxPlanProductStatus.getScheduleDate(),new StringBuilder());
        if(autoScheduleRecord==null){
            //创建排程记录
            autoScheduleRecord = scheduleCheckService.createAutoScheduleRecord(cxPlanProductStatus.getScheduleDate());
        }

        //对象转换
        //更新标记位和投产状态在module模块进行更新，引擎端不进行更新
    /*    com.zlt.aps.cx.engine.domain.CxPlanProductStatus updateCxPlanProductStatus =new com.zlt.aps.cx.engine.domain.CxPlanProductStatus();
        BeanUtils.copyProperties(cxPlanProductStatus,updateCxPlanProductStatus);
        //标记不投产标记为否
        updateCxPlanProductStatus.setMarkUnProduct(CxEngineConstants.MARK_UN_PRODUCT_NO);
        //标记投产状态：已投产
        updateCxPlanProductStatus.setProductStatus(CxEngineConstants.MDM_PLAN_PRODUCT_STATUS_YES);
        updateCxPlanProductStatus.setRemark("手动投产："+DateUtils.getTime());
        cxPlanProductStatusService.updateCxPlanProductStatusById(updateCxPlanProductStatus);*/
        //验证主计划版本主表信息
        MdmMonthPlanMain mdmMonthPlanMain=scheduleCheckService.validateMdmMonthPlanMain(cxPlanProductStatus.getScheduleDate());
        //获取成型胎胚计划汇总表
        List<CxEngineEmbryoMonthPlanSurplus> existEmbryoList=scheduleCheckService.listCxEngineEmbryoMonthPlanSurplus(mdmMonthPlanMain.getMonthPlanApsVersion(),cxPlanProductStatus.getEmbryoCode());
        CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus=existEmbryoList.get(0);
        CxEngineScheduleResult cxEngineScheduleResult=createCxScheduleResult(cxPlanProductStatus,cxEngineEmbryoMonthPlanSurplus,autoScheduleRecord);
        //投产状态表变更为待发布状态
        com.zlt.aps.cx.engine.domain.CxPlanProductStatus updateProduct= BeanConverUtil.conver(cxPlanProductStatus, com.zlt.aps.cx.engine.domain.CxPlanProductStatus.class);
        cxPlanProductStatusService.updatePlanProductToProduction(updateProduct);
        //插入插单日志日志
        StringBuilder logDetail= new StringBuilder();
        logDetail.append("操作人员：").append(SecurityUtils.getUsername()).append(division);
        logDetail.append("操作时间：").append(DateUtils.getTime()).append(division);
        logDetail.append("排程结果数据：").append(toJSONString(cxEngineScheduleResult)).append(division);
        autoScheduleLogService.insertCxScheduleLog(cxEngineScheduleResult.getCxBatchNo(), cxEngineScheduleResult.getOrderNo(), "【投产】生成成型排程数据",
                logDetail.toString()); //添加日志
        cxScheduleEngineMapper.insertCxScheduleResult(cxEngineScheduleResult);

        //Joran 2021-11-05 进行移除验证通过标记
        scheduleCheckService.delValidateRedisMark(key);

    }

    /**
     * 构建生产排程对象
     * @param cxPlanProductStatus
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    private CxEngineScheduleResult createCxScheduleResult(CxPlanProductStatus cxPlanProductStatus, CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus,CxEngineAutoScheduleRecord autoScheduleRecord) {
        //验证成型机
        StringBuilder errorMsg=new StringBuilder();
        //获取到施工信息
        EngineProductConstructionInfo engineConstructionInfo=scheduleCheckService.validateConstructionInfo(cxPlanProductStatus.getEmbryoCode(),cxPlanProductStatus.getBomDataVersion(),errorMsg);
        CxMachineInfo cxMachineInfo=scheduleCheckService.validateCxMachine(cxPlanProductStatus.getCxMachineCode(),errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            throw new CxScheduleEngineException(errorMsg.toString());
        }
        //生成排程结果
        CxEngineScheduleResult cxEngineScheduleResult=new CxEngineScheduleResult();
        cxEngineScheduleResult.setTaskType(CxEngineConstants.TASK_TYPE_TODO);//没有确定硫化机，则为待投产
        cxEngineScheduleResult.setScheduleDate(cxPlanProductStatus.getScheduleDate());//投产日期
        cxEngineScheduleResult.setStorageLocation(cxPlanProductStatus.getStorageLocation());//库存地点
        cxEngineScheduleResult.setCxMachineCode(cxPlanProductStatus.getCxMachineCode());//成型机台
        cxEngineScheduleResult.setCxMachineType(cxMachineInfo.getMachineType());//2021-12-15成型机台类型
        cxEngineScheduleResult.setCxMachineName(cxMachineInfo.getMachineName());//2021-12-15成型机台名称
        cxEngineScheduleResult.setSapCode(cxPlanProductStatus.getSapCode());//SAP品号
        cxEngineScheduleResult.setEmbryoCode(cxPlanProductStatus.getEmbryoCode());//胎胚代码
        cxEngineScheduleResult.setBomDataVersion(cxPlanProductStatus.getBomDataVersion());//胎胚版本
        cxEngineScheduleResult.setClass1PlanQty(cxPlanProductStatus.getClass1PlanQty());
        cxEngineScheduleResult.setClass2PlanQty(cxPlanProductStatus.getClass2PlanQty());
        cxEngineScheduleResult.setClass3PlanQty(cxPlanProductStatus.getClass3PlanQty());
        cxEngineScheduleResult.setClass4PlanQty(cxPlanProductStatus.getClass4PlanQty());
        cxEngineScheduleResult.setClass5PlanQty(cxPlanProductStatus.getClass5PlanQty());
        cxEngineScheduleResult.setWorkShifts(StringUtils.isEmpty(cxMachineInfo.getClassShift())?3:Integer.valueOf(cxMachineInfo.getClassShift()));
        Integer monthRemainQty=cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty(); //胎胚月度剩余量
        monthRemainQty -= cxPlanProductStatus.getTotalPlanQty();
        cxEngineScheduleResult.setMonthRemainQty(monthRemainQty);
        //获取到抓取记录后设置批次号
        cxEngineScheduleResult.setCxBatchNo(autoScheduleRecord.getCxBatchNo());
        //数据填充
        scheduleCheckService.dataFilling(cxEngineScheduleResult,engineConstructionInfo);
        return cxEngineScheduleResult;
    }


    /**
     * 每次调用检查前都要先清空属性防止缓存
     */
    public void init(){
        scheduleCheckService.initBaseData();
    }

    /**
      *  已收尾规格再次投产
      * @ClassName ProductTaskService
      * @Description TODO
      * @Author Joran.Zhang
      * @Date 2021/8/12 8:51
      * @Version 1.0
    **/
    public void closeOutReProduct(CxPlanProductStatus cxPlanProductStatus) throws CxScheduleEngineException{

        //Joran 2021-11-04验证是否通过没通过是没有设置键
        String key= scheduleCheckService.createKey(SecurityUtils.getUsername(),CxPrefixConstants.CX_RE_PRODUCT_PLAN_PREFIX);
        if(!scheduleCheckService.isValidate(key)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.product.uncheck.error"));
        }
        init();
        //获取排程抓取记录取得工单号和生产排程计划版本号
        CxEngineAutoScheduleRecord autoScheduleRecord=scheduleCheckService.validateAutoScheduleRecord(cxPlanProductStatus.getScheduleDate(),new StringBuilder());
        if(autoScheduleRecord==null){
            //创建排程记录
            autoScheduleRecord = scheduleCheckService.createAutoScheduleRecord(cxPlanProductStatus.getScheduleDate());
        }


        //验证主计划版本主表信息
        MdmMonthPlanMain mdmMonthPlanMain=scheduleCheckService.validateMdmMonthPlanMain(cxPlanProductStatus.getScheduleDate());
        //获取成型胎胚计划汇总表
        List<CxEngineEmbryoMonthPlanSurplus> existEmbryoList=scheduleCheckService.listCxEngineEmbryoMonthPlanSurplus(mdmMonthPlanMain.getMonthPlanApsVersion(),cxPlanProductStatus.getEmbryoCode());
        //获取成型胎胚计划汇总表
        CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus=existEmbryoList.get(0);
        //构建排程结果数据
        CxEngineScheduleResult cxEngineScheduleResult=createCxScheduleResult(cxPlanProductStatus,cxEngineEmbryoMonthPlanSurplus,autoScheduleRecord);
        //插入插单日志日志
        autoScheduleLogService.insertCxScheduleLog(cxEngineScheduleResult.getCxBatchNo(), cxEngineScheduleResult.getOrderNo(), "【收尾再投产】生成成型排程数据",
                "再次投产排程数据：" + toJSONString(cxEngineScheduleResult)); //添加日志
        cxScheduleEngineMapper.insertCxScheduleResult(cxEngineScheduleResult);
        //Joran 2021-11-05 进行移除验证通过标记
        scheduleCheckService.delValidateRedisMark(key);
    }

    /**
     * 已收尾规格二次投产前置校验
     * @param cxPlanProductStatus
     * @return
     */
    public ValidateResult reProductTaskPreCheck(CxPlanProductStatus cxPlanProductStatus){
        init();//每一次调用检查都用初始化
        StringBuilder errorMsg=new StringBuilder();
        scheduleCheckService.productValidateParam(cxPlanProductStatus,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }
        String embryoCode=cxPlanProductStatus.getEmbryoCode();//胎胚代码
        String machineCode=cxPlanProductStatus.getCxMachineCode();//成型机台编号
        //各个班次计划量验证格式化
        cxPlanProductStatus.initPlanQty();
        //验证成型机
        CxMachineInfo cxMachineInfo=scheduleCheckService.validateCxMachine(machineCode,errorMsg);
        //获取到施工信息
        EngineProductConstructionInfo engineConstructionInfo=scheduleCheckService.validateConstructionInfo(embryoCode,cxPlanProductStatus.getBomDataVersion(),errorMsg);
        //验证施工信息寸口
        if(engineConstructionInfo.getDimension()==null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.construction.dimension.empty.error")) ;
        }
        StringBuilder tipMsg= new StringBuilder("");
        //获取排程抓取记录取得工单号和生产排程计划版本号
        scheduleCheckService.validateAutoScheduleRecord(cxPlanProductStatus.getScheduleDate(),tipMsg);

        //验证主计划版本主表信息
        MdmMonthPlanMain mdmMonthPlanMain=scheduleCheckService.validateMdmMonthPlanMain(cxPlanProductStatus.getScheduleDate());

        //验证如果来源是主计划的存在，则证明是待投产插单，月度汇总表相关不需要进行操作
        List<CxEngineMonthPlanSurplus> existList=scheduleCheckService.listCxEngineMonthPlanSurplus(mdmMonthPlanMain.getMonthPlanApsVersion(),cxPlanProductStatus.getSapCode());
        if(StringUtils.isEmpty(existList)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.monthRemainQty.empty.error"));
        }

        //验证如果来源是主计划的存在，则证明是待投产插单，月度汇总表相关不需要进行操作
        List<CxEngineEmbryoMonthPlanSurplus> existEmbryoList=scheduleCheckService.listCxEngineEmbryoMonthPlanSurplus(mdmMonthPlanMain.getMonthPlanApsVersion(),cxPlanProductStatus.getEmbryoCode());
        if(StringUtils.isEmpty(existEmbryoList)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.monthRemainQty.embryo.empty.error"));
        }
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }
        //获取胎胚汇总中剩余量
        CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus=existEmbryoList.get(0);
        if(cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty()<=0){ //月度剩余量为0，不允许直接再次投产
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.monthRemainQty.zero.error"));
        }
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }

        //验证排程结果是否存在
        CxEngineScheduleResult existResult=scheduleCheckService.validateScheduleResult(cxPlanProductStatus.getScheduleDate(),cxPlanProductStatus.getSapCode(),cxPlanProductStatus.getEmbryoCode(),machineCode,cxPlanProductStatus.getBomDataVersion(),new StringBuilder());
        if(existResult!=null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.repeat.error"));
        }

        //主线业务校验
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }

        //寸口验证提示语
        scheduleCheckService.checkDimension(cxMachineInfo,engineConstructionInfo,tipMsg);
        //成型机定点相关信息验证
        this.cxEngineSpecifyMachineService.validateSpecifyMachine(cxPlanProductStatus.getSapCode(),cxPlanProductStatus.getEmbryoCode(),machineCode,tipMsg);
        //Joran 2021-11-05 验证通过标记到redis
        scheduleCheckService.validateRedisMark(scheduleCheckService.createKey(SecurityUtils.getUsername(), CxPrefixConstants.CX_RE_PRODUCT_PLAN_PREFIX));
        if(StringUtils.isNotEmpty(tipMsg)){
            return ValidateResult.success(tipMsg.toString());
        }
        return ValidateResult.success();
    }
}
