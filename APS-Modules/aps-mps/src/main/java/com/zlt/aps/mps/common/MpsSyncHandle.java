package com.zlt.aps.mps.common;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.mps.domain.TServiceSyncLog;
import com.zlt.aps.mps.service.*;
import com.zlt.sync.domain.AuxReqSyncDataLogs;
import com.zlt.sync.handle.SyncDataHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gim
 * 20211026 hak——原先该类也是继承自SyncDataHandle，但是接口开发规范要求一个模块只允许有一个类继承SyncDataHandle，因此做如下调整：
 * 	1、取消该类的继承关系，保留RequestMesController类的继承
 * 	2、该类的实现逻辑改为通过RequestMesController类的同名方法调用
 */
@Component
public class MpsSyncHandle {

    @Autowired
    private MesHalfPartService halfPartService;
    @Autowired
    private MesCxService cxService;
    @Autowired
    private MesSyncLogService logService;
    @Autowired
    private MesBaseInfoService infoService;
    @Autowired
    private MesFinishService finishService;
    @Autowired
    private MesCxMidNightFinishService mesCxMidNightFinishService;
    @Autowired
    private MesCxInProductionSpecService mesCxInProductionSpecService;
    @Autowired
    private MesLhInProductionSpecService mesLhInProductionSpecService;
    @Autowired
    private MesLhMoldAdjustPlanService mesLhMoldAdjustPlanService;
    @Autowired
    private MesBadNumService badNumService;
    @Autowired
    private MonthPlanSumService monthPlanSumService;
    //    @Resource
//    private TMpsConstructionInfoMapper constructionInfoMapper;
    @Resource
    private MesConstructionInfoService mesConstructionInfoService;

