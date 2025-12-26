package com.zlt.aps.cd90.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.dto.Cd90QuotaSettingDto;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.entity.Cd90QuotaSetting;
import com.zlt.aps.cd90.mapper.Cd90QuotaSettingMapper;
import com.zlt.aps.cd90.service.Cd90MachineInfoService;
import com.zlt.aps.cd90.service.Cd90QuotaSettingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;


/**
 * 90度裁断定额设定Service业务层处理
 *
 * @author chen
 * @date 2021-06-29
 */
@Service
public class Cd90QuotaSettingServiceImpl extends ServiceImpl<Cd90QuotaSettingMapper, Cd90QuotaSetting> implements Cd90QuotaSettingService {
    @Autowired
    private Cd90QuotaSettingMapper cd90QuotaSettingMapper;

    @Autowired
    private Cd90MachineInfoService cd90MachineInfoService;

    /**
     * 查询90度定额设定列表
     *
     * @param quotaSetting 90度定额设定
     * @return 90度定额设定集合
     */
    @Override
    public List<Cd90QuotaSettingDto> selectQuotaSettingList(Cd90QuotaSetting quotaSetting) {
        return cd90QuotaSettingMapper.selectQuotaSettingList(quotaSetting);
    }

    /**
     * 查询90度定额设定
     *
     * @param id 90度定额设定ID
     * @return 90度定额设定
     */
    @Override
    public Cd90QuotaSetting selectQuotaSettingById(Long id) {
        LambdaQueryWrapper<Cd90QuotaSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90QuotaSetting::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(Cd90QuotaSetting::getId, id);
        return cd90QuotaSettingMapper.selectOne(wrapper);
    }

    /**
     * 修改90度定额设定
     *
     * @param quotaSetting 90度定额设定
     */
    @Override
    public AjaxResult saveQuotaSetting(Cd90QuotaSetting quotaSetting) {
        // 定额设定记录唯一性校验
        checkParamAndUnique(quotaSetting);
        quotaSetting.setBaseVale(quotaSetting.getId());
        saveOrUpdate(quotaSetting);
        return AjaxResult.success();
    }

    /**
     * 批量删除90度定额设定
     *
     * @param ids 需要删除的90度定额设定ID
     */
    @Override
    public void deleteQuotaSettingByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        cd90QuotaSettingMapper.deleteCd90QuotaSettingByIds(ids);
    }

    /**
     * 验证定额设定信息唯一性
     *
     * @param quotaSetting 要校验的记录
     */
    @Override
    public String checkUnique(Cd90QuotaSetting quotaSetting) {
        List<Cd90QuotaSetting> list = cd90QuotaSettingMapper.checkUnique(quotaSetting);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param quotaSetting 要检查记录
     */
    private void checkParamAndUnique(Cd90QuotaSetting quotaSetting) {
        if (quotaSetting.getMachineId() == null && StringUtils.isEmpty(quotaSetting.getClothCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.quota.isAllNull"));
        }
        String unique = checkUnique(quotaSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<Cd90QuotaSettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<Cd90QuotaSetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<Cd90MachineInfo> machineInfoList = cd90MachineInfoService.selectMachineInfoList(new Cd90MachineInfo());
        Map<String, Long> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getId()));
        }
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(v -> (v.getClothCode() + v.getMachineName()), Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            Cd90QuotaSettingDto dto = list.get(i);

			// excel内业务主键唯一校验
			if (groupMap.get(dto.getClothCode() + dto.getMachineName()) > 1) {
				dto.setId(-999L);
				String columnName1 = I18nUtil.getMessage("ui.data.column.stock.clothCode");
				String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineCode");
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
                Cd90QuotaSetting newEntity = new Cd90QuotaSetting();
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
                    cd90QuotaSettingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {

                        Cd90QuotaSettingDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        Cd90QuotaSetting newItem = new Cd90QuotaSetting();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setMachineId(machineCodeMap.get(dto.getMachineName()));
                        newItem.setBaseVale(null);

                        List<Cd90QuotaSetting> exist = cd90QuotaSettingMapper.checkUnique(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            saveOrUpdate(newItem);
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
}
