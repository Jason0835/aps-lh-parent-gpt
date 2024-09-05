package com.zlt.aps.cd90.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.dto.Cd90LossSettingDto;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.entity.Cd90LossSetting;
import com.zlt.aps.cd90.mapper.Cd90LossSettingMapper;
import com.zlt.aps.cd90.service.Cd90LossSettingService;
import com.zlt.aps.cd90.service.Cd90MachineInfoService;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 90度裁断损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-19
 */
@Service
public class Cd90LossSettingServiceImpl extends ServiceImpl<Cd90LossSettingMapper, Cd90LossSetting> implements Cd90LossSettingService {
    @Autowired
    private Cd90LossSettingMapper cd90LossSettingMapper;

    @Autowired
    private Cd90MachineInfoService cd90MachineInfoService;

    /**
     * 查询90度裁断损耗率设定
     *
     * @param id 90度裁断损耗率设定ID
     * @return 90度裁断损耗率设定
     */
    @Override
    public Cd90LossSettingDto selectCd90LossSettingById(Long id) {
        return cd90LossSettingMapper.selectCd90LossSettingById(id);
    }

    /**
     * 查询90度裁断损耗率设定列表
     *
     * @param cd90LossSetting 90度裁断损耗率设定
     * @return 90度裁断损耗率设定
     */
    @Override
    public List<Cd90LossSettingDto> selectCd90LossSettingList(Cd90LossSetting cd90LossSetting) {
        return cd90LossSettingMapper.selectCd90LossSettingList(cd90LossSetting);
    }

    /**
     * 新增90度裁断损耗率设定
     *
     * @param cd90LossSetting 90度裁断损耗率设定
     * @return 结果
     */
    @Override
    public int insertCd90LossSetting(Cd90LossSetting cd90LossSetting) {
        checkParamAndUnique(cd90LossSetting);
        cd90LossSetting.setBaseVale(null);
        return cd90LossSettingMapper.insertCd90LossSetting(cd90LossSetting);
    }

    /**
     * 修改90度裁断损耗率设定
     *
     * @param cd90LossSetting 90度裁断损耗率设定
     * @return 结果
     */
    @Override
    public int updateCd90LossSetting(Cd90LossSetting cd90LossSetting) {
        checkParamAndUnique(cd90LossSetting);
        cd90LossSetting.setBaseVale(cd90LossSetting.getId());
        return cd90LossSettingMapper.updateCd90LossSetting(cd90LossSetting);
    }

    /**
     * 批量删除90度裁断损耗率设定
     *
     * @param ids 需要删除的90度裁断损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteCd90LossSettingByIds(Long[] ids) {
        return cd90LossSettingMapper.deleteCd90LossSettingByIds(ids);
    }

    /**
     * 删除90度裁断损耗率设定信息
     *
     * @param id 90度裁断损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteCd90LossSettingById(Long id) {
        return cd90LossSettingMapper.deleteCd90LossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkCd90LossSettingUnique(Cd90LossSetting cd90LossSetting) {
        if (cd90LossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = cd90LossSettingMapper.checkCd90LossSettingUnique(cd90LossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param cd90LossSetting 要检查记录
     */
    private void checkParamAndUnique(Cd90LossSetting cd90LossSetting) {
        if (cd90LossSetting.getMachineId() == null && StringUtils.isEmpty(cd90LossSetting.getClothCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkCd90LossSettingUnique(cd90LossSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<Cd90LossSettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<Cd90LossSetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<Cd90MachineInfo> machineInfoList = cd90MachineInfoService.selectMachineInfoList(new Cd90MachineInfo());
        Map<String, Long> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
//            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getId()));
            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineName(), a.getId()));
        }
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(v -> (v.getClothCode() + v.getMachineName()), Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            Cd90LossSettingDto dto = list.get(i);

			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(dto.getClothCode() + dto.getMachineName());
			if (hasValue > 1) {
				dto.setId(-999L);
				String columnName1 = I18nUtil.getMessage("ui.data.column.stock.clothCode");
				String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"),
								columnName1 + "+" + columnName2),
						importErrorLogs);
				failureNum++;
				continue;
			}
            
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            // 特殊校验（代码和机台名称不能同时为空校验）
            String machineName = dto.getMachineName();

            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(dto.getClothCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineCodeNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum = failureNum + 1;
                importErrorLogs.addAll(validated);
            } else {
                Cd90LossSetting newEntity = new Cd90LossSetting();
                BeanUtils.copyProperties(dto, newEntity);
                newEntity.setMachineId(machineCodeMap.get(machineName));
                newEntity.setBaseVale(null);
                newList.add(newEntity);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    cd90LossSettingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        Cd90LossSettingDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        Cd90LossSetting newItem = new Cd90LossSetting();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setMachineId(machineCodeMap.get(dto.getMachineName()));
                        newItem.setBaseVale(null);

                        String unique = checkCd90LossSettingUnique(newItem);
                        if (UserConstants.UNIQUE.equals(unique)) {
                            successNum++;
                            cd90LossSettingMapper.insertCd90LossSetting(newItem);
                        } else {
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public void deleteAll() {
        this.cd90LossSettingMapper.deleteAll();
    }

}
