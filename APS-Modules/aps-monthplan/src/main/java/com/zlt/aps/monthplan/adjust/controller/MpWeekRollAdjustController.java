package com.zlt.aps.monthplan.adjust.controller;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.monthplan.adjust.service.impl.MpWeekAdjustFactory;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.dto.MpWeekRollAdjustDTO;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 获取调整明细列表
     */
    @ApiOperation("获取调整明细列表")
    @PostMapping("/getAdjustDetailList")
    @DistributedLock(
            key = "'ADJ:GET:' #weekRollAdjustDTO.adjustType + #weekRollAdjustDTO.mpYear + #weekRollAdjustDTO.mpMonth",
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
            IMpWeekAdjustService weekAdjustStrategy = mpWeekAdjustFactory.getStrategy(weekRollAdjustDTO.getAdjustType());
            if (weekAdjustStrategy == null) {
                throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindStrategy"));
            }
            // 构建上下文对象
            MpRollAdjustContextDTO contextDTO = buildContext(weekRollAdjustDTO);
            log.info("自动调整 ==> 开始执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
                    contextDTO.getMpYear() + "" + contextDTO.getMpMonth());
            // 执行周程滚动调整策略（自动调整）
            weekAdjustStrategy.autoAdjust(contextDTO);
            log.info("自动调整 ==> 完成执行策略:[{}] 年月:[{}]", WeekAdjustTypeEnum.getByCode(contextDTO.getAdjustType()).getName(),
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
        return BeanUtil.copyProperties(weekRollAdjustDTO, MpRollAdjustContextDTO.class);
    }

}
