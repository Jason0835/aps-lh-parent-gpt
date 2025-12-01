package com.zlt.mix.setting.service.impl;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.FhGlueReturnRate;
import com.zlt.mix.setting.mapper.FhGlueReturnRateMapper;
import com.zlt.mix.setting.service.FhGlueReturnRateService;

/**
 * 返回胶日返回率Service业务层处理
 *
 * @author zlt
 * @date 2022-11-28
 */
@Service
public class FhGlueReturnRateServiceImpl extends ServiceImpl<FhGlueReturnRateMapper, FhGlueReturnRate>
		implements FhGlueReturnRateService {
	@Resource
	private FhGlueReturnRateMapper fhGlueReturnRateMapper;

	/**
	 * 查询返回胶日返回率列表
	 *
	 * @param fhGlueReturnRate 返回胶日返回率
	 * @return 返回胶日返回率
	 */
	@Override
	public List<FhGlueReturnRate> selectFhGlueReturnRateList(FhGlueReturnRate fhGlueReturnRate) {
		return fhGlueReturnRateMapper.selectFhGlueReturnRateList(fhGlueReturnRate);
	}

	/**
	 * 保存返回胶日返回率信息（id为空则新增，id不为空则修改）
	 *
	 * @param fhGlueReturnRate
	 */
	@Override
	public void saveFhGlueReturnRate(FhGlueReturnRate fhGlueReturnRate) {
		if (ZltConstant.NOT_UNIQUE.equals(checkFhGlueReturnRateUnique(fhGlueReturnRate))) {
			throw new RuntimeException(I18nUtil.getMessage("setting.fhGlueRate.database.unique"));
		}
		fhGlueReturnRate.setBaseValue(fhGlueReturnRate.getId());
		this.saveOrUpdate(fhGlueReturnRate);
	}

	/**
	 * 批量删除返回胶日返回率
	 *
	 * @param ids 需要删除的返回胶日返回率ID
	 * @return 结果
	 */
	@Override
	public int deleteFhGlueReturnRateByIds(Long[] ids) {
		return fhGlueReturnRateMapper.deleteFhGlueReturnRateByIds(ids);
	}

	/**
	 * 校验返回胶日返回率唯一性
	 */
	@Override
	public String checkFhGlueReturnRateUnique(FhGlueReturnRate fhGlueReturnRate) {
		if (fhGlueReturnRate == null) {
			return ZltConstant.NOT_UNIQUE;
		}

		QueryWrapper<FhGlueReturnRate> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
		queryWrapper.eq("MIX_AREA", fhGlueReturnRate.getMixArea());
		queryWrapper.eq("GLUE", fhGlueReturnRate.getGlue());
		if (fhGlueReturnRate.getId() != null) {
			queryWrapper.ne("ID", fhGlueReturnRate.getId()); // 编辑的时候校验，要过滤掉自身的id
		}

		List<FhGlueReturnRate> list = fhGlueReturnRateMapper.selectList(queryWrapper);
		if (list.size() > 0) {
			return ZltConstant.NOT_UNIQUE;
		}
		return ZltConstant.UNIQUE;
	}

	/**
	 * 导入返回胶日返回率数据
	 *
	 * @param list          要导入的数据集合
	 * @param updateSupport 已存在记录是否更新
	 * @param importLogId   导入日志id
	 */
	@Override
	public AjaxResult importData(List<FhGlueReturnRate> list, boolean updateSupport, Long importLogId) {
		// 初始化
		int successNum = 0;
		int failureNum = 0;
		List<FhGlueReturnRate> importList = new ArrayList<>(); // 各种校验通过后的导入数据列表（最终可以导入数据库的计划）
		List<ImportErrorLog> importErrorLogs = new ArrayList<>(); // 导入错误明显列表
		List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>(); // 违反数据库唯一键的错误列表
		Map<Integer, Long> codeUniqueErrorMap = new HashMap<>(); // 用来存储哪一行数据违反了数据库唯一键

		try {
			if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
				// 没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
				codeUniqueErrorLogs = this.fhGlueReturnRateMapper.listFhGlueReturnRateNotUnique(list, importLogId,
						I18nUtil.getMessage("setting.fhGlueRate.database.unique"), SecurityUtils.getUsername());
				importErrorLogs.addAll(codeUniqueErrorLogs);
				codeUniqueErrorMap = codeUniqueErrorLogs.stream()
						.collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
			}

			// 按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
			Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(
					a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getGlue()), Collectors.counting()));

			// 公共校验（非空校验、长度校验等）
			for (int i = 0; i < list.size(); i++) {
				FhGlueReturnRate fhGlueReturnRate = list.get(i);
				// exce中重复记录校验
				Long hasValue = groupMap.get(
						GenerageMapKeyUtils.createMapKey(fhGlueReturnRate.getMixArea(), fhGlueReturnRate.getGlue()));
				if (hasValue > 1) {
					// 导入的excel中的数据违反了唯一键约束
					fhGlueReturnRate.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
					String message = I18nUtil.getMessage("setting.fhGlueRate.excel.unique");
					addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
				}

				// 违反数据库唯一键的记录
				if (codeUniqueErrorMap.containsKey(i + 2)) {
					// 数据已经系统中存在
					fhGlueReturnRate.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
				}

				List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, fhGlueReturnRate); // 校验excel每个单元格长度、类型等

				if (CollectionUtils.isEmpty(validated) && fhGlueReturnRate.getId() == null) {
					fhGlueReturnRate.setBaseValue(null);
					importList.add(fhGlueReturnRate);
				} else {
					fhGlueReturnRate.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
					importErrorLogs.addAll(validated);
				}
			}

			// 勾选更新记录，调用merge即可
			if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
				fhGlueReturnRateMapper.mergeSql(importList); // 根据唯一键批量新增或修改
			} else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
				fhGlueReturnRateMapper.batchInsertFhGlueReturnRateInfo(importList); // 批量插入
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
