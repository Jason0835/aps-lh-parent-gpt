package com.zlt.aps.itf.mes.service.impl;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.mapper.MesBomItfMapper;
import com.zlt.aps.itf.mes.service.MesBomItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.maindata.mapper.MdmBomInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmConstructionInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmBomInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.core.dao.basedao.BaseDao;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MES接口-Bom相关接口
 *
 * @author zlt
 * @since 2025/12/19
 */
@Service("mesBomItfService")
public class MesBomItfServiceImpl implements MesBomItfService {
	@Autowired
	private MesBomItfMapper mesBomItfMapper;
	@Autowired
	private MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;
	@Autowired
	private MdmConstructionInfoEntityMapper mdmConstructionInfoEntityMapper;
	@Autowired
	private MdmBomInfoEntityMapper mdmBomInfoEntityMapper;
	@Autowired
	private BaseDao baseDao;

	/**
	 * 同步产月度计划及硫化施工信息同步接口（SKU与施工关系表）
	 *
	 * @param syncDataLogs 同步参数
	 * @return 结果
	 */
	@Override
	public AjaxResult syncLhConstructionInfo(AuxReqSyncDataLogs syncDataLogs) {
		List<MdmSkuConstructionRef> syncList = mesBomItfMapper.selectLhConstructionInfo(syncDataLogs);
		if (CollectionUtils.isNotEmpty(syncList)) {
			LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(MdmSkuConstructionRef::getIsDelete, ApsConstant.APS_YES_NO_0);
			queryWrapper.eq(MdmSkuConstructionRef::getFactoryCode, syncDataLogs.getFactoryCode());
			try {
				/** 切换APS数据源 start **/
				DynamicDataSourceContextHolder.push(DataSource.APS);
				List<MdmSkuConstructionRef> apsDataList = mdmSkuConstructionRefEntityMapper.selectList(queryWrapper); // 取出APS数据
				if (CollectionUtils.isNotEmpty(apsDataList)) {
					Map<String, List<MdmSkuConstructionRef>> refMap = syncList.stream()
							.collect(Collectors.groupingBy(item -> this.getMapKey(item))); // 按业务主键分组
					apsDataList.stream().filter(r -> refMap.containsKey(this.getMapKey(r))).forEach(item -> {
						item.setBaseVale(null);
						List<MdmSkuConstructionRef> updateList = refMap.get(this.getMapKey(item));
						for (MdmSkuConstructionRef updateItem : updateList) {
							updateItem.setId(item.getId());
							updateItem.setBaseVale(item.getId());
						}
					});
				}
				List<List<MdmSkuConstructionRef>> splitList = ScmListUtils.getSplitList(syncList, 1000);
				for (List<MdmSkuConstructionRef> saveList : splitList) { // 分批保存，防止长度超出限制
					baseDao.saveBatch(saveList);
				}
			} finally {
				DynamicDataSourceContextHolder.clear();
				/** 切换APS数据源 end **/
			}
		}
		return AjaxResult.success();
	}

	/**
	 * 获取分组key（SKU与施工关系表）
	 *
	 * @param info
	 * @return
	 */
	private String getMapKey(MdmSkuConstructionRef info) {
		return GenerageMapKeyUtils.createMapKey(info.getFactoryCode(),
//				info.getMesMaterialCode(),
				info.getMaterialCode(),
				info.getTrialStatus()
//				info.getBomVersion(), info.getEmbryoCode()
		);
	}

