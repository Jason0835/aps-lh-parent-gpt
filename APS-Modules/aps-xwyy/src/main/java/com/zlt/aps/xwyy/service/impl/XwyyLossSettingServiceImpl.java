package com.zlt.aps.xwyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.dto.XwyyLossSettingDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.entity.XwyyLossSetting;
import com.zlt.aps.xwyy.mapper.XwyyLossSettingMapper;
import com.zlt.aps.xwyy.service.XwyyLossSettingService;
import com.zlt.aps.xwyy.service.XwyyMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 纤维压延损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-19
 */
@Service
public class XwyyLossSettingServiceImpl extends ServiceImpl<XwyyLossSettingMapper, XwyyLossSetting> implements XwyyLossSettingService {
    @Autowired
    private XwyyLossSettingMapper xwyyLossSettingMapper;

    @Autowired
    private XwyyMachineInfoService xwyyMachineInfoService;

    /**
     * 查询纤维压延损耗率设定
     *
     * @param id 纤维压延损耗率设定ID
     * @return 纤维压延损耗率设定
     */
    @Override
    public XwyyLossSettingDto selectXwyyLossSettingById(Long id) {
        return xwyyLossSettingMapper.selectXwyyLossSettingById(id);
    }

    /**
     * 查询纤维压延损耗率设定列表
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 纤维压延损耗率设定
     */
    @Override
    public List<XwyyLossSettingDto> selectXwyyLossSettingList(XwyyLossSetting xwyyLossSetting) {
        return xwyyLossSettingMapper.selectXwyyLossSettingList(xwyyLossSetting);
    }

    /**
     * 新增纤维压延损耗率设定
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 结果
     */
    @Override
    public int insertXwyyLossSetting(XwyyLossSetting xwyyLossSetting) {
        checkParamAndUnique(xwyyLossSetting);
        xwyyLossSetting.setBaseVale(null);
        return xwyyLossSettingMapper.insertXwyyLossSetting(xwyyLossSetting);
    }

    /**
     * 修改纤维压延损耗率设定
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 结果
     */
    @Override
    public int updateXwyyLossSetting(XwyyLossSetting xwyyLossSetting) {
        checkParamAndUnique(xwyyLossSetting);
        xwyyLossSetting.setBaseVale(xwyyLossSetting.getId());
        return xwyyLossSettingMapper.updateXwyyLossSetting(xwyyLossSetting);
    }

    /**
     * 批量删除纤维压延损耗率设定
     *
     * @param ids 需要删除的纤维压延损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteXwyyLossSettingByIds(Long[] ids) {
        return xwyyLossSettingMapper.deleteXwyyLossSettingByIds(ids);
    }

    /**
     * 删除纤维压延损耗率设定信息
     *
     * @param id 纤维压延损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteXwyyLossSettingById(Long id) {
        return xwyyLossSettingMapper.deleteXwyyLossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkXwyyLossSettingUnique(XwyyLossSetting xwyyLossSetting) {
        if (xwyyLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = xwyyLossSettingMapper.checkXwyyLossSettingUnique(xwyyLossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param xwyyLossSetting 要检查记录
     */
    private void checkParamAndUnique(XwyyLossSetting xwyyLossSetting) {
        if (xwyyLossSetting.getMachineId() == null && StringUtils.isEmpty(xwyyLossSetting.getBigRollCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkXwyyLossSettingUnique(xwyyLossSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<XwyyLossSettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<XwyyLossSetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<XwyyMachineInfo> machineInfoList = xwyyMachineInfoService.selectMachineInfoList(new XwyyMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
//            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getId()));
            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineName(), a.getId()));
        }
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(v -> (v.getBigRollCode() + v.getMachineName()), Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            XwyyLossSettingDto dto = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(dto.getBigRollCode() + dto.getMachineName());
			if (hasValue > 1) {
				dto.setId(-999L);
				String columnName1 = I18nUtil.getMessage("ui.data.column.loss.xwyy.bigRollCode");
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
            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(dto.getBigRollCode())) {
                addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.column.machineCodeNotExist"), validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum = failureNum + 1;
                importErrorLogs.addAll(validated);
            } else{
                XwyyLossSetting newEntity = new XwyyLossSetting();
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
                    xwyyLossSettingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        XwyyLossSettingDto dto = list.get(i);
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        XwyyLossSetting newItem = new XwyyLossSetting();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setMachineId(machineCodeMap.get(dto.getMachineName()));
                        newItem.setBaseVale(null);

                        int num = xwyyLossSettingMapper.checkXwyyLossSettingUnique(newItem);
                        if (num <= 0) {
                            successNum++;
                            xwyyLossSettingMapper.insertXwyyLossSetting(newItem);
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
        this.xwyyLossSettingMapper.deleteAll();
    }
}
