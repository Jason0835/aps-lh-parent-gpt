package com.zlt.aps.common.engine.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.util.StringUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.mapper.ConstructionParseMapper;
import com.zlt.aps.common.engine.service.ProductConstructionService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 投产施工服务
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-12-4 14:20:34
 */
@Service("productConstructionService")
@Slf4j
public class ProductConstructionServiceImpl implements ProductConstructionService {
	@Autowired
	ConstructionParseMapper constructionParseMapper;
	@Autowired
	private CxEngineQuotaCommonService cxEngineQuotaCommonService;
	
	/**
	 * 根据月度计划初始化施工信息<br/>
	 * 月度计划涉及胎胚所有版本均同步至投产施工表（已有的版本不同步）<br/>
	 * 并将只有一个版本的胎胚的版本号赋值到月度计划中
	 * 
	 * @param prodList 月度计划
	 */
	@Override
	public void initBomDataVersionByPlan(List<MdmMonthProdPlan> prodList) {
		if (CollectionUtil.isEmpty(prodList)) {
			return;
		}

		// 判断月计划中的bom版本是否有效，如果无效需要清空掉（有效保留）
		this.judgeBomVersionStatus(prodList);

		// 取出无版本的月计划
		List<MdmMonthProdPlan> noVersionList = prodList.stream().filter(m -> StringUtils.isEmpty(m.getBomDataVersion()))
				.collect(Collectors.toList());
		// 如果都有有效版本，则不需要继续处理
		if (noVersionList.isEmpty()) {
			return;
		}
		// 初始化参数列表中所有胎胚的投产施工记录（含有已有有效的月度计划施工版本）
		List<String> allEmbryoCodeList = prodList.stream().map(MdmMonthProdPlan::getEmbryoCode).distinct()
				.collect(Collectors.toList());
		constructionParseMapper.initProductConstructionInfo(allEmbryoCodeList);

		// 取出单版本投产施工表记录
		List<String> embryoCodeList = noVersionList.stream().map(MdmMonthProdPlan::getEmbryoCode).distinct()
				.collect(Collectors.toList());
		List<CxProductConstructionInfo> constructionList = constructionParseMapper
				.selectSingleVersionConstruction(embryoCodeList);

		// 是否存在无版本的标识（如果但版本查找不到信息，说明都是无版本）
		boolean hasNoneVersion = CollectionUtils.isEmpty(constructionList);
		// 转换为胎胚号——版本映射map
		Map<String, String> versionMap = constructionList.stream().collect(Collectors
				.toMap(CxProductConstructionInfo::getEmbryoCode, CxProductConstructionInfo::getEmbryoVersion));
		log.info("========投产施工初始化：施工版本映射==============" + JSONObject.toJSONString(versionMap));
		for (MdmMonthProdPlan plan : noVersionList) {
			// 从映射表取出版本赋值给月度明细
			String bomDataVersion = versionMap.get(plan.getEmbryoCode());
			if (StringUtil.isNotEmpty(bomDataVersion)) {
				plan.setBomDataVersion(bomDataVersion);
			} else {
				hasNoneVersion = true;
			}
		}
		// 如果仍然存在无版本记录，则需要从最近一次版本中自动带入
		if (hasNoneVersion) {
			// 最近一次选择的版本信息
			List<MdmMonthProdPlan> latestProdList = constructionParseMapper.selectLatestProdPlanBomDataVersionList();
			Map<String, String> bomVersionMap = latestProdList.stream()
					.collect(Collectors.toMap(m -> this.createProdPlanKey(m), MdmMonthProdPlan::getBomDataVersion));
			for (MdmMonthProdPlan plan : noVersionList) {
				// 给无版本的月度计划赋值
				if (StringUtil.isEmpty(plan.getBomDataVersion())) {
					String bomDataVersion = bomVersionMap.get(this.createProdPlanKey(plan));
					plan.setBomDataVersion(bomDataVersion);
				}
			}
		}
		log.info("========投产施工初始化：月度计划版本处理结果==============" + JSONObject.toJSONString(noVersionList));
		// 清除施工表缓存
		cxEngineQuotaCommonService.delCacheConstructionInfoMap();
	}

	/**
	 * 判断月计划中的bom版本是否有效，如果无效需要清空掉，如果有效则保留
	 * 
	 * @param prodList 月度计划
	 */
	private void judgeBomVersionStatus(List<MdmMonthProdPlan> prodList) {
		// 先判断参数是否已经有版本
		List<MdmMonthProdPlan> hasVersionList = prodList.stream()
				.filter(m -> StringUtils.isNotEmpty(m.getBomDataVersion())).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(hasVersionList)) {
			return;
		}
		// 施工版本状态列表
		Map<Object, CxProductConstructionInfo> constructionFlagMap = constructionParseMapper
				.listEmbryoVersion(hasVersionList).stream()
				// 根据胎胚 + 版本分组
				.collect(
						Collectors.toMap(p -> GenerageMapKeyUtils.createMapKey(p.getEmbryoCode(), p.getEmbryoVersion()),
								Function.identity(), (v1, v2) -> v2));
		for (MdmMonthProdPlan prod : hasVersionList) {
			// 通过胎胚号于版本号获取到施工信息
			CxProductConstructionInfo construction = constructionFlagMap
					.get(GenerageMapKeyUtils.createMapKey(prod.getEmbryoCode(), prod.getBomDataVersion()));
			// 如果施工信息获取不到有效记录，则清空版本，后续处理再补上最新的有效版本
			if (construction == null) {
				prod.setBomDataVersion(null);
			}
		}
	}

	/**
	 * 构建月度计划明细唯一键，格式：胎胚编号&&外胎编号&&库存地点编号
	 * 
	 * @param prodPlan 月度计划
	 * @return
	 */
	private String createProdPlanKey(MdmMonthProdPlan prodPlan) {
		return StringUtils.join(
				new String[] { prodPlan.getEmbryoCode(), prodPlan.getMaterialCode(), prodPlan.getStorageLocation() },
				"&&");
	}
}