    public void asyncResult(AjaxResult ajaxResult, SyncDataHandle handle) {
        // AjaxResult 返回的都是成功的数据;

        // 需要一个dataVersion，一个调用哪个接口的标识
        List<AuxReqSyncDataLogs> dataList = (List<AuxReqSyncDataLogs>) ajaxResult.get(Constants.DATA);
        if (CollectionUtil.isEmpty(dataList)) {
            // 同步失败处理 日志
            TServiceSyncLog log = new TServiceSyncLog();
            log.setBaseVale(null);
            log.setServiceType(ServiceTypeEnum.REQUEST.ordinal() + "");
            log.setServiceStatus("1");
            log.setServiceResult("List<AuxReqSyncDataLogs> dataList为空");
            logService.addLog(log);
            handle.setSyncDataStatusFailure(dataList);
            return;
        }
        List<AuxReqSyncDataLogs> successList = new ArrayList<>();
        List<AuxReqSyncDataLogs> failList = new ArrayList<>();
        List<TServiceSyncLog> logList = new ArrayList<>();
        for (AuxReqSyncDataLogs syncDataLog : dataList) {
            TServiceSyncLog log = new TServiceSyncLog();
            log.setBaseVale(null);
            log.setServiceType(ServiceTypeEnum.REQUEST.ordinal() + "");
            log.setServiceStatus("0");
            log.setServiceResult(syncDataLog.getMsg());
            log.setServiceParams(syncDataLog.getParams());

            String syncKey = syncDataLog.getSyncKey();
            String dataVersion = syncDataLog.getDataVersion();
            try {
                // ========================================================================  库存  ============================================================
                if (syncKey.equals(SyncKeyEnum.EMBRYO_STOCK_SYNC.getDescription())) {
                    // 胎胚库存
                    AjaxResult result = cxService.mergeCxStock(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.EMBRYO_MONTH_SYNC.getDescription())) {
                    // 胎胚月结库存
                    AjaxResult result = cxService.mergeCxMonthStock(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.FINISHED_STOCK_SYNC.getDescription())) {
                    // 成品库存
                    AjaxResult result = cxService.mergeCxSapStock(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.TREAD_STOCK.getDescription())) {
                    // 胎面库存
                    AjaxResult result = halfPartService.mergeTm(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.SIDEWALL_STOCK.getDescription())) {
                    // 胎侧库存
                    AjaxResult result = halfPartService.mergeTc(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.LINING_STOCK.getDescription())) {
                    // 内衬库存
                    AjaxResult result = halfPartService.mergeNc(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.BEAD_STOCK.getDescription())) {
                    // 胎圈库存
                    AjaxResult result = halfPartService.mergeTq(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.STEEL_WIRE_STOCK.getDescription())) {
                    // 钢丝圈库存
                    AjaxResult result = halfPartService.mergeGsq(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.ADJUDI15_STOCK.getDescription())) {
                    // cd15库存
                    AjaxResult result = halfPartService.mergeCd15(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.ADJUDI15_LINESIDE_STOCK.getDescription())) {
                    // cd15线边库库存
                    AjaxResult result = halfPartService.mergeCd15LineSide(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.ADJUDI90_LINESIDE_STOCK.getDescription())) {
                    // cd90线边库库存
                    AjaxResult result = halfPartService.mergeCd90LineSide(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.ADJUDI90_STOCK.getDescription())) {
                    // cd90库存
                    AjaxResult result = halfPartService.mergeCd90(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.GDYY_STOCK.getDescription())) {
                    // 钢带压延库存
                    AjaxResult result = halfPartService.mergeGdyy(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.XWYY_STOCK.getDescription())) {
                    // 纤维压延库存
                    AjaxResult result = halfPartService.mergeXwyy(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.BOM_INFO_SYNC.getDescription())) {
                    // ========================================================================  基础信息  ============================================================
                    // bom信息
                    AjaxResult result = infoService.mergeBomInfo(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.PLM_CONSTRUCTION_INFO.getDescription())) {
                    // PLM参数同步
                    AjaxResult result = infoService.mergePlmConstructionInfo(dataVersion);
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.FINISH_SCHE_COMPLETE.getDescription())) {
                    // ========================================================================  完成量回报  ============================================================
                    // 成型排程完成量回报
                    AjaxResult result = finishService.mergeCxFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.VULCANIZE_SCHE_COMPLETE.getDescription())) {
                    // 硫化排程完成量回报
                    AjaxResult result = finishService.mergeLhFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.CX_DAY_COMPLETE.getDescription())) {
                    // 成型日完成量
                    AjaxResult result = finishService.mergeCxDayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.LH_DAY_COMPLETE.getDescription())) {
                    // 硫化日完成量
                    AjaxResult result = finishService.mergeLhDayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.FORMING8_12_COMPLETE.getDescription())) {
                    // 成型8-12点的完成量
                    AjaxResult result = finishService.mergeCxPartFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.TREAD_COMPLETE_QUANTITY.getDescription())) {
                    // 胎面完成量回报
                    AjaxResult result = finishService.mergeTmFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.SIDEWALL_COMPLETE_QUANTITY.getDescription())) {
                    // 胎侧完成量回报
                    AjaxResult result = finishService.mergeTcFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.LINING_COMPLETE_QUANTITY.getDescription())) {
                    // 内衬完成量回报
                    AjaxResult result = finishService.mergeNcFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.ADJUDI15_COMPLETE_QUANTITY.getDescription())) {
                    // 15度裁断完成量回报
                    AjaxResult result = finishService.mergeCd15Finish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.ADJUDI90_COMPLETE_QUANTITY.getDescription())) {
                    // 90度裁断完成量回报
                    AjaxResult result = finishService.mergeCd90Finish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.XWYY_ADJUDI_QUANTITY.getDescription())) {
                    // 纤维压延度裁断完成量回报
                    AjaxResult result = finishService.mergeXwyyFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.BEAD_COMPLETE_QUANTITY.getDescription())) {
                    // 胎圈完成量回报
                    AjaxResult result = finishService.mergeTqFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.STEEL_WIRE_COMPLETE_QUANTITY.getDescription())) {
                    // 钢丝圈完成量回报
                    AjaxResult result = finishService.mergeGsqFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.CX_MID_NIGHT_FINISH.getDescription())) {
                    // 成型中夜班完成量同步
                    AjaxResult result = mesCxMidNightFinishService.mergeFinishQty(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.CX_PRODUCTION_SPEC.getDescription())) {
                    // 成型机台当前生产规格同步
                    AjaxResult result = mesCxInProductionSpecService.mergeSpes(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.LH_IN_PRODUCTION_SPEC.getDescription())) {
                    // 硫化机台当前生产规格同步
                    AjaxResult result = mesLhInProductionSpecService.mergeData(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.LH_MOLD_ADJUST_PLAN.getDescription())) {
                    // 硫化工序模具调整计划接口
                    AjaxResult result = mesLhMoldAdjustPlanService.mergeData(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.CLASS_FINISH_QTY.getDescription())) {
                    // 各工序班次完成量同步接口接口
                    AjaxResult result = finishService.mergeClassFinishQty(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                }
                /*================================================= 半部件日完成量 ==============================================================*/
                else if (syncKey.equals(SyncKeyEnum.TM_DAY_COMPLETE.getDescription())) {
                    // 钢丝圈日完成量回报
                    AjaxResult result = finishService.mergeTmDayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.TC_DAY_COMPLETE.getDescription())) {
                    // 胎侧日完成量回报
                    AjaxResult result = finishService.mergeTcDayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.TQ_DAY_COMPLETE.getDescription())) {
                    // 胎圈日完成量回报
                    AjaxResult result = finishService.mergeTqDayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.GSQ_DAY_COMPLETE.getDescription())) {
                    // 钢丝圈日完成量回报
                    AjaxResult result = finishService.mergeGsqDayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.NC_DAY_COMPLETE.getDescription())) {
                    // 内衬日完成量回报
                    AjaxResult result = finishService.mergeNcDayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.CD15_DAY_COMPLETE.getDescription())) {
                    // 15度裁断日完成量回报
                    AjaxResult result = finishService.mergeCd15DayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.CD90_DAY_COMPLETE.getDescription())) {
                    // 90度裁断日完成量回报
                    AjaxResult result = finishService.mergeCd90DayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else if (syncKey.equals(SyncKeyEnum.XWYY_DAY_COMPLETE.getDescription())) {
                    // 钢丝圈日完成量回报
                    AjaxResult result = finishService.mergeXwyyDayFinish(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                }
                else if (syncKey.equals(SyncKeyEnum.EMBRYO_BAD_QUANTITY.getDescription())) {
                    // ========================================================================  胚胎不良量  ============================================================
                    // 胚胎不良量
                    AjaxResult result = badNumService.mergeBadNum(dataVersion);
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                    }
                    buildSuccessOrFailList(failList, successList, syncDataLog, result);
                } else {
                    log.setServiceStatus("1");
                    log.setServiceResult(I18nUtil.getMessage("mes.error.message.syncKey") + syncKey);
                }
            } catch (Exception e) {
                log.setServiceStatus("1");
                log.setServiceResult("内部处理错误");
                failList.add(syncDataLog);
            }
            logList.add(log);
        }
        handle.setSyncDataStatusSuccess(successList);
        handle.setSyncDataStatusFailure(failList);
        logService.mergeSql(logList);

    }

    private void buildSuccessOrFailList(List<AuxReqSyncDataLogs> failList, List<AuxReqSyncDataLogs> successList, AuxReqSyncDataLogs syncDataLog, AjaxResult result) {
    	if (result == null) {// 没有返回结果，说明是要忽略这个记录被锁定了，本次同步忽略掉（即不成功也不失败）
    		return;
    	}
        if ((int) result.get(Constants.CODE) == HttpStatus.ERROR) {
        	syncDataLog.setMsgCode(299);
        	syncDataLog.setMsg((String)result.get(GatewayConstants.MSG_TAG));
            failList.add(syncDataLog);
        } else {
            successList.add(syncDataLog);
        }
    }
}
