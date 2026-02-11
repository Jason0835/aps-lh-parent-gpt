package com.zlt.aps.monthplan.adjust.controller;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.monthplan.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.monthplan.adjust.service.impl.MpWeekAdjustFactory;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.dto.MpWeekRollAdjustDTO;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import com.zlt.aps.monthplan.common.utils.StringUtil;
import com.zlt.common.utils.PubUtil;
import com.zlt.msg.message.api.IMsgTemplateRemoteService;
import com.zlt.msg.message.domain.entity.MsgTemplate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpWeekRollAdjustController.java
* 描    述：周程滚动调整 控制层类：....
*@author zlt
*@date 2025-12-24
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "周程滚动调整")
@RestController
@RequestMapping("/mpWeekRollAdjust")
public class MpWeekRollAdjustController extends BaseController {

    @Autowired
    private MpWeekAdjustFactory mpWeekAdjustFactory;

    @Autowired
    private RedisService redisService;

    @Autowired
    private IMpAdjustStructureInService mpAdjustStructureInService;

    @Autowired
    private IMsgTemplateRemoteService templateRemoteService;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;


    /**
     * 获取调整明细列表
     */
    @ApiOperation("获取调整明细列表")
    @PostMapping("/getAdjustDetailList")
    @DistributedLock(
            key = "'ADJ:GET:' + #weekRollAdjustDTO.adjustType + #weekRollAdjustDTO.mpYear + #weekRollAdjustDTO.mpMonth",
            waitTime = 0,
            leaseTime = -1,
            failMsg = "ui.data.alert.getAdjustDetail.run",
            args = {"#weekRollAdjustDTO.mpYear","#weekRollAdjustDTO.mpMonth"}
    )
    public TableDataInfo getAdjustDetailList(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO) {
        // 获取周程滚动调整策略
        IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy(weekRollAdjustDTO.getAdjustType());
        if (weekAdjustStrategy == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindStrategy"));
        }
        // 构建上下文对象
        MpRollAdjustContextDTO contextDTO = buildAdjustContext(weekRollAdjustDTO);
        log.info("获取调整明细 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
        // 执行周程滚动调整策略（生成调整明细）
        weekAdjustStrategy.generateAdjust(contextDTO);
        log.info("获取调整明细 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
        // 返回结果处理
        return getDataTable(contextDTO.getAdjustDetailList());
    }

    /**
     * 自动调整
     */
    @ApiOperation("自动调整")
    @PostMapping("/autoAdjust")
    public AjaxResult autoAdjust(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO) {
        String key = ApsConstant.REDIS_ADJUST_STRUCT_AUTO + weekRollAdjustDTO.getFactoryCode()+weekRollAdjustDTO.getAdjustType();
        if (!StringUtil.isEmptyWithTrim(weekRollAdjustDTO.getScheduledMachines())){
            key = key+weekRollAdjustDTO.getScheduledMachines();
        }
        if (ApsConstant.TRUE.equals(redisService.getCacheObject(key))) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.distributed.lock.fail"));
        }
        redisService.setCacheObject(key, ApsConstant.TRUE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        try{
            // 获取周程滚动调整策略
            IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy(weekRollAdjustDTO.getAdjustType());
            if (weekAdjustStrategy == null) {
                throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindStrategy"));
            }
            // 构建上下文对象
            MpRollAdjustContextDTO contextDTO = buildAutoAdjustContext(weekRollAdjustDTO,weekAdjustStrategy);
            log.info("自动调整 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(weekRollAdjustDTO.getAdjustType()).getName(),
                    String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
            // 执行周程滚动调整策略（自动调整）
            weekAdjustStrategy.autoAdjust(contextDTO);
            log.info("自动调整 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(weekRollAdjustDTO.getAdjustType()).getName(),
                    String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
            return AjaxResult.success(contextDTO.getAdjustResultList());
        }finally {
            redisService.setCacheObject(key, ApsConstant.FALSE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        }
    }

    /**
     * 确认调整结果
     */
    @ApiOperation("确认调整结果")
    @PostMapping("/confirmAdjust")
    public AjaxResult confirmAdjust(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO) {
        // 获取周程滚动调整策略
        IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy(weekRollAdjustDTO.getAdjustType());
        if (weekAdjustStrategy == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindStrategy"));
        }
        // 构建上下文对象
        MpRollAdjustContextDTO contextDTO = buildAdjustContext(weekRollAdjustDTO);
        log.info("确认调整 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
        // 执行周程滚动调整策略（确认调整）
        weekAdjustStrategy.confirmAdjust(contextDTO);
        log.info("确认调整 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth()));
        return AjaxResult.success();
    }


    /**
     * 构建获取调整订单上下文对象
     * @param weekRollAdjustDTO
     * @return
     */
    private MpRollAdjustContextDTO buildAdjustContext(MpWeekRollAdjustDTO weekRollAdjustDTO) {
        MpRollAdjustContextDTO contextDTO = BeanUtil.copyProperties(weekRollAdjustDTO, MpRollAdjustContextDTO.class);
        return contextDTO;
    }
        /**
         * 构建自动调整上下文对象
         * @param weekRollAdjustDTO
         * @return
         */
    private MpRollAdjustContextDTO buildAutoAdjustContext(MpWeekRollAdjustDTO weekRollAdjustDTO, IMpWeekAdjustService weekAdjustStrategy) {
        MpRollAdjustContextDTO contextDTO = BeanUtil.copyProperties(weekRollAdjustDTO, MpRollAdjustContextDTO.class);
        //1.初始定稿版本信息
        weekAdjustStrategy.initVersion(contextDTO);
        //2.月计划定稿数据空检查
        if (PubUtil.isEmpty(contextDTO.getFactoryProductionVersionList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }
        MpFactoryProductionVersion firstVersion = contextDTO.getFactoryProductionVersionList().get(0);
        contextDTO.setMonthPlanVersion(firstVersion.getMonthPlanVersion());
        contextDTO.setProductType(firstVersion.getProductTypeCode());
        contextDTO.setProductionVersion(firstVersion.getProductionVersion());

        contextDTO.setFactoryMonthPlanProdFinalList(mpAdjustStructureInService.selectMpFinalList(contextDTO));

        contextDTO.setStructureAllocationList(mpAdjustStructureInService.selectMpStructureAllocationList(contextDTO));
        contextDTO.setParamMap(mpAdjustStructureInService.getMpWeekAdjustParam(contextDTO.getFactoryCode(),contextDTO.getProductType()));
        //设置调整日
        setAdjustDate(contextDTO);
        contextDTO.setVersion(weekRollAdjustDTO.getVersion());
        contextDTO.setAdjustType(weekRollAdjustDTO.getAdjustType());
        
        //结构硫化配比
        contextDTO.setStructureLhRatio(mpAdjustStructureInService.getStructureLhRatio(contextDTO));

        //初始工作日历
        contextDTO.setWorkCalendarMap(mpAdjustStructureInService.getWorkCalendarMap(contextDTO));
        //初始型腔与活块数量
        contextDTO.setCavity2BlockMap(mpAdjustStructureInService.getCavityAndBlockQtyMap(contextDTO));
        //初始消息模板
        MsgTemplate msgTemplate = templateRemoteService.getTemplateInfo(MsgTemplateEnums.MP_SKU_REMAIN_QTY_NO_FULL.getCode());
        if (msgTemplate != null){
            contextDTO.setMsgTemplateWithRemainQtyNoFull(msgTemplate.getContent());
        }
        contextDTO.setMsgRemainQtyNoFull(new StringBuilder());
        //初始调整过程日志
        contextDTO.setAdjustProcLogList(new ArrayList<>());
        //初始工厂名称
        List<SysDictData> dictDataList = iSysDictDataCacheService.getType("biz_factory_name");
        String factoryName = dictDataList.stream().filter(dictData -> dictData.getDictValue().equals(contextDTO.getFactoryCode())).findFirst().get().getDictLabel();
        contextDTO.setFactoryName(factoryName);
        return contextDTO;
    }

    /**
     * 设置调整日
     * @param contextDTO 周程滚动上下文
     */
    private void setAdjustDate(MpRollAdjustContextDTO contextDTO) {
        String weekRollAdjustDate = (String) contextDTO.getParamMap().get(MonthPlanEnums.WEEK_ROLL_ADJUST_DATE.getCode());
        Date adjustDate = StringUtil.isEmptyWithTrim(weekRollAdjustDate) ? DateUtils.getNowDate() : DateUtils.parseDate(weekRollAdjustDate);
        if (contextDTO.getMpMonth() != DateUtils.getMonth(adjustDate)){
            //若调整月不等于当前月，则将调整日设置1
            contextDTO.setAdjustDay(FactoryConstant.MONTH_START_DAY);
        }else{
            contextDTO.setAdjustDay(DateUtils.getDay(adjustDate));
        }
    }

}
