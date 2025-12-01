package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;

import com.zlt.aps.cx.mapper.entity.CxLossSettingMapper;
import com.zlt.aps.cx.service.CxLossSettingService;

import com.zlt.aps.cxlh.cx.api.domain.dto.CxLossSettingDto;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxLossSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 成型损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-19
 */
@Service
public class CxLossSettingServiceImpl extends ServiceImpl<CxLossSettingMapper, CxLossSetting> implements CxLossSettingService {
    @Autowired
    private CxLossSettingMapper cxLossSettingMapper;

    /**
     * 查询成型损耗率设定
     *
     * @param id 成型损耗率设定ID
     * @return 成型损耗率设定
     */
    @Override
    public CxLossSettingDto selectCxLossSettingById(Long id) {
        return cxLossSettingMapper.selectCxLossSettingById(id);
    }

    /**
     * 查询成型损耗率设定列表
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 成型损耗率设定
     */
    @Override
    public List<CxLossSettingDto> selectCxLossSettingList(CxLossSetting cxLossSetting) {
        return cxLossSettingMapper.selectCxLossSettingList(cxLossSetting);
    }

    /**
     * 新增成型损耗率设定
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 结果
     */
    @Override
    public int insertCxLossSetting(CxLossSetting cxLossSetting) {
        checkParamAndUnique(cxLossSetting);
        cxLossSetting.setBaseVale(null);
        return cxLossSettingMapper.insertCxLossSetting(cxLossSetting);
    }

    /**
     * 修改成型损耗率设定
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 结果
     */
    @Override
    public int updateCxLossSetting(CxLossSetting cxLossSetting) {
        checkParamAndUnique(cxLossSetting);
        cxLossSetting.setBaseVale(cxLossSetting.getId());
        return cxLossSettingMapper.updateCxLossSetting(cxLossSetting);
    }

    /**
     * 批量删除成型损耗率设定
     *
     * @param ids 需要删除的成型损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteCxLossSettingByIds(Long[] ids) {
        return cxLossSettingMapper.deleteCxLossSettingByIds(ids);
    }

    /**
     * 删除成型损耗率设定信息
     *
     * @param id 成型损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteCxLossSettingById(Long id) {
        return cxLossSettingMapper.deleteCxLossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkCxLossSettingUnique(CxLossSetting cxLossSetting) {
        if (cxLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = cxLossSettingMapper.checkCxLossSettingUnique(cxLossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param cxLossSetting 要检查记录
     */
    private void checkParamAndUnique(CxLossSetting cxLossSetting) {

        if (StringUtils.isAllEmpty(cxLossSetting.getMachineCode(), cxLossSetting.getEmbryoCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkCxLossSettingUnique(cxLossSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxLossSettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxLossSetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();


        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getEmbryoCode()+a.getMachineName()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxLossSettingDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getEmbryoCode()+dto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.loss.embryoCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            // 特殊校验（代码和机台名称不能同时为空校验）
            String machineName = dto.getMachineName();

            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(dto.getEmbryoCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum = failureNum + 1;
                importErrorLogs.addAll(validated);
            } else{
                CxLossSetting newEntity = new CxLossSetting();
                BeanUtils.copyProperties(dto, newEntity);
                newEntity.setMachineCode(dto.getMachineCode());
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
                    cxLossSettingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxLossSettingDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        CxLossSetting newItem = new CxLossSetting();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        String unique = checkCxLossSettingUnique(newItem);
                        if (UserConstants.UNIQUE.equals(unique)) {
                            successNum++;
                            cxLossSettingMapper.insertCxLossSetting(newItem);
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
