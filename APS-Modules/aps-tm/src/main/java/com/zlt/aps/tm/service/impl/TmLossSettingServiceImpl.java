package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tm.api.domain.dto.TmLossSettingDto;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.entity.TmLossSetting;
import com.zlt.aps.tm.mapper.TmLossSettingMapper;
import com.zlt.aps.tm.service.TmLossSettingService;
import com.zlt.aps.tm.service.TmMachineInfoService;
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
 * 胎面损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-12
 */
@Service
public class TmLossSettingServiceImpl extends ServiceImpl<TmLossSettingMapper, TmLossSetting> implements TmLossSettingService {
    @Autowired
    private TmLossSettingMapper tmLossSettingMapper;
    @Autowired
    private TmMachineInfoService tmMachineInfoService;

    /**
     * 查询胎面损耗率设定
     *
     * @param id 胎面损耗率设定ID
     * @return 胎面损耗率设定
     */
    @Override
    public TmLossSettingDto selectTmLossSettingById(Long id) {
        return tmLossSettingMapper.selectTmLossSettingById(id);
    }

    /**
     * 查询胎面损耗率设定列表
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 胎面损耗率设定
     */
    @Override
    public List<TmLossSettingDto> selectTmLossSettingList(TmLossSetting tmLossSetting) {
        return tmLossSettingMapper.selectTmLossSettingList(tmLossSetting);
    }

    /**
     * 新增胎面损耗率设定
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 结果
     */
    @Override
    public int insertTmLossSetting(TmLossSetting tmLossSetting) {
        checkParamAndUnique(tmLossSetting);
        tmLossSetting.setBaseVale(null);
        return tmLossSettingMapper.insertTmLossSetting(tmLossSetting);
    }

    /**
     * 修改胎面损耗率设定
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 结果
     */
    @Override
    public int updateTmLossSetting(TmLossSetting tmLossSetting) {
        checkParamAndUnique(tmLossSetting);
        tmLossSetting.setBaseVale(tmLossSetting.getId());
        return tmLossSettingMapper.updateTmLossSetting(tmLossSetting);
    }

    /**
     * 批量删除胎面损耗率设定
     *
     * @param ids 需要删除的胎面损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteTmLossSettingByIds(Long[] ids) {
        return tmLossSettingMapper.deleteTmLossSettingByIds(ids);
    }

    /**
     * 删除胎面损耗率设定信息
     *
     * @param id 胎面损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteTmLossSettingById(Long id) {
        return tmLossSettingMapper.deleteTmLossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkTmLossSettingUnique(TmLossSetting tmLossSetting) {
        if (tmLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = tmLossSettingMapper.checkTmLossSettingUnique(tmLossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param tmLossSetting 要检查记录
     */
    private void checkParamAndUnique(TmLossSetting tmLossSetting) {
        if (tmLossSetting.getMachineId() == null && StringUtils.isEmpty(tmLossSetting.getTreadCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkTmLossSettingUnique(tmLossSetting);
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
    public AjaxResult importData(List<TmLossSettingDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmLossSettingDto> importList = new ArrayList<>();
        //将机台名称转换为机台id，并做校验
        List<TmMachineInfo> machineInfoList = tmMachineInfoService.selectMachineInfoList(new TmMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineCode, TmMachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineName, TmMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getTreadCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TmLossSettingDto lossSetting = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(lossSetting.getTreadCode()+lossSetting.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                lossSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.treadCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, lossSetting);
            String machineName = lossSetting.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            // 代码和机台名称不能同时为空校验
            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(lossSetting.getTreadCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            if (CollectionUtils.isEmpty(validated)) {
                lossSetting.setMachineId(machineId);
                lossSetting.setBaseVale(null);
                if (StringUtils.isBlank(lossSetting.getTreadCode())) {
                    lossSetting.setTreadCode(null);
                }
                importList.add(lossSetting);
            } else {
                failureNum++;
                lossSetting.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tmLossSettingMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TmLossSettingDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    TmLossSetting tmLossSetting = new TmLossSetting();
                    BeanUtils.copyProperties(excelItem, tmLossSetting);
                    int unique = tmLossSettingMapper.checkTmLossSettingUnique(tmLossSetting);
                    if (unique == 0) {
                        //不存在插入
                        successNum++;
                        tmLossSettingMapper.insertTmLossSetting(tmLossSetting);
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
        this.tmLossSettingMapper.deleteAll();
    }
}
