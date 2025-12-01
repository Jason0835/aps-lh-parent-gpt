package com.zlt.mix.setting.service.impl;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.FactoryGlueAreaRelation;
import com.zlt.mix.setting.mapper.FactoryGlueAreaRelationMapper;
import com.zlt.mix.setting.service.FactoryGlueAreaRelationService;

/**
 * 分厂胶料与密炼区对应关系Service业务层处理
 * 
 * @author zlt
 * @date 2022-11-22
 */
@Service
public class FactoryGlueAreaRelationServiceImpl extends
		ServiceImpl<FactoryGlueAreaRelationMapper, FactoryGlueAreaRelation> implements FactoryGlueAreaRelationService {
	@Autowired
	private FactoryGlueAreaRelationMapper factoryGlueAreaRelationMapper;

	/**
	 * 查询分厂胶料与密炼区对应关系
	 * 
	 * @param id 分厂胶料与密炼区对应关系ID
	 * @return 分厂胶料与密炼区对应关系
	 */
	@Override
	public FactoryGlueAreaRelation selectFactoryGlueAreaRelationById(Long id) {
		return factoryGlueAreaRelationMapper.selectFactoryGlueAreaRelationById(id);
	}

	/**
	 * 查询分厂胶料与密炼区对应关系列表
	 * 
	 * @param tFactoryGlueAreaRelation 分厂胶料与密炼区对应关系
	 * @return 分厂胶料与密炼区对应关系
	 */
	@Override
	public List<FactoryGlueAreaRelation> selectFactoryGlueAreaRelationList(
			FactoryGlueAreaRelation tFactoryGlueAreaRelation) {
		return factoryGlueAreaRelationMapper.selectFactoryGlueAreaRelationList(tFactoryGlueAreaRelation);
	}

	/**
	 * 新增分厂胶料与密炼区对应关系
	 * 
	 * @param tFactoryGlueAreaRelation 分厂胶料与密炼区对应关系
	 * @return 结果
	 */
	@Override
	public int insertFactoryGlueAreaRelation(FactoryGlueAreaRelation tFactoryGlueAreaRelation) {
		tFactoryGlueAreaRelation.setBaseValue(null);
		return factoryGlueAreaRelationMapper.insertFactoryGlueAreaRelation(tFactoryGlueAreaRelation);
	}

	/**
	 * 修改分厂胶料与密炼区对应关系
	 * 
	 * @param tFactoryGlueAreaRelation 分厂胶料与密炼区对应关系
	 * @return 结果
	 */
	@Override
	public int updateFactoryGlueAreaRelation(FactoryGlueAreaRelation tFactoryGlueAreaRelation) {
		tFactoryGlueAreaRelation.setBaseValue(tFactoryGlueAreaRelation.getId());
		return factoryGlueAreaRelationMapper.updateFactoryGlueAreaRelation(tFactoryGlueAreaRelation);
	}

	/**
	 * 批量删除分厂胶料与密炼区对应关系
	 * 
	 * @param ids 需要删除的分厂胶料与密炼区对应关系ID
	 * @return 结果
	 */
	@Override
	public int deleteFactoryGlueAreaRelationByIds(Long[] ids) {
		return factoryGlueAreaRelationMapper.deleteFactoryGlueAreaRelationByIds(ids);
	}

	/**
	 * 删除分厂胶料与密炼区对应关系信息
	 * 
	 * @param id 分厂胶料与密炼区对应关系ID
	 * @return 结果
	 */
	@Override
	public int deleteFactoryGlueAreaRelationById(Long id) {
		return factoryGlueAreaRelationMapper.deleteFactoryGlueAreaRelationById(id);
	}

	/**
	 * 校验分厂胶料与密炼区对应关系唯一性
	 */
	@Override
	public String checkFactoryGlueAreaRelationUnique(FactoryGlueAreaRelation tFactoryGlueAreaRelation) {
		if (tFactoryGlueAreaRelation == null) {
			return UserConstants.NOT_UNIQUE;
		}
		tFactoryGlueAreaRelation.setMixArea(null);// 唯一校验只有分厂+胶料，密炼区不影响唯一性
		List<FactoryGlueAreaRelation> list = factoryGlueAreaRelationMapper
				.selectFactoryGlueAreaRelationList(tFactoryGlueAreaRelation);

		if (CollectionUtils.isEmpty(list)) {
			return UserConstants.UNIQUE;
		}
		// 如果数据库存在，且ID也匹配，则可视作唯一
		if (tFactoryGlueAreaRelation.getId() != null
				&& list.stream().anyMatch(f -> f.getId().equals(tFactoryGlueAreaRelation.getId()))) {
			return UserConstants.UNIQUE;
		}
		return UserConstants.NOT_UNIQUE;
	}

	/**
	 * 导入分厂胶料与密炼区对应关系数据
	 *
	 * @param list          要导入的数据集合
	 * @param updateSupport 已存在记录是否更新
	 * @param importLogId   导入日志id
	 */
	@Override
	public AjaxResult importData(List<FactoryGlueAreaRelation> list, boolean updateSupport, Long importLogId) {
		// 初始化
		int successNum = 0;
		int failureNum = 0;
		List<FactoryGlueAreaRelation> importList = new ArrayList<>(); // 各种校验通过后的导入数据列表（最终可以导入数据库的计划）
		List<ImportErrorLog> importErrorLogs = new ArrayList<>(); // 导入错误明显列表
		List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>(); // 违反数据库唯一键的错误列表
		Map<Integer, Long> codeUniqueErrorMap = new HashMap<>(); // 用来存储哪一行数据违反了数据库唯一键

		try {
			if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
				// 没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
				codeUniqueErrorLogs = this.factoryGlueAreaRelationMapper.listFactoryGlueAreaRelationNotUnique(list,
						importLogId, I18nUtil.getMessage("setting.factoryGlueAreaRelation.database.unique"),
						SecurityUtils.getUsername());
				importErrorLogs.addAll(codeUniqueErrorLogs);
				codeUniqueErrorMap = codeUniqueErrorLogs.stream()
						.collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
			}

			// 按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
			Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(
					a -> GenerageMapKeyUtils.createMapKey(a.getFactory(), a.getGlue()), Collectors.counting()));

			// 公共校验（非空校验、长度校验等）
			for (int i = 0; i < list.size(); i++) {
				FactoryGlueAreaRelation factoryGlueAreaRelation = list.get(i);
				// exce中重复记录校验
				Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(factoryGlueAreaRelation.getFactory(),
						factoryGlueAreaRelation.getGlue()));
				if (hasValue > 1) {
					// 导入的excel中的数据违反了唯一键约束
					factoryGlueAreaRelation.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
					String message = I18nUtil.getMessage("setting.factoryGlueAreaRelation.excel.unique");
					addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
				}

				// 违反数据库唯一键的记录
				if (codeUniqueErrorMap.containsKey(i + 2)) {
					// 数据已经系统中存在
					factoryGlueAreaRelation.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
				}

				List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, factoryGlueAreaRelation); // 校验excel每个单元格长度、类型等

				if (CollectionUtils.isEmpty(validated) && factoryGlueAreaRelation.getId() == null) {
					factoryGlueAreaRelation.setBaseValue(null);
					importList.add(factoryGlueAreaRelation);
				} else {
					factoryGlueAreaRelation.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
					importErrorLogs.addAll(validated);
				}
			}

			// 勾选更新记录，调用merge即可
			if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
				factoryGlueAreaRelationMapper.mergeSql(importList); // 根据唯一键批量新增或修改
			} else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
				factoryGlueAreaRelationMapper.batchInsertFactoryGlueAreaRelationInfo(importList); // 批量插入
			}
		} catch (Exception e) {
			log.error("导入出错", e);
			// 执行sql失败，插入导入失败记录
			failureNum = list.size();
			importErrorLogs.clear();
			addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
			return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
					importErrorLogs);
		}

		successNum = importList.size(); // 成功记录数
		failureNum = list.size() - successNum; // 失败记录数
		if (failureNum > 0) {
			return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
					importErrorLogs);
		} else {
			return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
		}
	}
}
