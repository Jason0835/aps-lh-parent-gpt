package com.zlt.aps.monthplan.demand.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tlt.aps.enums.YesOrNoEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.utils.AppUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.itf.scm.service.IScmItfService;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipResultVo;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmAreaEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmArea;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;

import lombok.extern.slf4j.Slf4j;

/**
 * Copyright (c) 2022, All rights reserved。 文件名称：SalesOrderPoolServiceImpl.java
 * 描 述：SalesOrderPoolServiceImpl销售订单池业务层处理
 * 
 * @author zlt
 * @date 2025-12-04
 * @version 1.0
 *
 *          修改记录： 修改时间：... 修 改 人：zlt 修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class SalesOrderPoolServiceImpl extends AbstractDocService<SalesOrderPool> implements ISalesOrderPoolService {
	@Autowired
	private IScmItfService iScmItfService;
	@Autowired
	private IFactoryParamService iFactoryParamService;

	@Autowired
	private SalesOrderPoolEntityMapper salesOrderPoolEntityMapper;
	@Autowired
	private MdmAreaEntityMapper mdmAreaEntityMapper;

	@Override
	protected String getDocTypeCode() {
		return "MP099";
	}

	@Override
	protected SysDocType getSysDocType() {
		SysDocType sysDocType = new SysDocType();
		sysDocType.setDocTypeCode("MP099");
		return sysDocType;
	}

	@Override
	public String checkUnique(SalesOrderPool docEntityVO) {
		String unique = super.checkUnique(docEntityVO);
		if (UserConstants.NOT_UNIQUE.equals(unique)) {
			throw new ServiceException(I18nUtil.getMessage("ui.data.alert.salesOrderPool.notUnique"));
		}
		return unique;
	}

	@Override
	protected List<String> getCheckUniqueFields() {
		// 唯一校验字段
		return Collections.emptyList();
	}

	/**
	 * 批量修改同PO号的销售优先级
	 * 
	 * @param salesOrderPool
	 * @return
	 */
	@Override
	public AjaxResult editBySalCodePo(SalesOrderPool salesOrderPool) {
		LambdaUpdateWrapper<SalesOrderPool> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(SalesOrderPool::getSalCodePo, salesOrderPool.getSalCodePo());
		updateWrapper.set(SalesOrderPool::getScmPriority, salesOrderPool.getScmPriority());
		salesOrderPoolEntityMapper.update(new SalesOrderPool(), updateWrapper);
		return AjaxResult.success();
	}

	/**
	 * 校验SCM已计划未发货数据
	 * 
	 * @param salesOrderPool
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AjaxResult checkSCMData(SalesOrderPool salesOrderPool) {
		// 通过接口获取供应链数据
		AjaxResult result = this.syncItfData(salesOrderPool);
		if (!AppUtils.checkAjaxSuccess(result)) {
			return result;
		}
		List<SyncPlanedNotShipResultVo> syncResultList = (List<SyncPlanedNotShipResultVo>) result
				.get(AjaxResult.DATA_TAG);
		if (CollectionUtils.isEmpty(syncResultList)) {
			return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.noScmData"));
		}

		// 加载区域
		LambdaQueryWrapper<MdmArea> areaQueryWrapper = new LambdaQueryWrapper<>();
		areaQueryWrapper.eq(MdmArea::getIsDelete, ApsConstant.APS_YES_NO_0);
		Map<String, MdmArea> areaMap = mdmAreaEntityMapper.selectList(areaQueryWrapper).stream()
				.collect(Collectors.toMap(MdmArea::getAreaCode, Function.identity()));

		// 校验是否有没有录入优先级的数据
		MdmArea defaultArea = new MdmArea();
		List<String> notPriorityAreaList = syncResultList.stream().filter(s -> StringUtils.isEmpty(s.getSalPriority()))
				.map(s -> areaMap.getOrDefault(s.getEmployeeDept(), defaultArea).getRemark()) // 先从备注获取名称
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());

		// 拼接警告信息
		StringBuilder warnMsg = new StringBuilder();
		String splitFlag = notPriorityAreaList.size() > 1 ? "\n" : ""; // 如果有多个区域没有维护优先级，则需要换行提示
		for (String area : notPriorityAreaList) {
			warnMsg.append(String.format(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.notPriorityArea"), area))
					.append(splitFlag);
		}
		if (warnMsg.length() > 0) {
			warnMsg.append(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.isContinue"));
			return AjaxResult.success(warnMsg.toString(), ApsConstant.APS_YES_NO_1);
		}

		// 校验通过，直接做保存操作
		return this.saveItfData(salesOrderPool, syncResultList);
	}

	/**
	 * 抓取SCM已计划未发货数据
	 * 
	 * @param salesOrderPool
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AjaxResult getSCMData(SalesOrderPool salesOrderPool) {
		// 通过接口获取供应链数据
		AjaxResult result = this.syncItfData(salesOrderPool);
		if (!AppUtils.checkAjaxSuccess(result)) {
			return result;
		}
		List<SyncPlanedNotShipResultVo> syncResultList = (List<SyncPlanedNotShipResultVo>) result
				.get(AjaxResult.DATA_TAG);
		if (CollectionUtils.isEmpty(syncResultList)) {
			return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.noScmData"));
		}
		return this.saveItfData(salesOrderPool, syncResultList);
	}

	@Override
	public List<SalesOrderPool> findCurrentSalesOrderPool() {
		LambdaQueryWrapper<SalesOrderPool> wrapper = Wrappers.lambdaQuery();
		wrapper.eq(SalesOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
		return salesOrderPoolEntityMapper.selectList(wrapper);
	}

	/**
	 * 保存接口数据
	 * 
	 * @param salesOrderPool 同步数据参数
	 * @param syncResultList 同步数据返回结果
	 * @return
	 */
	private AjaxResult saveItfData(SalesOrderPool salesOrderPool, List<SyncPlanedNotShipResultVo> syncResultList) {
		// 根据年月取出年月现有数据
		Integer year = salesOrderPool.getYear();
		Integer month = salesOrderPool.getMonth();
		String factoryCode = salesOrderPool.getFactoryCode();
		LambdaQueryWrapper<SalesOrderPool> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SalesOrderPool::getYear, year);
		queryWrapper.eq(SalesOrderPool::getMonth, month);
		queryWrapper.eq(SalesOrderPool::getFactoryCode, factoryCode);
		queryWrapper.eq(SalesOrderPool::getIsDelete, ApsConstant.APS_YES_NO_0);
		List<SalesOrderPool> salesOrderPoolList = salesOrderPoolEntityMapper.selectList(queryWrapper);
		boolean isFirstCatch = CollectionUtils.isEmpty(salesOrderPoolList); // 如果抓取不到，是年月首次抓取

		// 排产参数
		String salesOrderStockFlag = this.getFactoryParam(factoryCode, ProductTypeEnum.WHOLE_STEEL.getValue(),
				MonthPlanEnums.SALESORDER_STOCK_FLAG); // 从参数取出储备标记
		String hightPriorityOrderRateStr = this.getFactoryParam(factoryCode, ProductTypeEnum.WHOLE_STEEL.getValue(),
				MonthPlanEnums.HIGHT_PRIORITY_ORDER_RATE); // 高优先级订单占比
		BigDecimal hightPriorityOrderRate = BigDecimalUtils
				.percentages2Decimals(BigDecimalUtils.valueOf(hightPriorityOrderRateStr)); // 占比参数转换成小数

		// 把接口数据转换成aps订单池对象
		List<SalesOrderPool> newSalesOrderPoolList = syncResultList.stream().map(vo -> {
			String salPriority = vo.getSalPriority(); // 销售优先级
			String salCodePo = vo.getSalCodePo(); // po号
			String shipType = null; // 发货模式，需要做字典映射
			switch (vo.getShipType()) {
			case ApsConstant.SCM_DELIVERY_MODE_ALL:
				shipType = ApsConstant.DELIVERY_MODE_ALL;
				break;
			default:
				shipType = ApsConstant.DELIVERY_MODE_SPLIT;
			}

			SalesOrderPool newVO = new SalesOrderPool();
			newVO.setFactoryCode(factoryCode);
			newVO.setYear(year);
			newVO.setMonth(month);
			newVO.setArea(String.valueOf(vo.getEmployeeDept()));
			newVO.setBillDate(vo.getBillDate());
			newVO.setBrand(vo.getBrandName());
			newVO.setDeliverGoodsType(shipType);
			newVO.setEudr(vo.getEudrFlag());
			newVO.setMaterialDesc(vo.getSpecDesc());
			newVO.setNatCode(vo.getNatCode());
			newVO.setOrderPriority(salPriority);
			newVO.setOrdQty(vo.getOrdQty());
			newVO.setOriMaterialCode(vo.getOriMaterialCode());
			newVO.setProductType(vo.getProductType());
			newVO.setSalCode(vo.getSalCode());
			newVO.setSalCodePo(salCodePo);
			newVO.setSalNCode(vo.getSalNCode());
			newVO.setWeekYear(vo.getWeekYearRequirement());
			newVO.setScmDetailId(vo.getSaleBillDetailId());
//					newVO.setScmBillId(vo.getSaleBillId());
//					newVO.setDynamicBalance(dynamicBalance);
//					newVO.setUniformity(uniformity);
			String scmPriority = ApsConstant.SAL_PRIORITY_MID;
			if (isFirstCatch) { // 年月首次抓取
				// 订单类型 !=空，且PO号含有储备字样，则供应链优先级 = 中优先级;，否则供应链优先级 = 订单类型
				if (StringUtils.isNotEmpty(salPriority) && StringUtils.isNotEmpty(salesOrderStockFlag)
						&& StringUtils.isNotEmpty(salCodePo) && !salCodePo.contains(salesOrderStockFlag)) {
					scmPriority = salPriority;
				}
			}
			newVO.setScmPriority(scmPriority);
			newVO.setBaseVale(newVO.getId());

			return newVO;
		}).collect(Collectors.toList());

		if (isFirstCatch) { // 年月首次抓取
			// 根据发货模式，当发货模式 = 整单发货的订单，对其下所有SKU获取供应链优先级最高的值(即供应链优先级的值最小)，
			List<String> salCodePoList = newSalesOrderPoolList.stream()
					.filter(s -> ApsConstant.DELIVERY_MODE_ALL.equals(s.getDeliverGoodsType()))
					.map(s -> s.getSalCodePo()).distinct().collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(salCodePoList)) {
				Map<String, String> greatestPriorityMap = newSalesOrderPoolList.stream()
						.filter(s -> salCodePoList.contains(s.getSalCodePo()))
						.collect(Collectors.groupingBy(SalesOrderPool::getSalCodePo,
								Collectors.collectingAndThen(
										Collectors.minBy(Comparator.comparing(SalesOrderPool::getScmPriority)), // 取最高优先级（值最小）
										s -> s.get().getScmPriority())));
				for (Entry<String, String> entry : greatestPriorityMap.entrySet()) {
					String salCodePo = entry.getKey();
					String scmPriority = entry.getValue();
					if (StringUtils.isEmpty(salCodePo)) {
						continue;
					}
					// 将PO下的所有SKU的供应链优先级 = 找到的最高优先级的值(即供应链优先级值最小的值)；
					newSalesOrderPoolList.stream().filter(s -> salCodePo.equals(s.getSalCodePo()))
							.forEach(s -> s.setScmPriority(scmPriority));
				}

				// 按供应链优先级汇总高优先级的订单总量【SUM(数量)，且供应链优先级 = 1】，与所有订单总量【SUM(数量)】。
				// 如果高优先级总量/所有总量> 85%，则对高优先级订单按提报日期升序排序
				// 将提报日期晚(最后)的订单的供应链优先级由高优先级修改为中优先级，直到新的高优先级总量/所有总量 <=85%（第一次）为止
				if (hightPriorityOrderRate.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal totalOrdQty = newSalesOrderPoolList.stream().map(SalesOrderPool::getOrdQty)
							.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add); // 总订单量合计
					List<SalesOrderPool> hightPriorityList = newSalesOrderPoolList.stream()
							.filter(s -> ApsConstant.SAL_PRIORITY_HIGHT.equals(s.getOrderPriority())
									&& ApsConstant.SAL_PRIORITY_HIGHT.equals(s.getScmPriority()))
							.sorted(Comparator.comparing(SalesOrderPool::getBillDate, Comparator.reverseOrder()))
							.collect(Collectors.toList());
					BigDecimal hightPriorityOrdQty = hightPriorityList.stream().map(SalesOrderPool::getOrdQty)
							.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add); // 高优先级订单量合计
					for (SalesOrderPool pool : hightPriorityList) {
						if (BigDecimalUtils.div(hightPriorityOrdQty, totalOrdQty)
								.compareTo(hightPriorityOrderRate) > 0) { // 高优先级总量/所有总量> 85%
							pool.setScmPriority(ApsConstant.SAL_PRIORITY_MID); // 优先级设置未中
							hightPriorityOrdQty = BigDecimalUtils.sub(hightPriorityOrdQty, pool.getOrdQty()); // 高优先级订单量合计扣减掉该订单量
						}
					}
				}
			}
		} else { // 年月再次抓取数据
			Map<Long, List<SalesOrderPool>> scmDetailMap = salesOrderPoolList.stream()
					.filter(s -> s.getScmDetailId() != null)
					.collect(Collectors.groupingBy(SalesOrderPool::getScmDetailId));
			for (SalesOrderPool pool : newSalesOrderPoolList) {
				SalesOrderPool oldPool = CollectionUtils.firstElement(scmDetailMap.get(pool.getScmDetailId()));
				if (oldPool != null) {
					// 如果(SCM行ID)存在，则订单类型 = 旧订单类型，供应链优先级 = 旧供应链优先级；同时，更新订单其他字段数据；
					pool.setOrderPriority(oldPool.getOrderPriority());
					pool.setScmPriority(oldPool.getScmPriority());
				} else {
					// 如果(SCM行ID)不存在，则订单类型=抓取数据的订单类型；供应链优先级 = 中优先级；
					pool.setScmPriority(ApsConstant.SAL_PRIORITY_MID);
				}
			}
		}
		// 删除旧数据
		LambdaUpdateWrapper<SalesOrderPool> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(SalesOrderPool::getYear, year);
		updateWrapper.eq(SalesOrderPool::getMonth, month);
		updateWrapper.eq(SalesOrderPool::getIsDelete, ApsConstant.APS_YES_NO_0);
		updateWrapper.set(SalesOrderPool::getIsDelete, ApsConstant.APS_YES_NO_1);
		updateWrapper.set(SalesOrderPool::getUpdateTime, new Date());
		salesOrderPoolEntityMapper.update(new SalesOrderPool(), updateWrapper);

		salesOrderPoolEntityMapper.batchInsert(newSalesOrderPoolList); // 新增一批数据

		// TODO 调用MES接口获取成品库存
		return AjaxResult.success();
	}

	/**
	 * 获取同步接口数据
	 * 
	 * @param salesOrderPool
	 * @return
	 */
	private AjaxResult syncItfData(SalesOrderPool salesOrderPool) {
		// 调用接口获取供应链数据
		SyncPlanedNotShipParamVo planedNotShipParamVo = new SyncPlanedNotShipParamVo();
		planedNotShipParamVo.setYear(salesOrderPool.getYear());
		planedNotShipParamVo.setMonth(salesOrderPool.getMonth());
		planedNotShipParamVo.setFactory(salesOrderPool.getFactoryCode());
		AjaxResult result = iScmItfService.syncPlanedNotShipList(planedNotShipParamVo);
		if (!AppUtils.checkAjaxSuccess(result)) {
			log.error(String.valueOf(result.get(AjaxResult.MSG_TAG)));
			return result;
		}
		List<SyncPlanedNotShipResultVo> syncResultList = null;
		Object resultData = result.get(AjaxResult.DATA_TAG);
		if (resultData instanceof List) {
			syncResultList = JSONArray.parseArray(JSONArray.toJSONString(resultData), SyncPlanedNotShipResultVo.class);
		}

		return AjaxResult.success(syncResultList);
	}

	/**
	 * 获取配置信息
	 * 
	 * @param paramCode
	 * @return
	 */
	private String getFactoryParam(String factoryCode, String productTypeCode, MonthPlanEnums paramCode) {
		FactoryParam factoryParam = new FactoryParam();
		factoryParam.setFactoryCode(factoryCode);
		factoryParam.setParamCode(paramCode.getCode());
		factoryParam.setProductTypeCode(productTypeCode);
		FactoryParam param = iFactoryParamService.getFacParamSingle(factoryParam);
		String paramValue = null;
		if (param != null) {
			paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue()
					: param.getDefauleValue();
		}
		return paramValue;
	}
}
