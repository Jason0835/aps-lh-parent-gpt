package com.zlt.aps.mp.demand.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.maindata.mapper.DpAreaEntityMapper;
import com.zlt.aps.maindata.mapper.DpNationEntityMapper;
import com.zlt.aps.mp.api.domain.entity.DpArea;
import com.zlt.aps.mp.api.domain.entity.DpNation;
import com.zlt.aps.mp.api.domain.entity.SalesOrderPool;
import com.zlt.aps.mp.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.mp.demand.service.ISalesOrderPoolService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;
import com.zlt.common.utils.PubUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：SalesOrderPoolController.java
* 描    述：销售订单池 控制层类：....
*@author zlt
*@date 2025-12-04
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "销售订单池")
@RestController
@RequestMapping("/SalesOrderPool")
public class SalesOrderPoolController extends AbstractDocBizController<SalesOrderPool> {

    @Autowired
    private ISalesOrderPoolService salesOrderPoolService;

    @Autowired
    private SalesOrderPoolEntityMapper entityMapper;
    
	@Autowired
	private DpAreaEntityMapper dpAreaEntityMapper;
	@Autowired
	private DpNationEntityMapper dpNationEntityMapper;

    /**
     * 查询销售订单池列表
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody SalesOrderPool queryVO) {
    	TableDataInfo tableResult = super.list(queryVO);
		this.translationList((List<SalesOrderPool>)tableResult.getRows());
        return tableResult;
    }

    @Override
    protected String getOrderBy() {
        return "bill_date desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "monthplan:SalesOrderPool:edit")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody SalesOrderPool billVO){
        return super.save(billVO);
    }

    /**
     *  批量修改同PO号的销售优先级
     */
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "monthplan:SalesOrderPool:edit")
    @ApiOperation("批量修改同PO号的销售优先级")
    @PostMapping("/editBySalCodePo")
    public AjaxResult editBySalCodePo(@RequestBody SalesOrderPool billVO){
        return salesOrderPoolService.editBySalCodePo(billVO);
    }

	/**
	 * 锁定订单池
	 * @return
	 */
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "monthplan:SalesOrderPool:lock")
    @ApiOperation("锁定订单池")
    @PostMapping("/lockSalesOrderPool")
    public AjaxResult lockSalesOrderPool(@RequestBody SalesOrderPool billVO){
        return salesOrderPoolService.lockSalesOrderPool(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "monthplan:SalesOrderPool:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取销售订单池详细信息
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public SalesOrderPool getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入销售订单池数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:import")
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:export")
    @Log(title = "销售订单池", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody SalesOrderPool queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<SalesOrderPool> listExportData(SalesOrderPool obj) {
        QueryWrapper<SalesOrderPool> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return translationList(entityMapper.selectList(wrapper));
    }
    
    /**
     * 检查SCM数据
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:getSCMData")
    @ApiOperation("检查SCM数据")
    @PostMapping("/checkSCMData")
    public AjaxResult checkSCMData(@RequestBody SalesOrderPool salesOrderPool){
        return salesOrderPoolService.checkSCMData(salesOrderPool);
    }
    
    /**
     * 抓取SCM数据
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:getSCMData")
    @ApiOperation("抓取SCM数据")
    @PostMapping("/getSCMData")
    @DistributedLock(key = "'redissonLock:salesOrderPool:getSCMData:' + #salesOrderPool.factoryCode + #salesOrderPool.year + #salesOrderPool.month",
        failMsg = "ui.data.alert.SalesOrderPool.getSCMData.lock",
        args = {"#salesOrderPool.factoryCode", "#salesOrderPool.year", "#salesOrderPool.month"},
        waitTime = 1,
        leaseTime = 300
    )
    public AjaxResult getSCMData(@RequestBody SalesOrderPool salesOrderPool){
        return salesOrderPoolService.getSCMData(salesOrderPool);
    }

    @Override
    protected IDocService getDocService(){
        return salesOrderPoolService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<SalesOrderPool> queryWrapper, SalesOrderPool queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productType")), "PRODUCT_TYPE", queryVO.getFieldValueByFieldName("productType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderPriority")), "ORDER_PRIORITY", queryVO.getFieldValueByFieldName("orderPriority"));
        queryWrapper.exists(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("area")),"SELECT 0 FROM T_DP_AREA t WHERE t.AREA_CODE = T_DP_SALES_ORDER_POOL.AREA AND AREA_NAME like concat('%', {0}, '%')", queryVO.getFieldValueByFieldName("area"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salCode")), "SAL_CODE", queryVO.getFieldValueByFieldName("salCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salNCode")), "SAL_N_CODE", queryVO.getFieldValueByFieldName("salNCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("natCode")), "NAT_CODE", queryVO.getFieldValueByFieldName("natCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salCodePo")), "SAL_CODE_PO", queryVO.getFieldValueByFieldName("salCodePo"));
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("billDateStartTime")), "BILL_DATE", queryVO.getBillDateStartTime());
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("billDateEndTime")), "BILL_DATE", queryVO.getBillDateEndTime());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriMaterialCode")), "ORI_MATERIAL_CODE", queryVO.getFieldValueByFieldName("oriMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("ordQty")), "ORD_QTY", queryVO.getFieldValueByFieldName("ordQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("weekYear")), "WEEK_YEAR", queryVO.getFieldValueByFieldName("weekYear"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDynamicBalance")), "IS_DYNAMIC_BALANCE", queryVO.getFieldValueByFieldName("isDynamicBalance"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isUniformity")), "IS_UNIFORMITY", queryVO.getFieldValueByFieldName("isUniformity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEudr")), "IS_EUDR", queryVO.getFieldValueByFieldName("isEudr"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliverGoodsType")), "DELIVER_GOODS_TYPE", queryVO.getFieldValueByFieldName("deliverGoodsType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmPriority")), "SCM_PRIORITY", queryVO.getFieldValueByFieldName("scmPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmDetailId")), "SCM_DETAIL_ID", queryVO.getFieldValueByFieldName("scmDetailId"));
    }


    @Override
    protected String getTypeCode(){
        return "DP0202";
    }

    /**
     * 翻译列表
     * @param resultList
     */
	private List<SalesOrderPool> translationList(List<SalesOrderPool> resultList) {
		// 加载区域
		List<DpArea> dpAreaList = dpAreaEntityMapper.selectList(new LambdaQueryWrapper<>());
		JsonI18nConvertUtils.conventJsonI18n(dpAreaList, DpArea.class);
		Map<String, String> areaMap = dpAreaList.stream()
				.collect(Collectors.toMap(DpArea::getAreaCode, DpArea::getAreaNameI18n));
		// 加载国家地区
		List<DpNation> dpNationList = dpNationEntityMapper.selectList(new LambdaQueryWrapper<>());
		JsonI18nConvertUtils.conventJsonI18n(dpNationList, DpNation.class);
		Map<String, String> nationMap = dpNationList.stream()
				.collect(Collectors.toMap(DpNation::getNationCode, DpNation::getNationNameI18n));
		for (SalesOrderPool item: resultList) {
			String salNCode = item.getSalNCode();
			String natCode = item.getNatCode();
			String area = item.getArea();
			item.setSalNCode(nationMap.getOrDefault(salNCode, ""));
			item.setNatCode(nationMap.getOrDefault(natCode, ""));
			item.setArea(areaMap.getOrDefault(area, ""));
			item.setUpdateTimeExport(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, item.getUpdateTime()));
		}
		return resultList;
	}
}