	/**
	 * 半部件BOM接口
	 *
	 * @param syncDataLogs 同步参数
	 * @return 结果
	 */
	@Override
	public AjaxResult syncConstructionInfo(AuxReqSyncDataLogs syncDataLogs) {
		List<MdmConstructionInfo> syncList = mesBomItfMapper.selectMesConstructionInfo(syncDataLogs);
		if (CollectionUtils.isNotEmpty(syncList)) {
			LambdaQueryWrapper<MdmConstructionInfo> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(MdmConstructionInfo::getIsDelete, ApsConstant.APS_YES_NO_0);
			queryWrapper.eq(MdmConstructionInfo::getFactoryCode, syncDataLogs.getFactoryCode());
			try {
				/** 切换APS数据源 start **/
				DynamicDataSourceContextHolder.push(DataSource.APS);
				List<MdmConstructionInfo> apsDataList = mdmConstructionInfoEntityMapper.selectList(queryWrapper); // 取出APS数据
				if (CollectionUtils.isNotEmpty(apsDataList)) {
					Map<String, List<MdmConstructionInfo>> refMap = syncList.stream()
							.collect(Collectors.groupingBy(item -> this.getMapKey(item))); // 按业务主键分组
					apsDataList.stream().filter(r -> refMap.containsKey(this.getMapKey(r))).forEach(item -> {
						List<MdmConstructionInfo> updateList = refMap.get(this.getMapKey(item));
						for (MdmConstructionInfo updateItem : updateList) {
							updateItem.setId(item.getId());
							updateItem.setBaseVale(item.getId());
						}
					});
				}
				List<List<MdmConstructionInfo>> splitList = ScmListUtils.getSplitList(syncList, 1000);
				for (List<MdmConstructionInfo> saveList : splitList) { // 分批保存，防止长度超出限制
					baseDao.saveBatch(saveList);
				}
			} finally {
				DynamicDataSourceContextHolder.clear();
				/** 切换APS数据源 end **/
			}
		}
		return AjaxResult.success();
	}

	/**
	 * 获取分组key（SKU与施工关系表）
	 *
	 * @param info
	 * @return
	 */
	private String getMapKey(MdmConstructionInfo info) {
		return GenerageMapKeyUtils.createMapKey(info.getFactoryCode(), info.getMesMaterialCode(),
				info.getMaterialCode(), info.getConstructionVersion());
	}

	/**
	 * 成型及半部件BOM施工信息同步
	 *
	 * @param syncDataLogs 同步参数
	 * @return 结果
	 */
	@Override
	public AjaxResult syncBomInfo(AuxReqSyncDataLogs syncDataLogs) {
		List<MdmBomInfo> syncList = mesBomItfMapper.selectMesBomInfo(syncDataLogs);
		if (CollectionUtils.isNotEmpty(syncList)) {
			LambdaQueryWrapper<MdmBomInfo> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(MdmBomInfo::getIsDelete, ApsConstant.APS_YES_NO_0);
			queryWrapper.eq(MdmBomInfo::getFactoryCode, syncDataLogs.getFactoryCode());
			try {
				/** 切换APS数据源 start **/
				DynamicDataSourceContextHolder.push(DataSource.APS);
				List<MdmBomInfo> apsDataList = mdmBomInfoEntityMapper.selectList(queryWrapper); // 取出APS数据
				if (CollectionUtils.isNotEmpty(apsDataList)) {
					Map<String, List<MdmBomInfo>> refMap = syncList.stream()
							.collect(Collectors.groupingBy(item -> this.getMapKey(item))); // 按业务主键分组
					apsDataList.stream().filter(r -> refMap.containsKey(this.getMapKey(r))).forEach(item -> {
						List<MdmBomInfo> updateList = refMap.get(this.getMapKey(item));
						for (MdmBomInfo updateItem : updateList) {
							updateItem.setId(item.getId());
							updateItem.setBaseVale(item.getId());
						}
					});
				}
				List<List<MdmBomInfo>> splitList = ScmListUtils.getSplitList(syncList, 1000);
				for (List<MdmBomInfo> saveList : splitList) { // 分批保存，防止长度超出限制
					baseDao.saveBatch(saveList);
				}
			} finally {
				DynamicDataSourceContextHolder.clear();
				/** 切换APS数据源 end **/
			}
		}
		return AjaxResult.success();
	}

	/**
	 * 获取分组key（SKU与施工关系表）
	 *
	 * @param info
	 * @return
	 */
	private String getMapKey(MdmBomInfo info) {
		return GenerageMapKeyUtils.createMapKey(info.getFactoryCode(), info.getParentMaterialCode(),
				info.getParentVersion(), info.getChildMaterialCode(), info.getChildMaterialVersion());
	}

}
