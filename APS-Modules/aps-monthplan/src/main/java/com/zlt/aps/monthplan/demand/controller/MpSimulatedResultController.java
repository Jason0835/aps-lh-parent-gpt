package com.zlt.aps.monthplan.demand.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.zlt.aps.monthplan.api.domain.entity.MpPredictionDetail;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.aps.monthplan.demand.mapper.MpSimulatedResultEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpPredictionDetailService;
import com.zlt.aps.monthplan.demand.service.IMpSimulatedResultService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSimulatedResultController.java
 * 描    述：S2-1004.实单模拟排产 控制层类：....
 *
 * @author yelq
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：yelq
 * 修改内容：...
 * @date 2025-12-31
 */
@Slf4j
@Api(tags = "S2-1004.实单模拟排产")
@RestController
@RequestMapping("/simulatedResult")
@AllArgsConstructor
public class MpSimulatedResultController extends AbstractDocBizController<MpSimulatedResult> {
    private final IMpSimulatedResultService mpSimulatedResultService;
    private final MpSimulatedResultEntityMapper entityMapper;
    private final IMpPredictionDetailService mpPredictionDetailService;

    /**
     * 查询S2-1004.实单模拟排产列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpSimulatedResult queryVO) {
        TableDataInfo tableResult = super.list(queryVO);
        if(CollectionUtils.isEmpty(tableResult.getRows())) {
            return tableResult;
        }
        this.translationList((List<MpSimulatedResult>)tableResult.getRows(),true);
        return tableResult;
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.simulatedResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpSimulatedResult billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.simulatedResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取S2-1004.实单模拟排产详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpSimulatedResult getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入S2-1004.实单模拟排产数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.simulatedResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "S2-1004.实单模拟排产", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpSimulatedResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 实单模拟排产
     */
    @ApiOperation("实单模拟排产")
    @RedissonLockAnno(uniqueMark = "redissonLock:simulatedResult:createVmMonthPrediction:",
            expressions = {"#createCondition.factoryCode", "#createCondition.year", "#createCondition.month"},
            msgKey = "ui.data.alert.createVmMonthPrediction.run",
            waitTime = 5,
            leaseTime = 300
    )
    @PostMapping("/createVmMonthPrediction")
    public AjaxResult createVmMonthPrediction(@RequestBody MpSimulatedResult createCondition) throws InterruptedException {
        return this.mpSimulatedResultService.createVmMonthPrediction(createCondition);
    }

    @Override
    protected List<MpSimulatedResult> listExportData(MpSimulatedResult obj) {
        QueryWrapper<MpSimulatedResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return translationList(entityMapper.selectList(wrapper),false);
    }

    /**
     * 翻译列表
     *
     * @param resultList
     */
    private List<MpSimulatedResult> translationList(List<MpSimulatedResult> resultList,boolean fetchVersion) {
        if(!fetchVersion) {
            for (MpSimulatedResult item : resultList) {
                item.setUpdateDate(DateUtil.formatDateTime(item.getUpdateTime()));
            }
            return resultList;
        }
        Set<String> batchNumbers = resultList.stream().map(MpSimulatedResult::getMonthPlanVersion).collect(Collectors.toSet());
        Map<String,Map<String, MpPredictionDetail>> versionMap = this.mpPredictionDetailService.fetchVersion(batchNumbers);
        for (MpSimulatedResult item : resultList) {
            item.setVersionMap(versionMap.get(item.getMonthPlanVersion()));
        }
        return resultList;
    }

    @Override
    protected IDocService getDocService() {
        return mpSimulatedResultService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpSimulatedResult> queryWrapper, MpSimulatedResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainPattern")), "MAIN_PATTERN", queryVO.getFieldValueByFieldName("mainPattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainMaterialDesc")), "MAIN_MATERIAL_DESC", queryVO.getFieldValueByFieldName("mainMaterialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldQty")), "MOULD_QTY", queryVO.getFieldValueByFieldName("mouldQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("typeBlockQty")), "TYPE_BLOCK_QTY", queryVO.getFieldValueByFieldName("typeBlockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("netQty")), "NET_QTY", queryVO.getFieldValueByFieldName("netQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightQty")), "HEIGHT_QTY", queryVO.getFieldValueByFieldName("heightQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionQty")), "PRODUCTION_QTY", queryVO.getFieldValueByFieldName("productionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month1")), "MONTH_1", queryVO.getFieldValueByFieldName("month1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month2")), "MONTH_2", queryVO.getFieldValueByFieldName("month2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month3")), "MONTH_3", queryVO.getFieldValueByFieldName("month3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month4")), "MONTH_4", queryVO.getFieldValueByFieldName("month4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month5")), "MONTH_5", queryVO.getFieldValueByFieldName("month5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month6")), "MONTH_6", queryVO.getFieldValueByFieldName("month6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month7")), "MONTH_7", queryVO.getFieldValueByFieldName("month7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month8")), "MONTH_8", queryVO.getFieldValueByFieldName("month8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month9")), "MONTH_9", queryVO.getFieldValueByFieldName("month9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month10")), "MONTH_10", queryVO.getFieldValueByFieldName("month10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month11")), "MONTH_11", queryVO.getFieldValueByFieldName("month11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month12")), "MONTH_12", queryVO.getFieldValueByFieldName("month12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month13")), "MONTH_13", queryVO.getFieldValueByFieldName("month13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month14")), "MONTH_14", queryVO.getFieldValueByFieldName("month14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month15")), "MONTH_15", queryVO.getFieldValueByFieldName("month15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month16")), "MONTH_16", queryVO.getFieldValueByFieldName("month16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month17")), "MONTH_17", queryVO.getFieldValueByFieldName("month17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month18")), "MONTH_18", queryVO.getFieldValueByFieldName("month18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month19")), "MONTH_19", queryVO.getFieldValueByFieldName("month19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month20")), "MONTH_20", queryVO.getFieldValueByFieldName("month20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month21")), "MONTH_21", queryVO.getFieldValueByFieldName("month21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month22")), "MONTH_22", queryVO.getFieldValueByFieldName("month22"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month23")), "MONTH_23", queryVO.getFieldValueByFieldName("month23"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month24")), "MONTH_24", queryVO.getFieldValueByFieldName("month24"));
    }


    @Override
    protected String getTypeCode() {
        return "2025123114";
    }


}
