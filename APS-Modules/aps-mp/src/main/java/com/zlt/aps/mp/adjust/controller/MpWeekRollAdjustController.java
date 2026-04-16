package com.zlt.aps.mp.adjust.controller;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.mp.adjust.service.impl.MpWeekAdjustFactory;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.mp.common.utils.StringUtil;
import com.zlt.aps.mp.engine.scheduling.matching.MatchingAdjuestProductionHandler;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.mp.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.dto.MpWeekRollAdjustDTO;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.api.enums.WeekAdjustTypeEnum;
import com.zlt.common.utils.PubUtil;
import com.zlt.msg.message.api.IMsgTemplateRemoteService;
import com.zlt.msg.message.domain.entity.MsgTemplate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Autowired
    private MatchingAdjuestProductionHandler matchingAdjuestProductionHandler;


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
            // 排序 按英寸->结构->最大型腔数->主花纹->活块数->物料描述
            sortAdjustResultList(contextDTO.getAdjustResultList());
            return AjaxResult.success(contextDTO.getAdjustResultList());
        }finally {
            redisService.setCacheObject(key, ApsConstant.FALSE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        }
    }

    /**
     * 排序：按英寸->结构->最大型腔数->主花纹->活块数->物料描述
     * @param mpAdjustResultList
     */
    protected void sortAdjustResultList(List<MpAdjustResult> mpAdjustResultList) {
        if (PubUtil.isEmpty(mpAdjustResultList)) {
            return;
        }
        // 主花纹的最大型腔数
        Map<String, Integer> maxMouldCavityQtyMap = new HashMap<>();
        for (MpAdjustResult adjustResult: mpAdjustResultList) {
            //记录主花纹的最大型腔数
            Integer maxMouldCavityQty = maxMouldCavityQtyMap.getOrDefault(adjustResult.getMainPattern(), 0);
            maxMouldCavityQtyMap.put(adjustResult.getMainPattern(), Math.max(maxMouldCavityQty, adjustResult.getMouldCavityQty()));
        }
        mpAdjustResultList.stream().forEach(s -> { // 设置对应的最大型腔数和最大活块数
            s.setMaxMouldCavityQty(maxMouldCavityQtyMap.getOrDefault(s.getMainPattern(), 0));
        });

        Collections.sort(mpAdjustResultList, getAdjustResultSortComparator());
    }

    /**
     * 排序器：按英寸->结构->最大型腔数->主花纹->活块数->物料描述
     * @return
     */
    protected Comparator<MpAdjustResult> getAdjustResultSortComparator() {
        // 一级排序：结构名称升序，空值排最后
        return Comparator.comparing(MpAdjustResult::getTbrProSize, Comparator.nullsLast(String::compareTo))
                .thenComparing(MpAdjustResult::getStructureName,Comparator.nullsLast(String::compareTo))
                // 最大型腔数
                .thenComparing(MpAdjustResult::getMaxMouldCavityQty, Comparator.reverseOrder())
                // 主花纹
                .thenComparing(MpAdjustResult::getMainPattern, Comparator.nullsLast(String::compareTo))
                // 活块数
                .thenComparing(MpAdjustResult::getTypeBlockQty, Comparator.reverseOrder())
                // 物料描述
                .thenComparing(MpAdjustResult::getMaterialDesc, Comparator.nullsLast(String::compareTo));
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
        //周期结构最低硫化机台数
        contextDTO.setCycleStructureMinLhMachinesMap(mpAdjustStructureInService.getCycleStructureMinMachinesMap(contextDTO));
        //初始型腔与活块数量
        contextDTO.setCavity2BlockMap(mpAdjustStructureInService.getCavityAndBlockQtyMap(contextDTO));
        //初始消息模板
        initMsgTemplate(contextDTO);
        //初始调整过程日志
        contextDTO.setAdjustProcLogList(new ArrayList<>());
        //初始工厂名称
        List<SysDictData> dictDataList = iSysDictDataCacheService.getType("biz_factory_name");
        String factoryName = dictDataList.stream().filter(dictData -> dictData.getDictValue().equals(contextDTO.getFactoryCode())).findFirst().get().getDictLabel();
        contextDTO.setFactoryName(factoryName);
        //初始总的硫化机台数
        contextDTO.setTotalLhMachines(mpAdjustStructureInService.getLhMachineCount(contextDTO));
        //设置OEM配置集合
        initOemParam(contextDTO);
        //设置结构统计
        contextDTO.setStructureStatisticMap(mpAdjustStructureInService.loadMpMonthPlanStatistics(contextDTO));

        // 加载搭配排产的必要基础数据
        matchingAdjuestProductionHandler.initAdjustContextDTO(contextDTO);
        return contextDTO;
    }

    /**
     * 初始消息模板
     * @param contextDTO
     */
    private void initMsgTemplate(MpRollAdjustContextDTO contextDTO) {
        //1. SKU原余量小于调整次日至锁定截止日的计划量提醒
        MsgTemplate msgTemplate = templateRemoteService.getTemplateInfo(MsgTemplateEnums.MP_SKU_REMAIN_QTY_NO_FULL.getCode());
        if (msgTemplate != null){
            contextDTO.setMsgTemplateWithRemainQtyNoFull(msgTemplate.getContent());
        }
        contextDTO.setMsgRemainQtyNoFull(new StringBuilder());
        //2. 结构内调整减量提前收尾
        msgTemplate = templateRemoteService.getTemplateInfo(MsgTemplateEnums.MP_STRUCTURE_ADJUST_PRE_CLOSE.getCode());
        if (msgTemplate != null){
            contextDTO.setMsgTemplateWithStructureAdjustPreClose(msgTemplate.getContent());
        }
        contextDTO.setMsgStructureAdjustPreClose(new StringBuilder());
    }

    /**
     * 初始OEM相关参数
     * @param contextDTO
     */
    private void initOemParam(MpRollAdjustContextDTO contextDTO) {
        String oemBrandConfig = (String) contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CONFIG.getCode());
        Set<String> oemBrandConfigSet = Collections.emptySet();
        if (!StringUtil.isEmptyWithTrim(oemBrandConfig)){
            oemBrandConfigSet = Stream.of(oemBrandConfig.split(StringConstant.COMMA)).collect(Collectors.toSet());
        }
        contextDTO.setOemBrandConfigSet(oemBrandConfigSet);
        if (contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CAPACITY.getCode()) == null){
            contextDTO.setTotalOemQty(0);
        }else{
            contextDTO.setTotalOemQty((Integer) contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CAPACITY.getCode()));
        }
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
