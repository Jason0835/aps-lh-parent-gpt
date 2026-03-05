package com.zlt.aps.itf.mes.service.impl;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.mapper.MesBomItfMapper;
import com.zlt.aps.itf.mes.service.MesBomItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.maindata.mapper.MdmBomInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmConstructionInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.mp.api.domain.entity.MdmBomInfo;
import com.zlt.aps.mp.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.core.dao.basedao.BaseDao;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
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
	private MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;
	@Autowired
	private MdmSkuStructureRefEntityMapper mdmSkuStructureRefEntityMapper;
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
				// 更新胎胚描述到物料信息
				MdmSkuConstructionRef queryVO = new MdmSkuConstructionRef();
				queryVO.setBaseVale(null);
				mdmSkuConstructionRefEntityMapper.updateMainMaterialDescToMaterialInfo(queryVO);
				// 新增不存在的胎胚描述到 胎胚描述与结构关系表
				mdmSkuStructureRefEntityMapper.insertMainMaterialDesc4SkuConstructionRef(queryVO);
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
	 * 获取分组key（半部件施工表）
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
//	    MdmBomInfo mdm = new MdmBomInfo();mdm.setId(-1L);
//	    List<MdmBomInfo> syncList = Collections.singletonList(mdm);
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

                // 构建胎胚原料消耗量
                List<MdmMaterialConsumeDetail> detaiList = this.buildConsumeDetailLIst(syncList, apsDataList, syncDataLogs.getFactoryCode());
                if (CollectionUtils.isNotEmpty(detaiList)) {
//                    // 保存前先删除本次同步涉及的胎胚原材料明细
//                    List<String> embryoCodeList = detaiList.stream().map(MdmMaterialConsumeDetail::getEmbryoCode).distinct().collect(Collectors.toList());
//                    List<List<String>> splitDeleteList = ScmListUtils.getSplitList(embryoCodeList, 1000);
//                    for (List<String> deleteList : splitDeleteList) { // 分批处理，防止长度超出限制
//                        Map<String, Object> paramMap = new HashMap<>();
//                        paramMap.put("embryoCode", deleteList);
//                        baseDao.deleteByMap(MdmMaterialConsumeDetail.class, paramMap);
//                    }
                    List<List<MdmMaterialConsumeDetail>> splitDetailList = ScmListUtils.getSplitList(detaiList, 1000);
                    for (List<MdmMaterialConsumeDetail> saveList : splitDetailList) { // 分批处理，防止长度超出限制
                        baseDao.saveBatch(saveList);
                    }
                }
			} finally {
				DynamicDataSourceContextHolder.clear();
				/** 切换APS数据源 end **/
			}
		}
		return AjaxResult.success();
	}

    /**
     * 构建胎胚原料消耗量
     *
     * @param mesDateList 接口同步的bom数据
     * @param apsDataList aps库现有的bom数据
     * @param factoryCode 厂别
     * @return
     */
    private List<MdmMaterialConsumeDetail> buildConsumeDetailLIst(List<MdmBomInfo> mesDateList,
                                                                  List<MdmBomInfo> apsDataList,
                                                                  String factoryCode) {
        // 1、合并MES和aps的bom记录
        Set<Long> updateIdSet = mesDateList.stream().map(MdmBomInfo::getId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<MdmBomInfo> bomList = apsDataList.stream().filter(bom -> !updateIdSet.contains(bom.getId()))
                .collect(Collectors.toList());
        bomList.addAll(mesDateList);

        // 2、加载已有原料消耗表
        LambdaQueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmMaterialConsumeDetail::getIsDelete, ApsConstant.APS_YES_NO_0);
        queryWrapper.eq(MdmMaterialConsumeDetail::getFactoryCode, factoryCode);
        List<MdmMaterialConsumeDetail> oldDetailList = mdmMaterialConsumeDetailMapper.selectList(queryWrapper);
        Map<String, MdmMaterialConsumeDetail> oldDetailMap = oldDetailList.stream().collect(Collectors.toMap(detail -> this.getMapKey(detail), Function.identity(), (d1, d2) -> d1));

        // 3、根据父节汇总
        Map<String, List<MdmBomInfo>> parentMap = bomList.stream()
                .collect(Collectors.groupingBy(bom -> this.getBomParentMapKey(bom)));
        // 4、构建bom树
        for (MdmBomInfo bom: bomList) {
            String childKey = this.getBomChildMapKey(bom);
            List<MdmBomInfo> children = parentMap.get(childKey);
            boolean isLeaf = CollectionUtils.isEmpty(children); // 如果没有子节点，判定为叶子节点
            bom.setIsLeaf(isLeaf);
            if (!isLeaf) { // 非叶子节点，关联父子节点的关系
                bom.setChildren(children);
                children.forEach(child -> child.setParent(bom));
            }
        };
        // 5、构建物料消耗清单
        List<MdmMaterialConsumeDetail> detaiList = new ArrayList<>();
        LinkedList<MdmBomInfo> pathList = new LinkedList<>();
        List<MdmBomInfo> leafList = bomList.stream()
                .filter(bom -> bom.getIsLeaf() && StringUtils.isNotEmpty(bom.getChildMaterialCode()))
                .collect(Collectors.toList());
        for (MdmBomInfo bom: leafList) {
            // 5.1、构建bom树路径，从叶子节点开始向上
            pathList.clear();
            MdmBomInfo currentNode = bom; // 当前节点
            if (StringUtils.isEmpty(bom.getChildMaterialCode())) {
                continue;
            }
            do {
                if (currentNode == null || pathList.contains(currentNode)) { // 编号为空或者死循环了，结束
                    break;
                }
                pathList.addLast(currentNode); // 倒序添加节点
                currentNode = currentNode.getParent(); // 切换成父节点
            } while (true);
            
            if (pathList.size() <= 1) {
                continue; // 层架不到1层的，说明Bom数据有问题，跳过
            }

            // 5.2、判断如果路径上有任意一个节点是本次更新的bom，则需要新生成一笔消耗明细，
            if (pathList.stream().anyMatch(b -> updateIdSet.contains(b.getId()))) {
                // 5.2.1、取胎胚，必然是路径的最后一个元素
                MdmBomInfo embryoBom = pathList.getLast();
                if (StringUtils.isEmpty(embryoBom.getParentCode())) { // 胎胚号为空说明有问题，跳过
                    continue;
                }
                // 5.2.2、初始化消耗量
                MdmMaterialConsumeDetail consumeDetail = new MdmMaterialConsumeDetail();
                consumeDetail.setFactoryCode(factoryCode);
                consumeDetail.setChildMaterialCode(bom.getChildMaterialCode());
                consumeDetail.setChildMaterialName(bom.getChildMaterialName());
                consumeDetail.setChildMaterialVersion(bom.getChildMaterialVersion());
                consumeDetail.setUnit(bom.getUnit());
                consumeDetail.setEmbryoCode(embryoBom.getParentCode()); // 20260302，由于胎胚在bom里没有单独的记录，需要关联出最上级的物料后
                consumeDetail.setEmbryoVersion(embryoBom.getParentVersion());
                // 5.2.3、计算用量，用量为每一层bom的用量乘数
                BigDecimal dosage = pathList.stream().map(node -> BigDecimalUtils.valueOf(node.getDosage()))
                        .reduce(BigDecimal.ONE, BigDecimal::multiply);
                consumeDetail.setDosage(dosage);
                
                MdmMaterialConsumeDetail oldDetail = oldDetailMap.get(this.getMapKey(consumeDetail));
                if (oldDetail != null) {
                    consumeDetail.setId(oldDetail.getId());
                }
                detaiList.add(consumeDetail);
            }
        }
        if (CollectionUtils.isEmpty(detaiList)) {
            return detaiList;
        }
        
        // 按胎胚号 + 物料号去重
        Map<String, MdmMaterialConsumeDetail> distinctDetailList = detaiList.stream()
                .collect(Collectors.groupingBy(detail -> this.getMapKey(detail),
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.get(0))));        
        return new ArrayList<>(distinctDetailList.values());
    }

	/**
	 * 获取分组key（BOM表）
	 *
	 * @param info
	 * @return
	 */
	private String getMapKey(MdmBomInfo info) {
		return GenerageMapKeyUtils.createMapKey(info.getFactoryCode(), info.getParentCode(),
				info.getParentVersion(), info.getChildCode(), info.getChildMaterialVersion());
	}

    /**
     * 获取父节点分组key（BOM表）
     *
     * @param info
     * @return
     */
    private String getBomParentMapKey(MdmBomInfo info) {
        return GenerageMapKeyUtils.createMapKey(info.getFactoryCode(), info.getParentCode(), info.getParentVersion());
    }

    /**
     * 获取子节点分组key（BOM表）
     *
     * @param info
     * @return
     */
    private String getBomChildMapKey(MdmBomInfo info) {
        return GenerageMapKeyUtils.createMapKey(info.getFactoryCode(), info.getChildCode(), info.getChildMaterialVersion());
    }

    /**
     * 获取分组key（消耗表）
     *
     * @param info
     * @return
     */
    private String getMapKey(MdmMaterialConsumeDetail info) {
        return GenerageMapKeyUtils.createMapKey(info.getFactoryCode(), info.getEmbryoCode(), info.getChildMaterialCode());
    }
}
