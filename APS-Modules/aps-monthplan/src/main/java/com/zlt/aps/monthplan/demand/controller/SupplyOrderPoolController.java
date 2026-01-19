package com.zlt.aps.monthplan.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.tlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.api.domain.vo.AreaConvertVo;
import com.zlt.aps.monthplan.demand.mapper.SupplyOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：SupplyOrderPoolController.java
* 描    述：供应链订单池 控制层类：....
*@author yelq
*@date 2025-12-22
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：yelq
*     修改内容：...
*/
@Slf4j
@Api(tags = "供应链订单池")
@RestController
@RequestMapping("/supplyOrderPool")
public class SupplyOrderPoolController extends AbstractDocBizController<SupplyOrderPool> {

    @Autowired
    private ISupplyOrderPoolService supplyOrderPoolService;

    @Autowired
    private SupplyOrderPoolEntityMapper entityMapper;
    /**
     * 查询供应链订单池列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody SupplyOrderPool queryVO) {
        TableDataInfo tableResult = super.list(queryVO);
        if(CollectionUtils.isEmpty(tableResult.getRows())) {
            return tableResult;
        }
        this.translationList((List<SupplyOrderPool>)tableResult.getRows());
        return tableResult;
    }

    private void translationList(List<SupplyOrderPool> list) {
        // 把区域都转成名称
        List<AreaConvertVo> convertVoList = list.stream().filter(item -> StringUtils.isNotBlank(item.getSaleArea())).map(SupplyOrderPool::getSaleArea)
                .flatMap(item -> Arrays.stream(item.split(",")))
                .distinct()
                .filter(com.ruoyi.common.utils.StringUtils::isNotBlank)
                .map(item -> {
                    AreaConvertVo areaConvertVo = new AreaConvertVo();
                    areaConvertVo.setAreaCode(item);
                    return areaConvertVo;
                })
                .sorted(Comparator.comparing(AreaConvertVo::getAreaCode))
                .collect(Collectors.toList());
        Map<String, String> areaNameMap = getAreaNameMap(convertVoList);
        for (SupplyOrderPool supplyOrderPool : list) {
            String saleArea = supplyOrderPool.getSaleArea();
            if (StringUtils.isNotBlank(saleArea)){
                String[] areaSplitArr = saleArea.split(",");
                List<String> areaNameList = new ArrayList<>();
                for (String areaCode : areaSplitArr) {
                    if (areaNameMap.containsKey(areaCode)) {
                        String name = areaNameMap.get(areaCode);
                        areaNameList.add(name);
                    }
                }
                supplyOrderPool.setSaleAreaName(String.join(",", areaNameList));
            }
        }
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    private Map<String, String> getAreaNameMap(List<AreaConvertVo> convertVoList) {
        // 执行表达式，转义区域
        try {
            QueryFormulaUtil.execFormula(convertVoList, new String[]{
                    "areaCodeName->getcolvaluewithcondition(t_dp_area, area_name, area_code, areaCode, is_delete = 0)",
            });
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("转换区域，执行查询公式时发生错误.");
        }
        JsonI18nConvertUtils.conventJsonI18n(convertVoList, AreaConvertVo.class);
        return convertVoList.stream().filter(item -> com.ruoyi.common.utils.StringUtils.isNotBlank(item.getAreaCodeNameI18n()))
                .collect(Collectors.toMap(AreaConvertVo::getAreaCode, AreaConvertVo::getAreaCodeNameI18n, (k1, k2) -> k1));
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody SupplyOrderPool billVO){
        //  (1).根据SKU、订单类型进行唯一性校验，如果存在，提示信息"xxx物料的周期排产/常规储备已经存在，请确认"，系统不做处理
        //  (2). 根据选择的储备类型校验近12个月是否出现过超期周期排产储备/超期常规储备，如果出现过，则提示信息“近12个月有出现过超期胎，不可新增”
        supplyOrderPoolService.checkUnique(billVO);

        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取供应链订单池详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public SupplyOrderPool getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入供应链订单池数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "供应链订单池", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody SupplyOrderPool queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<SupplyOrderPool> listExportData(SupplyOrderPool obj) {
        QueryWrapper<SupplyOrderPool> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<SupplyOrderPool> list = entityMapper.selectList(wrapper);
        this.translationList(list);
        return list;
    }


    @ApiOperation("生成周期排产储备")
    @RedissonLockAnno(uniqueMark = "redissonLock:supplyOrderPool:createCycleStockUp:",
        msgKey = "ui.data.alert.createCycleStockUp.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createCycleStockUp")
    public AjaxResult createCycleStockUp(@RequestBody SupplyOrderPool supplyOrderPool) throws InterruptedException {
        supplyOrderPoolService.createCycleStockUp(supplyOrderPool);
        return AjaxResult.success();
    }

    @ApiOperation("生成常规储备")
    @RedissonLockAnno(uniqueMark = "redissonLock:supplyOrderPool:createPrecedentStockUp:",
        msgKey = "ui.data.alert.createPrecedentStockUp.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createPrecedentStockUp")
    public AjaxResult createPrecedentStockUp(@RequestBody SupplyOrderPool supplyOrderPool){
        supplyOrderPoolService.createPrecedentStockUp(supplyOrderPool);
        return AjaxResult.success();
    }

    /**
     * 新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少
     * @param supplyOrderPool 入参
     * @return AjaxResult
     */
    @ApiOperation("新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少")
    @PostMapping("/queryStockUpByMaterialCode")
    public AjaxResult queryStockUpByMaterialCode(@RequestBody SupplyOrderPool supplyOrderPool){
        return AjaxResult.success(supplyOrderPoolService.calculateStockMsg(supplyOrderPool));
    }

