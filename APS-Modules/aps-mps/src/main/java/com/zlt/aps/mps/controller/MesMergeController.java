package com.zlt.aps.mps.controller;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.TSyncMps2ApsFac;
import com.zlt.aps.mps.common.ServiceTypeEnum;
import com.zlt.aps.mps.common.SyncKeyEnum;
import com.zlt.aps.mps.domain.TServiceSyncLog;
import com.zlt.aps.mps.service.MesBadNumService;
import com.zlt.aps.mps.service.MesBaseInfoService;
import com.zlt.aps.mps.service.MesCxInProductionSpecService;
import com.zlt.aps.mps.service.MesCxMidNightFinishService;
import com.zlt.aps.mps.service.MesCxService;
import com.zlt.aps.mps.service.MesFinishService;
import com.zlt.aps.mps.service.MesHalfPartService;
import com.zlt.aps.mps.service.MesSyncLogService;
import com.zlt.aps.mps.service.MonthPlanSumService;

import io.swagger.annotations.Api;

import java.util.List;

/**
 * @author Gim
 * MES库存同步Controller
 */
@Api(tags = "MES同步接口")
@RestController
@RequestMapping("/mps/mes/sync")
public class MesMergeController {

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
    private MesBadNumService badNumService;
    @Autowired
    private MesCxMidNightFinishService mesCxMidNightFinishService;
    @Autowired
    private MesCxInProductionSpecService mesCxInProductionSpecService;
    @Autowired
    private MonthPlanSumService monthPlanSumService;

