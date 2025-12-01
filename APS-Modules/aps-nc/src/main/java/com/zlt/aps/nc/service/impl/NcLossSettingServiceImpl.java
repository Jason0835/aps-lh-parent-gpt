package com.zlt.aps.nc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.nc.api.domain.dto.NcLossSettingDto;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.entity.NcLossSetting;
import com.zlt.aps.nc.mapper.NcLossSettingMapper;
import com.zlt.aps.nc.service.NcLossSettingService;
import com.zlt.aps.nc.service.NcMachineInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 内衬损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-13
 */
@Service
public class NcLossSettingServiceImpl extends ServiceImpl<NcLossSettingMapper, NcLossSetting> implements NcLossSettingService {
    @Autowired
    private NcLossSettingMapper ncLossSettingMapper;
    @Autowired
    private NcMachineInfoService ncMachineInfoService;

    /**
     * 查询内衬损耗率设定
     *
     * @param id 内衬损耗率设定ID
     * @return 内衬损耗率设定
     */
    @Override
    public NcLossSettingDto selectNcLossSettingById(Long id) {
        return ncLossSettingMapper.selectNcLossSettingById(id);
    }

    /**
     * 查询内衬损耗率设定列表
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 内衬损耗率设定
     */
    @Override
    public List<NcLossSettingDto> selectNcLossSettingList(NcLossSetting ncLossSetting) {
        return ncLossSettingMapper.selectNcLossSettingList(ncLossSetting);
    }

    /**
     * 新增内衬损耗率设定
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 结果
     */
    @Override
    public int insertNcLossSetting(NcLossSetting ncLossSetting) {
        checkParamAndUnique(ncLossSetting);
        ncLossSetting.setBaseVale(null);
        return ncLossSettingMapper.insertNcLossSetting(ncLossSetting);
    }

    /**
     * 修改内衬损耗率设定
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 结果
     */
    @Override
    public int updateNcLossSetting(NcLossSetting ncLossSetting) {
        checkParamAndUnique(ncLossSetting);
        ncLossSetting.setBaseVale(ncLossSetting.getId());
        return ncLossSettingMapper.updateNcLossSetting(ncLossSetting);
    }

    /**
     * 批量删除内衬损耗率设定
     *
     * @param ids 需要删除的内衬损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteNcLossSettingByIds(Long[] ids) {
        return ncLossSettingMapper.deleteNcLossSettingByIds(ids);
    }

    /**
     * 删除内衬损耗率设定信息
     *
     * @param id 内衬损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteNcLossSettingById(Long id) {
        return ncLossSettingMapper.deleteNcLossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkNcLossSettingUnique(NcLossSetting ncLossSetting) {
        if (ncLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = ncLossSettingMapper.checkNcLossSettingUnique(ncLossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }


    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param ncLossSetting 要检查记录
     */
    private void checkParamAndUnique(NcLossSetting ncLossSetting) {
        if (ncLossSetting.getMachineId() == null && StringUtils.isEmpty(ncLossSetting.getLiningCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkNcLossSettingUnique(ncLossSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<NcLossSettingDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<NcLossSettingDto> importList = new ArrayList<>();
        //将机台名称转换为机台id，并做校验
        List<NcMachineInfo> machineInfoList = ncMachineInfoService.selectMachineInfoList(new NcMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(NcMachineInfo::getMachineCode, NcMachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(NcMachineInfo::getMachineName, NcMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getLiningCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            NcLossSettingDto lossSetting = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(lossSetting.getLiningCode()+lossSetting.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                lossSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.loss.liningCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, lossSetting);
            String machineName = lossSetting.getMachineName();
            Long machineId = machineCodeMap.get(machineName);

            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(lossSetting.getLiningCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                lossSetting.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                lossSetting.setMachineId(machineId);
                lossSetting.setBaseVale(null);
                if (StringUtils.isBlank(lossSetting.getLiningCode())) {
                    lossSetting.setLiningCode(null);
                }
                importList.add(lossSetting);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                ncLossSettingMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    NcLossSettingDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    NcLossSetting tmLossSetting = new NcLossSetting();
                    BeanUtils.copyProperties(excelItem, tmLossSetting);
                    int unique = ncLossSettingMapper.checkNcLossSettingUnique(tmLossSetting);
                    if (unique == 0) {
                        //不存在插入
                        successNum++;
                        ncLossSettingMapper.insertNcLossSetting(tmLossSetting);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.loss.unique"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public void deleteAll() {
        this.ncLossSettingMapper.deleteAll();
    }
}
