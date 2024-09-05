package com.zlt.aps.tq.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.dto.TqMouthPlateDto;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.entity.TqMouthPlate;
import com.zlt.aps.tq.mapper.TqMouthPlateMapper;
import com.zlt.aps.tq.service.TqMachineInfoService;
import com.zlt.aps.tq.service.TqMouthPlateService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 胎圈口型板信息维护 服务实现类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-08
 */
@Service
public class TqMouthPlateServiceImpl extends ServiceImpl<TqMouthPlateMapper, TqMouthPlate> implements TqMouthPlateService {

    @Autowired
    private TqMouthPlateMapper mouthPlateMapper;
    @Autowired
    private TqMachineInfoService tqMachineInfoService;

    /**
     * 查询胎圈口型板信息维护列表
     *
     * @param mouthPlate 胎圈口型板信息维护
     * @return 胎圈口型板信息维护集合
     */
    @Override
    public List<TqMouthPlateDto> selectMouthPlateList(TqMouthPlate mouthPlate) {
        return mouthPlateMapper.selectMouthPlateWithMachineInfo(mouthPlate);
    }

    /**
     * 查询胎圈口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 胎圈口型板信息维护集合
     */
    @Override
    public TqMouthPlate selectMouthPlateById(Long id) {
        LambdaQueryWrapper<TqMouthPlate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqMouthPlate::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(TqMouthPlate::getId, id);
        return mouthPlateMapper.selectOne(wrapper);
    }

    /**
     * 新增或更新胎圈口型板信息维护
     *
     * @param mouthPlate 胎圈口型板信息维护
     */
    @Override
    public void saveMouthPlate(TqMouthPlate mouthPlate) {
        if (ObjectUtils.allNotNull(mouthPlate.getMouthPlateCode(), mouthPlate.getMachineId()) && mouthPlateMapper.checkUnique(mouthPlate) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.unique"));
        }
        mouthPlate.setBaseVale(mouthPlate.getId());
        saveOrUpdate(mouthPlate);
    }

    /**
     * 批量删除胎圈口型板信息维护
     *
     * @param ids 需要删除的胎圈口型板信息维护ID
     */
    @Override
    public void deleteMouthPlateByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        List<TqMouthPlate> list = new ArrayList<>();
        for (Long id : ids) {
            TqMouthPlate mouthPlate = new TqMouthPlate();
            mouthPlate.setId(id);
            mouthPlate.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            mouthPlate.setBaseVale(mouthPlate.getId());
            list.add(mouthPlate);
        }
        updateBatchById(list);
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
    public AjaxResult importData(List<TqMouthPlateDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;

        List<TqMouthPlateDto> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        //将机台名称转换为机台id，并做校验
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TqMachineInfo::getMachineCode, TqMachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getMouthPlateCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqMouthPlateDto mouthPlate = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(mouthPlate.getMouthPlateCode()+mouthPlate.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                mouthPlate.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.mouthPlateCode");
                String columnName2 = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            // 校验
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, mouthPlate);
            String machineName = mouthPlate.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            if (machineId == null && StringUtil.isNotBlank(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                // 设置导入默认状态，机台id，更新人，更新时间
                mouthPlate.setMachineId(machineId);
                mouthPlate.setBaseVale(null);
                importList.add(mouthPlate);
            } else {
                failureNum++;
                mouthPlate.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                mouthPlateMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TqMouthPlateDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    TqMouthPlate mouthPlate = new TqMouthPlate();
                    BeanUtils.copyProperties(excelItem, mouthPlate);
                    int unique = mouthPlateMapper.checkUnique(mouthPlate);
                    if (unique == 0) {
                        //不存在插入
                        successNum++;
                        mouthPlateMapper.insert(mouthPlate);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.mouthPlate.message.unique"), importErrorLogs);
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
        this.mouthPlateMapper.deleteAll();
    }

}
