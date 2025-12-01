package com.zlt.mix.setting.service.impl;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.GlueCommonDemand;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.mapper.GlueCommonDemandMapper;
import com.zlt.mix.setting.mapper.MesBasMaterialMapper;
import com.zlt.mix.setting.service.GlueCommonDemandService;
import com.zlt.mix.setting.service.MixMachineService;

/**
 * 密炼机常用大规格设置Service业务层处理
 *
 * @author zlt
 * @date 2023-02-05
 */
@Service
public class GlueCommonDemandServiceImpl extends ServiceImpl<GlueCommonDemandMapper, GlueCommonDemand>
		implements GlueCommonDemandService {
	@Resource
	private GlueCommonDemandMapper glueCommonDemandMapper;
	@Resource
	private MixMachineService mixMachineService;
	@Resource
	private MesBasMaterialMapper mesBasMaterialMapper;

	/**
	 * 查询密炼机常用大规格设置列表
	 *
	 * @param glueCommonDemand 密炼机常用大规格设置
	 * @return 密炼机常用大规格设置
	 */
	@Override
	public List<GlueCommonDemand> selectGlueCommonDemandList(GlueCommonDemand glueCommonDemand) {
		return glueCommonDemandMapper.selectGlueCommonDemandList(glueCommonDemand);
	}

	/**
	 * 保存密炼机常用大规格设置信息（id为空则新增，id不为空则修改）
	 *
	 * @param glueCommonDemand
	 */
	@Override
	public void saveGlueCommonDemand(GlueCommonDemand glueCommonDemand) {
		if (ZltConstant.NOT_UNIQUE.equals(checkGlueCommonDemandUnique(glueCommonDemand))) {
			throw new RuntimeException(I18nUtil.getMessage("setting.glueCommonDemand.database.unique"));
		}

		List<String> majorTypes = new ArrayList<>();
		majorTypes.add(glueCommonDemand.getMachineCode());
		Set<String> zlGLueList = mesBasMaterialMapper.listMesBasMaterial(Arrays.asList(5)).stream()
				.collect(Collectors.toSet());
		if (!zlGLueList.contains(glueCommonDemand.getGlue())) { // 校验胶料是不是终炼胶
			throw new RuntimeException(I18nUtil.getMessage("setting.glueCommonDemand.excel.error.glue"));
		}

		glueCommonDemand.setBaseValue(glueCommonDemand.getId());
		this.saveOrUpdate(glueCommonDemand);
	}

	/**
	 * 批量删除密炼机常用大规格设置
	 *
	 * @param ids 需要删除的密炼机常用大规格设置ID
	 * @return 结果
	 */
	@Override
	public int deleteGlueCommonDemandByIds(Long[] ids) {
		return glueCommonDemandMapper.deleteGlueCommonDemandByIds(ids);
	}

	/**
	 * 校验密炼机常用大规格设置唯一性
	 */
	@Override
	public String checkGlueCommonDemandUnique(GlueCommonDemand glueCommonDemand) {
		if (glueCommonDemand == null) {
			return ZltConstant.NOT_UNIQUE;
		}

		QueryWrapper<GlueCommonDemand> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
		queryWrapper.eq("MIX_AREA", glueCommonDemand.getMixArea());
		queryWrapper.eq("MACHINE_CODE", glueCommonDemand.getMachineCode());
		queryWrapper.eq("GLUE", glueCommonDemand.getGlue());
		if (glueCommonDemand.getId() != null) {
			queryWrapper.ne("ID", glueCommonDemand.getId()); // 编辑的时候校验，要过滤掉自身的id
		}

		List<GlueCommonDemand> list = glueCommonDemandMapper.selectList(queryWrapper);
		if (list.size() > 0) {
			return ZltConstant.NOT_UNIQUE;
		}
		return ZltConstant.UNIQUE;
	}

	/**
	 * 导入密炼机常用大规格设置数据
	 *
	 * @param list          要导入的数据集合
	 * @param updateSupport 已存在记录是否更新
	 * @param importLogId   导入日志id
	 */
	@Override
	public AjaxResult importData(List<GlueCommonDemand> list, boolean updateSupport, Long importLogId) {
		// 初始化
		int successNum = 0;
		int failureNum = 0;
		List<GlueCommonDemand> importList = new ArrayList<>(); // 各种校验通过后的导入数据列表（最终可以导入数据库的计划）
		List<ImportErrorLog> importErrorLogs = new ArrayList<>(); // 导入错误明显列表
		List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>(); // 违反数据库唯一键的错误列表
		Map<Integer, Long> codeUniqueErrorMap = new HashMap<>(); // 用来存储哪一行数据违反了数据库唯一键

		try {
			// 将机台名转换为机台编号，无法转换的记录异常
			MixMachine mixMachine = new MixMachine();
			Map<String, MixMachine> machineMap = mixMachineService.selectMixMachineList(mixMachine).stream()
					.collect(Collectors.toMap(MixMachine::getMachineName, Function.identity(), (m1, m2) -> m1));
			Set<String> zlGLueList = mesBasMaterialMapper.listMesBasMaterial(Arrays.asList(5)).stream()
					.collect(Collectors.toSet());
			for (int i = 0; i < list.size(); i++) {
				GlueCommonDemand glueCommonDemand = list.get(i);
				String machineName = glueCommonDemand.getMachineName();
				String glue = glueCommonDemand.getGlue();
				String mixArea = glueCommonDemand.getMixArea();
				String machineCode = null;
				if (StringUtils.isNotEmpty(machineName)) {
					MixMachine machine = machineMap.get(machineName);
					// 取不到机台，或者取到机台了但是与密炼区不匹配，都提示机台错误
					if (machine == null || StringUtils.isNotEmpty(mixArea) && !mixArea.equals(machine.getMixArea())) {
						machineCode = machineName;
						glueCommonDemand.setId(-999L); // 校验没通过，设置id为-999作为标记
						String message = I18nUtil.getMessage("setting.glueCommonDemand.excel.error.machine");
						addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
					} else {
						machineCode = machine.getMachineCode();
					}
				}

				if (StringUtils.isNotEmpty(glue)) {
					if (!zlGLueList.contains(glue)) { // 校验胶料是不是终炼胶
						glueCommonDemand.setId(-999L); // 校验没通过，设置id为-999作为标记
						String message = I18nUtil.getMessage("setting.glueCommonDemand.excel.error.glue");
						addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
					}

				}

				glueCommonDemand.setMachineCode(machineCode != null ? machineCode : machineName); // 没有合法的机台，则直接把机台名称赋值给编号
			}

			if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
				// 没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
				codeUniqueErrorLogs = this.glueCommonDemandMapper.listGlueCommonDemandNotUnique(list, importLogId,
						I18nUtil.getMessage("setting.glueCommonDemand.database.unique"), SecurityUtils.getUsername());
				importErrorLogs.addAll(codeUniqueErrorLogs);
				codeUniqueErrorMap = codeUniqueErrorLogs.stream()
						.collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
			}

			// 按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
			Map<String, Long> groupMap = list.stream()
					.collect(Collectors.groupingBy(
							a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getMachineCode(), a.getGlue()),
							Collectors.counting()));

			// 公共校验（非空校验、长度校验等）
			for (int i = 0; i < list.size(); i++) {
				GlueCommonDemand glueCommonDemand = list.get(i);
				// exce中重复记录校验
				Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(glueCommonDemand.getMixArea(),
						glueCommonDemand.getMachineCode(), glueCommonDemand.getGlue()));
				if (hasValue > 1) {
					// 导入的excel中的数据违反了唯一键约束
					glueCommonDemand.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
					String message = I18nUtil.getMessage("setting.glueCommonDemand.excel.unique");
					addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
				}

				// 违反数据库唯一键的记录
				if (codeUniqueErrorMap.containsKey(i + 2)) {
					// 数据已经系统中存在
					glueCommonDemand.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
				}

				List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueCommonDemand); // 校验excel每个单元格长度、类型等

				if (CollectionUtils.isEmpty(validated) && glueCommonDemand.getId() == null) {
					glueCommonDemand.setBaseValue(null);
					importList.add(glueCommonDemand);
				} else {
					glueCommonDemand.setId(-999L); // 校验没通过的记录，设置id为-999作为标记
					importErrorLogs.addAll(validated);
				}
			}

			// 勾选更新记录，调用merge即可
			if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
				glueCommonDemandMapper.mergeSql(importList); // 根据唯一键批量新增或修改
			} else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
				glueCommonDemandMapper.batchInsertGlueCommonDemandInfo(importList); // 批量插入
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
