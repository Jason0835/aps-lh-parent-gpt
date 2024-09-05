package com.zlt.aps.xwyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.dto.XwyyQuotaSettingDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.entity.XwyyQuotaSetting;
import com.zlt.aps.xwyy.mapper.XwyyMachineInfoMapper;
import com.zlt.aps.xwyy.mapper.XwyyQuotaSettingMapper;
import com.zlt.aps.xwyy.service.ImportErrorLogService;
import com.zlt.aps.xwyy.service.XwyyQuotaSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 纤维压延定额设定Service业务层处理
 *
 * @author chen
 * @date 2021-06-29
 */
@Service
public class XwyyQuotaSettingServiceImpl extends ServiceImpl<XwyyQuotaSettingMapper, XwyyQuotaSetting> implements XwyyQuotaSettingService {
    @Autowired
    private XwyyQuotaSettingMapper xwyyQuotaSettingMapper;

    @Autowired
    private XwyyMachineInfoMapper xwyyMachineInfoMapper;

    @Autowired
    private ImportErrorLogService importErrorLogService;

    /**
     * 查询纤维压延定额设定列表
     *
     * @param quotaSetting 纤维压延定额设定
     * @return 纤维压延定额设定集合
     */
    @Override
    public List<XwyyQuotaSettingDto> selectQuotaSettingList(XwyyQuotaSetting quotaSetting) {
        return xwyyQuotaSettingMapper.selectQuotaSettingList(quotaSetting);
    }

    /**
     * 查询纤维压延定额设定
     *
     * @param id 纤维压延定额设定ID
     * @return 纤维压延定额设定
     */
    @Override
    public XwyyQuotaSetting selectQuotaSettingById(Long id) {
        LambdaQueryWrapper<XwyyQuotaSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XwyyQuotaSetting::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(XwyyQuotaSetting::getId, id);
        return xwyyQuotaSettingMapper.selectOne(wrapper);
    }

    /**
     * 修改纤维压延定额设定
     *
     * @param quotaSetting 纤维压延定额设定
     */
    @Override
    public AjaxResult saveQuotaSetting(XwyyQuotaSetting quotaSetting) {
        checkParamAndUnique(quotaSetting);
        quotaSetting.setBaseVale(quotaSetting.getId());
        saveOrUpdate(quotaSetting);
        return AjaxResult.success();
    }

    /**
     * 批量删除纤维压延定额设定
     *
     * @param ids 需要删除的纤维压延定额设定ID
     */
    @Override
    public void deleteQuotaSettingByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        xwyyQuotaSettingMapper.deleteQuotaSettingByIds(ids);
    }

    /**
     * 验证定额设定信息唯一性
     *
     * @param quotaSetting 要校验的记录
     */
    @Override
    public String checkUnique(XwyyQuotaSetting quotaSetting) {
        List<XwyyQuotaSetting> list = xwyyQuotaSettingMapper.checkUnique(quotaSetting);
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
    private void checkParamAndUnique(XwyyQuotaSetting quotaSetting) {
        if (quotaSetting.getMachineId() == null && StringUtils.isEmpty(quotaSetting.getBigRollCode())) {
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
    public AjaxResult importData(List<XwyyQuotaSettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<XwyyQuotaSetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<XwyyMachineInfo> machineInfoList = xwyyMachineInfoMapper.selectMachineInfoList(new XwyyMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineCodeMap = new HashMap<>();
        for (XwyyMachineInfo machineInfo : machineInfoList) {
            machineCodeMap.put(machineInfo.getMachineCode(), machineInfo.getId());
        }
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(v -> (v.getBigRollCode() + v.getMachineName()), Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            XwyyQuotaSettingDto dto = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(dto.getBigRollCode() + dto.getMachineName());
			if (hasValue > 1) {
				dto.setId(-999L);
				String columnName1 = I18nUtil.getMessage("ui.data.column.xwyy.quota.bigRollCode");
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

            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(dto.getBigRollCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (StringUtils.isNotEmpty(machineName) && machineCodeMap.get(machineName) == null) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineCodeNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum = failureNum + 1;
                importErrorLogs.addAll(validated);
            } else{
                XwyyQuotaSetting newEntity = new XwyyQuotaSetting();
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
                    xwyyQuotaSettingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        XwyyQuotaSettingDto dto = list.get(i);
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        XwyyQuotaSetting newItem = new XwyyQuotaSetting();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setMachineId(machineCodeMap.get(dto.getMachineName()));
                        newItem.setBaseVale(null);

                        List<XwyyQuotaSetting> exist = xwyyQuotaSettingMapper.checkUnique(newItem);
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
