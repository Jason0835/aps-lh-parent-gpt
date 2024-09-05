package com.zlt.aps.lh.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.lh.api.domain.dto.LhLossSettingDto;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.entity.LhLossSetting;
import com.zlt.aps.lh.mapper.LhLossSettingMapper;
import com.zlt.aps.lh.service.LhLossSettingService;
import com.zlt.aps.lh.service.LhMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 硫化损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-19
 */
@Service
public class LhLossSettingServiceImpl extends ServiceImpl<LhLossSettingMapper, LhLossSetting> implements LhLossSettingService {
    @Autowired
    private LhLossSettingMapper lhLossSettingMapper;

    @Autowired
    private LhMachineInfoService lhMachineInfoService;

    /**
     * 查询硫化损耗率设定
     *
     * @param id 硫化损耗率设定ID
     * @return 硫化损耗率设定
     */
    @Override
    public LhLossSettingDto selectLhLossSettingById(Long id) {
        return lhLossSettingMapper.selectLhLossSettingById(id);
    }

    /**
     * 查询硫化损耗率设定列表
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 硫化损耗率设定
     */
    @Override
    public List<LhLossSettingDto> selectLhLossSettingList(LhLossSetting lhLossSetting) {
        return lhLossSettingMapper.selectLhLossSettingList(lhLossSetting);
    }

    /**
     * 新增硫化损耗率设定
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 结果
     */
    @Override
    public int insertLhLossSetting(LhLossSetting lhLossSetting) {
        checkParamAndUnique(lhLossSetting);
        lhLossSetting.setBaseVale(null);
        return lhLossSettingMapper.insertLhLossSetting(lhLossSetting);
    }

    /**
     * 修改硫化损耗率设定
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 结果
     */
    @Override
    public int updateLhLossSetting(LhLossSetting lhLossSetting) {
        checkParamAndUnique(lhLossSetting);
        lhLossSetting.setBaseVale(lhLossSetting.getId());
        return lhLossSettingMapper.updateLhLossSetting(lhLossSetting);
    }

    /**
     * 批量删除硫化损耗率设定
     *
     * @param ids 需要删除的硫化损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteLhLossSettingByIds(Long[] ids) {
        return lhLossSettingMapper.deleteLhLossSettingByIds(ids);
    }

    /**
     * 删除硫化损耗率设定信息
     *
     * @param id 硫化损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteLhLossSettingById(Long id) {
        return lhLossSettingMapper.deleteLhLossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkLhLossSettingUnique(LhLossSetting lhLossSetting) {
        if (lhLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = lhLossSettingMapper.checkLhLossSettingUnique(lhLossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param lhLossSetting 要检查记录
     */
    private void checkParamAndUnique(LhLossSetting lhLossSetting) {

        if (StringUtils.isAllEmpty(lhLossSetting.getMachineCode(), lhLossSetting.getSapCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkLhLossSettingUnique(lhLossSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<LhLossSettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhLossSetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<LhMachineInfo> machineInfoList = lhMachineInfoService.selectMachineInfoList(new LhMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {

            //根据机台名称去重
            TreeSet<LhMachineInfo> treeSet = new TreeSet<LhMachineInfo>(new Comparator<LhMachineInfo>() {
                @Override
                public int compare(LhMachineInfo o1, LhMachineInfo o2) {
                    return o1.getMachineName().compareTo(o2.getMachineName());
                }
            });
            treeSet.addAll(machineInfoList);
            machineInfoList =new ArrayList<>(treeSet);

            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineName(), a.getMachineCode()));
        }

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSapCode()+a.getMachineName()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            LhLossSettingDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getSapCode()+dto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.loss.sapCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            // 特殊校验（代码和机台名称不能同时为空校验）
            String machineName = dto.getMachineName();
            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(dto.getSapCode())) {
                addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (StringUtils.isNotEmpty(machineName) && machineCodeMap.get(machineName) == null) {
                addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum = failureNum + 1;
                importErrorLogs.addAll(validated);
            } else{
                LhLossSetting newEntity = new LhLossSetting();
                dto.setMachineCode(machineCodeMap.get(machineName));
                BeanUtils.copyProperties(dto, newEntity);
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
                    lhLossSettingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        LhLossSettingDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        LhLossSetting newItem = new LhLossSetting();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        String unique = checkLhLossSettingUnique(newItem);
                        if (UserConstants.UNIQUE.equals(unique)) {
                            successNum++;
                            lhLossSettingMapper.insertLhLossSetting(newItem);
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
