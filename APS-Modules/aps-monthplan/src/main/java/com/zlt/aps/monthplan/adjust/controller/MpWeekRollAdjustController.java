package com.zlt.aps.monthplan.adjust.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.monthplan.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.monthplan.adjust.service.impl.MpWeekAdjustFactory;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.dto.MpWeekRollAdjustDTO;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import com.zlt.aps.monthplan.common.utils.PubUtil;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProdFinalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
        MpRollAdjustContextDTO contextDTO = buildContext(weekRollAdjustDTO);
        log.info("获取调整明细 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                contextDTO.getMpYear() + "" + contextDTO.getMpMonth());
        // 执行周程滚动调整策略（生成调整明细）
        weekAdjustStrategy.generateAdjust(contextDTO);
        log.info("获取调整明细 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                contextDTO.getMpYear() + "" + contextDTO.getMpMonth());
        // 返回结果处理
        return getDataTable(contextDTO.getAdjustDetailList());
    }

    /**
     * 自动调整
     */
    @ApiOperation("自动调整")
    @PostMapping("/autoAdjust")
    public AjaxResult autoAdjust(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO) {
        String key = ApsConstant.REDIS_ADJUST_STRUCT_IN_AUTO + weekRollAdjustDTO.getFactoryCode();
        if (ApsConstant.TRUE.equals(redisService.getCacheObject(key))) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.distributed.lock.fail"));
        }
        redisService.setCacheObject(key, ApsConstant.TRUE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        try{
            // 获取周程滚动调整策略
            //IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy(weekRollAdjustDTO.getAdjustType());
            IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy("01");
            if (weekAdjustStrategy == null) {
                throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindStrategy"));
            }
            // 构建上下文对象
            MpRollAdjustContextDTO contextDTO = buildContext(weekRollAdjustDTO);
            log.info("自动调整 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode("01").getName(),
                    contextDTO.getMpYear() + "" + contextDTO.getMpMonth());
            // 执行周程滚动调整策略（自动调整）
            weekAdjustStrategy.autoAdjust(contextDTO);
            log.info("自动调整 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode("01").getName(),
                    contextDTO.getMpYear() + "" + contextDTO.getMpMonth());
            return AjaxResult.success();
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
        MpRollAdjustContextDTO contextDTO = buildContext(weekRollAdjustDTO);
        log.info("确认调整 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                contextDTO.getMpYear() + "" + contextDTO.getMpMonth());
        // 执行周程滚动调整策略（确认调整）
        weekAdjustStrategy.confirmAdjust(contextDTO);
        log.info("确认调整 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                contextDTO.getMpYear() + "" + contextDTO.getMpMonth());
        return AjaxResult.success();
    }


    /**
     * 构建上下文对象
     * @param weekRollAdjustDTO
     * @return
     */
    private MpRollAdjustContextDTO buildContext(MpWeekRollAdjustDTO weekRollAdjustDTO) {
        MpRollAdjustContextDTO contextDTO = BeanUtil.copyProperties(weekRollAdjustDTO, MpRollAdjustContextDTO.class);
        contextDTO.setFactoryMonthPlanProdFinalList(mpAdjustStructureInService.selectMpFinalList(contextDTO));
        if (PubUtil.isNotEmpty(contextDTO.getFactoryMonthPlanProdFinalList())){
            FactoryMonthPlanFinalAdjustVo firstFinalVo = contextDTO.getFactoryMonthPlanProdFinalList().get(0);
            contextDTO.setMonthPlanVersion(firstFinalVo.getProductionVersion());
            contextDTO.setProductType(firstFinalVo.getProductTypeCode());
        }
        contextDTO.setStructureAllocationList(mpAdjustStructureInService.selectMpStructureAllocationList(contextDTO));
        //当日作为调整日
        contextDTO.setAdjustDay(DateUtils.getDay(DateUtils.getNowDate()));
        contextDTO.setParamMap(mpAdjustStructureInService.getMpWeekAdjustParam(contextDTO.getFactoryCode(),contextDTO.getProductType()));
        //初始锁定日
        contextDTO.setLockEndDay(mpAdjustStructureInService.getLockEndDay(contextDTO));
        //初始结构收尾日
        contextDTO.setStructureDeadLine(mpAdjustStructureInService.getStructureDeadline(contextDTO));

        //测试数据
        //TODO sandy
        MpAdjustStructureIn structureIn = new MpAdjustStructureIn();
        structureIn.setMaterialCode("3302001884");
        structureIn.setMaterialDesc("215/75R17.5 135/133L 16PR BF188 BL3EBL");
        structureIn.setStructureName("245/70R19.5");
        structureIn.setPreviousNetQty(800);
        structureIn.setCurrentNetQty(1400);
        structureIn.setNetQtyChange(600);
        structureIn.setMonthScheduledQty(800);
        structureIn.setPendingQty(1000);
        structureIn.setConfirmAdjustQty(1000);

        MpAdjustStructureIn structureIn2 = new MpAdjustStructureIn();
        structureIn2.setMaterialCode("3302001162");
        structureIn2.setMaterialDesc("245/70R19.5 144/142J 18PR BF188 BL3EBL");
        structureIn2.setStructureName("245/70R19.5");
        structureIn2.setPreviousNetQty(1000);
        structureIn2.setCurrentNetQty(600);
        structureIn2.setNetQtyChange(-400);
        structureIn2.setMonthScheduledQty(1000);
        structureIn2.setPendingQty(-400);
        structureIn2.setConfirmAdjustQty(-400);

        List<MpAdjustStructureIn> mpAdjustStructureInList = new ArrayList<>();
        mpAdjustStructureInList.add(structureIn);
        mpAdjustStructureInList.add(structureIn2);
        contextDTO.setMpAdjustStructureInList(mpAdjustStructureInList);
        return contextDTO;
    }

}
