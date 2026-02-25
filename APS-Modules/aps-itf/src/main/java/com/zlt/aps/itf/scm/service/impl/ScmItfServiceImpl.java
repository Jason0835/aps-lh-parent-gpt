package com.zlt.aps.itf.scm.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.scm.service.ScmItfService;
import com.zlt.aps.itf.scm.vo.SyncOutFacScheduleVersionVo;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;
//import com.zlt.aps.itf.util.ItfSyncDataHandle;
import com.zlt.aps.itf.util.PostMethodUtils;
import com.zlt.aps.maindata.mapper.DpAreaEntityMapper;
import com.zlt.aps.maindata.mapper.DpNationEntityMapper;
import com.zlt.aps.mp.api.domain.entity.DpArea;
import com.zlt.aps.mp.api.domain.entity.DpNation;
import com.zlt.core.dao.basedao.BaseDao;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ScmItfServiceImpl implements ScmItfService {
//	@Autowired
//	private ItfSyncDataHandle itfSyncDataHandle;

	@Value("${itf.scm.syncPlanedNotShipList.url}")
	private String SYNC_PLANED_NOTSHIP_URL;

	@Value("${itf.scm.lockSalesOrderPool.url}")
	private String SYNC_LOCK_SALES_ORDER_POOL_URL;

	@Value("${itf.scm.syncOutShipDmdOrdList.url}")
	private String SYNC_OUT_SHIP_DMD_ORD_LIST_URL;

	@Value("${itf.scm.syncArea.url}")
	private String SYNC_AREA_URL;

	@Value("${itf.scm.nation.url}")
	private String SYNC_NATION_URL;

	@Value("${itf.scm.publicFacScheduleVersion.url}")
	private String PUBLIC_FAC_SCHEDULE_VERSION_URL;
	
	@Autowired
	private DpAreaEntityMapper dpAreaEntityMapper;
	@Autowired
	private DpNationEntityMapper dpNationEntityMapper;
	@Autowired
	private BaseDao baseDao;

	/**
	 * 同步已计划未发货数据
	 * 
	 * @param planedNotShipParamVo
	 * @return
	 */
	@Override
	public AjaxResult syncPlanedNotShipList(SyncPlanedNotShipParamVo planedNotShipParamVo) {
		AjaxResult ajaxResult = null;
		try {
			if (planedNotShipParamVo == null) {
				ajaxResult = AjaxResult.error("传入参数为空");
				return ajaxResult;
			}
			// 调用供应链接口获取数据
			String result = PostMethodUtils.sendPost(SYNC_PLANED_NOTSHIP_URL,
					JSONObject.toJSONString(planedNotShipParamVo), null);
			// 校验数据格式是否合法
			if (StringUtils.isEmpty(result) || !JSONValidator.from(result).validate()) {
				String errorMsg = "syncPlanedNotShipList 返回数据格式校验失败：" + result;
				log.error(errorMsg);
				ajaxResult = AjaxResult.error(errorMsg);
				return ajaxResult;
			}
			ajaxResult = JSONObject.parseObject(result, AjaxResult.class);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			// 暂时先移除接口日志记录
//			Integer status = AppUtils.checkAjaxSuccess(ajaxResult) ? ApsConstant.SYNC_STATUS_6
//					: ApsConstant.SYNC_STATUS_3;
//			// 记录同步记录
//			SyncParamsVO paramsVO = new SyncParamsVO();
//			paramsVO.setSyncKey(ApsConstant.SYNC_PLANED_NOT_SHIP);
//			paramsVO.setParams(new JSONObject());
//			paramsVO.setFactoryCode(planedNotShipParamVo.getFactory());
//			paramsVO.setCompanyCode(planedNotShipParamVo.getFactory());
//			paramsVO.setStatus(status);
//			paramsVO.setNoMq(ApsConstant.APS_YES_NO_1); // 不发送mq，仅记录日志
//			itfSyncDataHandle.syncRequest(paramsVO); // 记录请求日志
		}
		return ajaxResult;
	}

	/**
	 * 锁定订单池
	 * 
	 * @param planedNotShipParamVo
	 * @return
	 */
	@Override
	public AjaxResult lockSalesOrderPool(SyncPlanedNotShipParamVo planedNotShipParamVo) {
		AjaxResult ajaxResult = null;
		try {
			if (planedNotShipParamVo == null) {
				ajaxResult = AjaxResult.error("传入参数为空");
				return ajaxResult;
			}
			// 调用供应链接口获取数据
			String result = PostMethodUtils.sendPost(SYNC_LOCK_SALES_ORDER_POOL_URL,
					JSONObject.toJSONString(planedNotShipParamVo), null);
			// 校验数据格式是否合法
			if (StringUtils.isEmpty(result) || !JSONValidator.from(result).validate()) {
				String errorMsg = "lockSalesOrderPool 返回数据格式校验失败：" + result;
				log.error(errorMsg);
				ajaxResult = AjaxResult.error(errorMsg);
				return ajaxResult;
			}
			ajaxResult = JSONObject.parseObject(result, AjaxResult.class);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return ajaxResult;
	}

	/**
	 * 同步发货明细数据
	 * 
	 * @param planedNotShipParamVo
	 * @return
	 */
	@Override
	public AjaxResult syncOutShipDmdOrdList(SyncPlanedNotShipParamVo syncOutShipDmdOrdVo) {
		AjaxResult ajaxResult = null;
		try {
			if (syncOutShipDmdOrdVo == null) {
				ajaxResult = AjaxResult.error("传入参数为空");
				return ajaxResult;
			}
			// 调用供应链接口获取数据
			String result = PostMethodUtils.sendPost(SYNC_OUT_SHIP_DMD_ORD_LIST_URL,
					JSONObject.toJSONString(syncOutShipDmdOrdVo), null);
			// 校验数据格式是否合法
			if (StringUtils.isEmpty(result) || !JSONValidator.from(result).validate()) {
				String errorMsg = "syncOutShipDmdOrdList 返回数据格式校验失败：" + result;
				log.error(errorMsg);
				ajaxResult = AjaxResult.error(errorMsg);
				return ajaxResult;
			}
			ajaxResult = JSONObject.parseObject(result, AjaxResult.class);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return ajaxResult;
	}

	/**
	 * 月计划排程结果推送
	 * 
	 * @param planedNotShipParamVo
	 * @return
	 */
	@Override
	public AjaxResult publicFacScheduleVersion(List<SyncOutFacScheduleVersionVo> outFacScheduleVersionList) {
		AjaxResult ajaxResult = null;
		try {
			if (CollectionUtils.isEmpty(outFacScheduleVersionList)) {
				ajaxResult = AjaxResult.error("传入参数为空");
				return ajaxResult;
			}
			// 调用供应链接口获取数据
			String result = PostMethodUtils.sendPost(PUBLIC_FAC_SCHEDULE_VERSION_URL,
					JSONObject.toJSONString(outFacScheduleVersionList), null);
			// 校验数据格式是否合法
			if (StringUtils.isEmpty(result) || !JSONValidator.from(result).validate()) {
				String errorMsg = "publicFacScheduleVersion 返回数据格式校验失败：" + result;
				log.error(errorMsg);
				ajaxResult = AjaxResult.error(errorMsg);
				return ajaxResult;
			}
			ajaxResult = JSONObject.parseObject(result, AjaxResult.class);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return ajaxResult;
	}
	

	/**
	 * 同步区域/国家数据
	 * 
	 * @return
	 */
	@Override
	public AjaxResult syncArea() {
		List<DpArea> areaList = this.getScmAreaData(SYNC_AREA_URL, DpArea.class); // 抓取scm数据
		List<DpNation> nationlist = this.getScmAreaData(SYNC_NATION_URL, DpNation.class); // 抓取scm数据
		try {
			/** 切换APS数据源 start **/
			DynamicDataSourceContextHolder.push(DataSource.APS);
			if (!CollectionUtils.isEmpty(areaList)) {
				Map<String, DpArea> areaMap = areaList.stream().collect(Collectors.toMap(DpArea::getAreaCode, Function.identity(), (a1, a2) -> a1));
				List<DpArea> oldAreaList = dpAreaEntityMapper.selectList(new LambdaQueryWrapper<>());
				for (DpArea area: oldAreaList) {
					DpArea newArea = areaMap.get(area.getAreaCode());
					if (newArea != null) {
						newArea.setId(area.getId());
						newArea.setBaseVale(area.getId());
					}
				}
				baseDao.saveBatch(areaList);
			}
			if (!CollectionUtils.isEmpty(nationlist)) {
				Map<String, DpNation> nationMap = nationlist.stream().collect(Collectors.toMap(DpNation::getNationCode, Function.identity(), (a1, a2) -> a1));
				List<DpNation> oldNationList = dpNationEntityMapper.selectList(new LambdaQueryWrapper<>());
				for (DpNation nation: oldNationList) {
					DpNation newNation = nationMap.get(nation.getNationCode());
					if (newNation != null) {
						newNation.setId(nation.getId());
						newNation.setBaseVale(nation.getId());
					}
				}
				baseDao.saveBatch(nationlist);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			DynamicDataSourceContextHolder.clear();
			/** 切换APS数据源 end **/
		}
		return AjaxResult.success();
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	private <T> List<T> getScmAreaData(String url, Class<T> clazz) {
		// 调用供应链接口获取数据
		String result = PostMethodUtils.sendPost(url, "{}", null);
		// 校验数据格式是否合法
		if (StringUtils.isEmpty(result) || !JSONValidator.from(result).validate()) {
			String errorMsg = "syncArea 返回数据格式校验失败：" + result;
			log.error(errorMsg);
		}
		AjaxResult ajaxResult = JSONObject.parseObject(result, AjaxResult.class);
		return AjaxResultUtils.getList(ajaxResult, clazz);
	}
}