    @ApiOperation("MPS同步测试")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "dataVersion", dataType = "String", value = "同步版本", required = true),
            @ApiImplicitParam(name = "syncKey", dataType = "String", value = "同步KEY", required = true),
            @ApiImplicitParam(name = "year", dataType = "String", value = "年(主计划下发版本测试需填写)"),
            @ApiImplicitParam(name = "month", dataType = "String", value = "月(主计划下发版本测试需填写)"),
            @ApiImplicitParam(name = "isFinal", dataType = "String", value = "是否定稿(主计划下发版本测试需填写) 0是1否")
    })
    @GetMapping(value = "/unit/{dataVersion}/{syncKey}/{year}/{month}/{isFinal}")
    public AjaxResult mpsSyncTest(@PathVariable("dataVersion") String dataVersion, @PathVariable("syncKey") String syncKey,
                                  @PathVariable("year") String year, @PathVariable("month") String month, @PathVariable("isFinal") String isFinal){
        if (StringUtils.isBlank(dataVersion)) {
            return AjaxResult.error("dataVersion为空");
        }
        if (StringUtils.isBlank(syncKey)) {
            return AjaxResult.error("syncKey为空");
        }
        TServiceSyncLog log = new TServiceSyncLog();
        log.setBaseVale(null);
        log.setServiceType(ServiceTypeEnum.REQUEST.ordinal() + "");
        log.setServiceStatus("0");
        log.setServiceParams("dataVersion = "+ dataVersion + ", syncKey = " + syncKey);
        AjaxResult result1 = new AjaxResult();
        // ========================================================================  库存  ============================================================
        if (syncKey.equals(SyncKeyEnum.EMBRYO_STOCK_SYNC.getDescription())) {
            // 胎胚库存
            cxService.mergeCxStock(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.EMBRYO_MONTH_SYNC.getDescription())) {
            // 胎胚月结库存
            cxService.mergeCxMonthStock(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.FINISHED_STOCK_SYNC.getDescription())) {
            // 成品库存
            cxService.mergeCxSapStock(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.TREAD_STOCK.getDescription())) {
            // 胎面库存
            halfPartService.mergeTm(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.SIDEWALL_STOCK.getDescription())) {
            // 胎侧库存
            halfPartService.mergeTc(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.LINING_STOCK.getDescription())) {
            // 内衬库存
            halfPartService.mergeNc(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.BEAD_STOCK.getDescription())) {
            // 胎圈库存
            halfPartService.mergeTq(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.STEEL_WIRE_STOCK.getDescription())) {
            // 钢丝圈库存
            halfPartService.mergeGsq(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.ADJUDI15_STOCK.getDescription())) {
            // cd15库存
            halfPartService.mergeCd15(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.ADJUDI90_STOCK.getDescription())) {
            // cd90库存
            halfPartService.mergeCd90(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.GDYY_STOCK.getDescription())) {
            // 钢带压延库存
            halfPartService.mergeGdyy(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.XWYY_STOCK.getDescription())) {
            // 纤维压延库存
            halfPartService.mergeXwyy(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.BOM_INFO_SYNC.getDescription())) {
            // ========================================================================  基础信息  ============================================================
            // bom信息
            infoService.mergeBomInfo(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.PLM_CONSTRUCTION_INFO.getDescription())) {
            // PLM参数同步
        	infoService.mergePlmConstructionInfo(dataVersion);
        } else if (syncKey.equals(SyncKeyEnum.FINISH_SCHE_COMPLETE.getDescription())) {
            // ========================================================================  完成量回报  ============================================================
            // 成型排程完成量回报
            AjaxResult result = finishService.mergeCxFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.VULCANIZE_SCHE_COMPLETE.getDescription())) {
            // 硫化排程完成量回报
            AjaxResult result = finishService.mergeLhFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.CX_DAY_COMPLETE.getDescription())) {
            // 成型日完成量
            AjaxResult result = finishService.mergeCxDayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.LH_DAY_COMPLETE.getDescription())) {
            // 硫化日完成量
            AjaxResult result = finishService.mergeLhDayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.FORMING8_12_COMPLETE.getDescription())) {
            // 成型8-12点的完成量
            AjaxResult result = finishService.mergeCxPartFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.TREAD_COMPLETE_QUANTITY.getDescription())) {
            // 胎面完成量回报
            AjaxResult result = finishService.mergeTmFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.SIDEWALL_COMPLETE_QUANTITY.getDescription())) {
            // 胎侧完成量回报
            AjaxResult result = finishService.mergeTcFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.LINING_COMPLETE_QUANTITY.getDescription())) {
            // 内衬完成量回报
            AjaxResult result = finishService.mergeNcFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.ADJUDI15_COMPLETE_QUANTITY.getDescription())) {
            // 15度裁断完成量回报
            AjaxResult result = finishService.mergeCd15Finish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.ADJUDI90_COMPLETE_QUANTITY.getDescription())) {
            // 90度裁断完成量回报
            AjaxResult result = finishService.mergeCd90Finish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.XWYY_ADJUDI_QUANTITY.getDescription())) {
            // 纤维压延度裁断完成量回报
            AjaxResult result = finishService.mergeXwyyFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.BEAD_COMPLETE_QUANTITY.getDescription())) {
            // 胎圈完成量回报
            AjaxResult result = finishService.mergeTqFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.STEEL_WIRE_COMPLETE_QUANTITY.getDescription())) {
            // 钢丝圈完成量回报
            AjaxResult result = finishService.mergeGsqFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.CX_MID_NIGHT_FINISH.getDescription())) {
            // 成型中夜班完成量同步
            AjaxResult result = mesCxMidNightFinishService.mergeFinishQty(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.CX_PRODUCTION_SPEC.getDescription())) {
            // 成型机台当前生产规格同步
            AjaxResult result = mesCxInProductionSpecService.mergeSpes(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        }
        /*================================================= 半部件日完成量 ==============================================================*/
        else if (syncKey.equals(SyncKeyEnum.TM_DAY_COMPLETE.getDescription())) {
            // 钢丝圈日完成量回报
            AjaxResult result = finishService.mergeTmDayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.TC_DAY_COMPLETE.getDescription())) {
            // 胎侧日完成量回报
            AjaxResult result = finishService.mergeTcDayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.TQ_DAY_COMPLETE.getDescription())) {
            // 胎圈日完成量回报
            AjaxResult result = finishService.mergeTqDayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.GSQ_DAY_COMPLETE.getDescription())) {
            // 钢丝圈日完成量回报
            AjaxResult result = finishService.mergeGsqDayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.NC_DAY_COMPLETE.getDescription())) {
            // 内衬日完成量回报
            AjaxResult result = finishService.mergeNcDayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.CD15_DAY_COMPLETE.getDescription())) {
            // 15度裁断日完成量回报
            AjaxResult result = finishService.mergeCd15DayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.CD90_DAY_COMPLETE.getDescription())) {
            // 90度裁断日完成量回报
            AjaxResult result = finishService.mergeCd90DayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.XWYY_DAY_COMPLETE.getDescription())) {
            // 钢丝圈日完成量回报
            AjaxResult result = finishService.mergeXwyyDayFinish(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        }
        else if (syncKey.equals(SyncKeyEnum.EMBRYO_BAD_QUANTITY.getDescription())) {
            // ========================================================================  胚胎不良量  ============================================================
            // 胚胎不良量
            AjaxResult result = badNumService.mergeBadNum(dataVersion);
            if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
            }
            result1 = result;
        } else if (syncKey.equals(SyncKeyEnum.MPS_TO_APS_FAC.getDescription())) {
            // ========================================================================  主计划同步  ============================================================
            // 抽取到APS系统中待投产数据
            // 获取生产排程版本
            int exist = monthPlanSumService.checkMpsExist(Integer.parseInt(year), Integer.parseInt(month), dataVersion);
            if (exist == 0) {
                log.setServiceStatus("1");
                log.setServiceResult("T_SYNC_MPS_2_APS_FAC表数据不存在：year:" + year + " month:" + month + " dataVersion:" + dataVersion);
            } else {
                // 月度计划汇总
                AjaxResult result = monthPlanSumService.monthPlanAmountSum(dataVersion, year, month, Integer.parseInt(isFinal));
                if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                    log.setServiceResult((String) result.get(GatewayConstants.MSG_TAG));
                }
                result1 = result;
            }
        } else {
            log.setServiceStatus("1");
            log.setServiceResult(I18nUtil.getMessage("mes.error.message.syncKey") + syncKey);
        }
        logService.addLog(log);
        if (log.getServiceStatus().equals("1")) {
            return AjaxResult.success("调用失败");
        }
        if (!CollectionUtil.isEmpty(result1)) {
            return result1;
        }
        return AjaxResult.success("调用成功");
    }
}
