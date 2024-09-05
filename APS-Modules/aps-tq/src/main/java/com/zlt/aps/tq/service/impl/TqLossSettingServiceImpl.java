package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.dto.TqLossSettingDto;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.entity.TqLossSetting;
import com.zlt.aps.tq.mapper.TqLossSettingMapper;
import com.zlt.aps.tq.service.TqLossSettingService;
import com.zlt.aps.tq.service.TqMachineInfoService;
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
 * 胎圈损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-13
 */
@Service
public class TqLossSettingServiceImpl extends ServiceImpl<TqLossSettingMapper, TqLossSetting> implements TqLossSettingService {
    @Autowired
    private TqLossSettingMapper tqLossSettingMapper;

    @Autowired
    private TqMachineInfoService tqMachineInfoService;

    /**
     * 查询胎圈损耗率设定
     *
     * @param id 胎圈损耗率设定ID
     * @return 胎圈损耗率设定
     */
    @Override
    public TqLossSettingDto selectTqLossSettingById(Long id) {
        return tqLossSettingMapper.selectTqLossSettingById(id);
    }

    /**
     * 查询胎圈损耗率设定列表
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 胎圈损耗率设定
     */
    @Override
    public List<TqLossSettingDto> selectTqLossSettingList(TqLossSetting tqLossSetting) {
        return tqLossSettingMapper.selectTqLossSettingList(tqLossSetting);
    }

    /**
     * 新增胎圈损耗率设定
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 结果
     */
    @Override
    public int insertTqLossSetting(TqLossSetting tqLossSetting) {
        checkParamAndUnique(tqLossSetting);
        tqLossSetting.setBaseVale(null);
        return tqLossSettingMapper.insertTqLossSetting(tqLossSetting);
    }

    /**
     * 修改胎圈损耗率设定
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 结果
     */
    @Override
    public int updateTqLossSetting(TqLossSetting tqLossSetting) {
        checkParamAndUnique(tqLossSetting);
        tqLossSetting.setBaseVale(tqLossSetting.getId());
        return tqLossSettingMapper.updateTqLossSetting(tqLossSetting);
    }

    /**
     * 批量删除胎圈损耗率设定
     *
     * @param ids 需要删除的胎圈损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteTqLossSettingByIds(Long[] ids) {
        return tqLossSettingMapper.deleteTqLossSettingByIds(ids);
    }

    /**
     * 删除胎圈损耗率设定信息
     *
     * @param id 胎圈损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteTqLossSettingById(Long id) {
        return tqLossSettingMapper.deleteTqLossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkTqLossSettingUnique(TqLossSetting tqLossSetting) {
        if (tqLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = tqLossSettingMapper.checkTqLossSettingUnique(tqLossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
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
    public AjaxResult importData(List<TqLossSettingDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqLossSettingDto> importList = new ArrayList<>();
        //将机台名称转换为机台id，并做校验
        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TqMachineInfo::getMachineCode, TqMachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getBeadCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqLossSettingDto lossSetting = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(lossSetting.getBeadCode()+lossSetting.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                lossSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.loss.beadCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, lossSetting);
            String machineName = lossSetting.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            // 代码和机台名称不能同时为空校验
            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(lossSetting.getBeadCode())) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineId == null && StringUtils.isNotEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                lossSetting.setMachineId(machineId);
                lossSetting.setBaseVale(null);
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
                tqLossSettingMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TqLossSettingDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    TqLossSetting tmLossSetting = new TqLossSetting();
                    BeanUtils.copyProperties(excelItem, tmLossSetting);
                    int unique = tqLossSettingMapper.checkTqLossSettingUnique(tmLossSetting);
                    if (unique == 0) {
                        //不存在插入
                        successNum++;
                        tqLossSettingMapper.insertTqLossSetting(tmLossSetting);
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
        this.tqLossSettingMapper.deleteAll();
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param tqLossSetting 要检查记录
     */
    private void checkParamAndUnique(TqLossSetting tqLossSetting) {
        if (tqLossSetting.getMachineId() == null && StringUtils.isEmpty(tqLossSetting.getBeadCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkTqLossSettingUnique(tqLossSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
    }

}
