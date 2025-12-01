package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.dto.Cd15LossSettingDto;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.entity.Cd15LossSetting;
import com.zlt.aps.cd15.mapper.Cd15LossSettingMapper;
import com.zlt.aps.cd15.service.Cd15LossSettingService;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
import com.zlt.aps.common.core.utils.ImportUtil;
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
 * 15度裁断损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-19
 */
@Service
public class Cd15LossSettingServiceImpl extends ServiceImpl<Cd15LossSettingMapper, Cd15LossSetting> implements Cd15LossSettingService {
    @Autowired
    private Cd15LossSettingMapper cd15LossSettingMapper;
    @Autowired
    private Cd15MachineInfoService cd15MachineInfoService;

    /**
     * 查询15度裁断损耗率设定
     *
     * @param id 15度裁断损耗率设定ID
     * @return 15度裁断损耗率设定
     */
    @Override
    public Cd15LossSettingDto selectCd15LossSettingById(Long id) {
        return cd15LossSettingMapper.selectCd15LossSettingById(id);
    }

    /**
     * 查询15度裁断损耗率设定列表
     *
     * @param cd15LossSetting 15度裁断损耗率设定
     * @return 15度裁断损耗率设定
     */
    @Override
    public List<Cd15LossSettingDto> selectCd15LossSettingList(Cd15LossSetting cd15LossSetting) {
        return cd15LossSettingMapper.selectCd15LossSettingList(cd15LossSetting);
    }

    /**
     * 新增15度裁断损耗率设定
     *
     * @param cd15LossSetting 15度裁断损耗率设定
     * @return 结果
     */
    @Override
    public int insertCd15LossSetting(Cd15LossSetting cd15LossSetting) {
        checkParamAndUnique(cd15LossSetting);
        cd15LossSetting.setBaseVale(null);
        return cd15LossSettingMapper.insertCd15LossSetting(cd15LossSetting);
    }

    /**
     * 修改15度裁断损耗率设定
     *
     * @param cd15LossSetting 15度裁断损耗率设定
     * @return 结果
     */
    @Override
    public int updateCd15LossSetting(Cd15LossSetting cd15LossSetting) {
        checkParamAndUnique(cd15LossSetting);
        cd15LossSetting.setBaseVale(cd15LossSetting.getId());
        return cd15LossSettingMapper.updateCd15LossSetting(cd15LossSetting);
    }

    /**
     * 批量删除15度裁断损耗率设定
     *
     * @param ids 需要删除的15度裁断损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteCd15LossSettingByIds(Long[] ids) {
        return cd15LossSettingMapper.deleteCd15LossSettingByIds(ids);
    }

    /**
     * 删除15度裁断损耗率设定信息
     *
     * @param id 15度裁断损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteCd15LossSettingById(Long id) {
        return cd15LossSettingMapper.deleteCd15LossSettingById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkCd15LossSettingUnique(Cd15LossSetting cd15LossSetting) {
        if (cd15LossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = cd15LossSettingMapper.checkCd15LossSettingUnique(cd15LossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param cd15LossSetting 要检查记录
     */
    private void checkParamAndUnique(Cd15LossSetting cd15LossSetting) {
        if (cd15LossSetting.getMachineId() == null && StringUtils.isEmpty(cd15LossSetting.getSteelStripCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkCd15LossSettingUnique(cd15LossSetting);
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
    public AjaxResult importData(List<Cd15LossSettingDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd15LossSettingDto> importList = new ArrayList<>();
        //将机台名称转换为机台id，并做校验
        List<Cd15MachineInfo> machineInfoList = cd15MachineInfoService.selectMachineInfoList(new Cd15MachineInfo());
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getMachineCode, Cd15MachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getMachineName, Cd15MachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSteelStripCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            Cd15LossSettingDto lossSetting = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(lossSetting.getSteelStripCode()+lossSetting.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                lossSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.loss.steelStripCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, lossSetting);
            String machineName = lossSetting.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            // 代码和机台名称不能同时为空校验
            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(lossSetting.getSteelStripCode())) {
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
                if (StringUtils.isBlank(lossSetting.getSteelStripCode())) {
                    lossSetting.setSteelStripCode(null);
                }
                importList.add(lossSetting);
            } else {
                failureNum++;
                lossSetting.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    cd15LossSettingMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        Cd15LossSettingDto excelItem = list.get(i);
                        if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                            continue;
                        }
                        // 唯一性校验
                        Cd15LossSetting tmLossSetting = new Cd15LossSetting();
                        BeanUtils.copyProperties(excelItem, tmLossSetting);
                        int unique = cd15LossSettingMapper.checkCd15LossSettingUnique(tmLossSetting);
                        if (unique == 0) {
                            //不存在插入
                            successNum++;
                            cd15LossSettingMapper.insertCd15LossSetting(tmLossSetting);
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
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public void deleteAll() {
        this.cd15LossSettingMapper.deleteAll();
    }
}