    /**
     * 输入物料编码，带出对应信息
     */
    @ApiOperation("输入物料编码，带出对应信息")
    @PostMapping("/queryRelationByMaterialCode")
    public AjaxResult queryRelationByMaterialCode(@RequestBody SupplyOrderPool supplyOrderPool)
    {
        return AjaxResult.success(supplyOrderPoolService.queryRelationByMaterialCode(supplyOrderPool));
    }

    /**
     * 超期校验
     */
    @ApiOperation("超期校验")
    @PostMapping("/checkOverdue")
    public AjaxResult checkOverdue(@RequestBody SupplyOrderPool supplyOrderPool)
    {
        return supplyOrderPoolService.checkOverdue(supplyOrderPool);
    }

    @Override
    protected IDocService getDocService(){
        return supplyOrderPoolService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<SupplyOrderPool> queryWrapper, SupplyOrderPool queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCategory")), "PRODUCT_CATEGORY", queryVO.getFieldValueByFieldName("productCategory"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderType")), "ORDER_TYPE", queryVO.getFieldValueByFieldName("orderType"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("saleArea")), "SALE_AREA", queryVO.getFieldValueByFieldName("saleArea"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("qty")), "QTY", queryVO.getFieldValueByFieldName("qty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("threeAverageQty")), "THREE_AVERAGE_QTY", queryVO.getFieldValueByFieldName("threeAverageQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sixAverageQty")), "SIX_AVERAGE_QTY", queryVO.getFieldValueByFieldName("sixAverageQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliveryFrequency")), "DELIVERY_FREQUENCY", queryVO.getFieldValueByFieldName("deliveryFrequency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureFrequency")), "STRUCTURE_FREQUENCY", queryVO.getFieldValueByFieldName("structureFrequency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("threeOverdueStockQty")), "THREE_OVERDUE_STOCK_QTY", queryVO.getFieldValueByFieldName("threeOverdueStockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sixOverdueStockQty")), "SIX_OVERDUE_STOCK_QTY", queryVO.getFieldValueByFieldName("sixOverdueStockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nightOverdueStockQty")), "NIGHT_OVERDUE_STOCK_QTY", queryVO.getFieldValueByFieldName("nightOverdueStockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("twelveOverdueStockQty")), "TWELVE_OVERDUE_STOCK_QTY", queryVO.getFieldValueByFieldName("twelveOverdueStockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockLimit")), "STOCK_LIMIT", queryVO.getFieldValueByFieldName("stockLimit"));
        queryWrapper.eq("SOURCE_TYPE", ProductionPlanType.NORMAL.getPlanType());
    }


    @Override
    protected String getTypeCode(){
        return "2025122214";
    }


}
