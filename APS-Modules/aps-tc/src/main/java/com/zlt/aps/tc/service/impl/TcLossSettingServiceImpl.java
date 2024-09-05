package com.zlt.aps.tc.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tc.api.domain.dto.TcLossSettingDto;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.entity.TcLossSetting;
import com.zlt.aps.tc.mapper.TcLossSettingMapper;
import com.zlt.aps.tc.service.TcLossSettingService;
import com.zlt.aps.tc.service.TcMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 胎侧损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-13
 */
@Service
public class TcLossSettingServiceImpl extends ServiceImpl<TcLossSettingMapper, TcLossSetting> implements TcLossSettingService {
    @Autowired
    private TcLossSettingMapper tcLossSettingMapper;

    @Autowired
    private TcMachineInfoService tcMachineInfoService;

    /**
     * 查询胎侧损耗率设定
     *
     * @param id 胎侧损耗率设定ID
     * @return 胎侧损耗率设定
     */
    @Override
    public TcLossSettingDto selectTcLossSettingById(Long id) {
        return tcLossSettingMapper.selectTcLossSettingById(id);
    }

    /**
     * 查询胎侧损耗率设定列表
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 胎侧损耗率设定
     */
    @Override
    public List<TcLossSettingDto> selectTcLossSettingList(TcLossSetting tcLossSetting) {
        return tcLossSettingMapper.selectTcLossSettingList(tcLossSetting);
    }

    /**
     * 新增胎侧损耗率设定
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 结果
     */
    @Override
    public int insertTcLossSetting(TcLossSetting tcLossSetting) {
        checkParamAndUnique(tcLossSetting);
        tcLossSetting.setBaseVale(null);
        return tcLossSettingMapper.insertTcLossSetting(tcLossSetting);
    }

    /**
     * 修改胎侧损耗率设定
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 结果
     */
    @Override
    public int updateTcLossSetting(TcLossSetting tcLossSetting) {
        checkParamAndUnique(tcLossSetting);
        tcLossSetting.setBaseVale(tcLossSetting.getId());
        return tcLossSettingMapper.updateTcLossSetting(tcLossSetting);
    }

    /**
     * 批量删除胎侧损耗率设定
     *
     * @param ids 需要删除的胎侧损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteTcLossSettingByIds(Long[] ids) {
        return tcLossSettingMapper.deleteTcLossSettingByIds(ids);
    }

    /**
     * 删除胎侧损耗率设定信息
     *
     * @param id 胎侧损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteTcLossSettingById(Long id) {
        return tcLossSettingMapper.deleteTcLossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkTcLossSettingUnique(TcLossSetting tcLossSetting) {
        if (tcLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = tcLossSettingMapper.checkTcLossSettingUnique(tcLossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param tcLossSetting 要检查记录
     */
    private void checkParamAndUnique(TcLossSetting tcLossSetting) {
        if (tcLossSetting.getMachineId() == null && StringUtils.isEmpty(tcLossSetting.getSidewallCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkTcLossSettingUnique(tcLossSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<TcLossSettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化值准备
        int successNum = 0;
        int failureNum = 0;
        List<TcLossSettingDto> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<TcMachineInfo> machineInfoList = tcMachineInfoService.selectMachineInfoList(new TcMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            ImportUtil.addImportErrorLog(importLogId, null, errorMsg, importErrorLogs);
            return AjaxResult.error(errorMsg, importErrorLogs);
        }
        Map<String, Long> machineCodeMap = new HashMap<>();
//        machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getId()));
        machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineName(), a.getId()));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSidewallCode()+a.getMachineName()), Collectors.counting()));

        //校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            TcLossSettingDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getSidewallCode()+dto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.loss.sidewallCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }


            String machineName = dto.getMachineName();
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(dto.getSidewallCode())) {
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
                dto.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                dto.setMachineId(machineCodeMap.get(machineName));
                dto.setBaseVale(null);
                newList.add(dto);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                successNum = newList.size();
                tcLossSettingMapper.mergeSql(newList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TcLossSettingDto entity = list.get(i);
                    // 错误跳过
                    if (entity.getId() != null && entity.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    TcLossSetting lossSetting = new TcLossSetting();
                    BeanUtils.copyProperties(entity, lossSetting);
                    String unique = checkTcLossSettingUnique(lossSetting);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.saveOrUpdate(lossSetting);
                    } else {
                        failureNum++;
                        String message = I18nUtil.getMessage("ui.error.message.quota.unique");
                        ImportUtil.addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportUtil.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
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
        this.tcLossSettingMapper.deleteAll();
    }

}
