package com.zlt.aps.gsq.service.impl;


import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqTwiningDiscDto;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.GsqTwiningDiscService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 钢丝圈缠绕盘表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Service
public class GsqTwiningDiscServiceImpl extends ServiceImpl<GsqTwiningDiscMapper, GsqTwiningDisc> implements GsqTwiningDiscService {

    @Resource
    private GsqTwiningDiscMapper gsqTwiningDiscMapper;
    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 根据条件查询缠绕盘列表
     *
     * @return
     */
    public List<GsqTwiningDiscDto> listTwiningDisc(GsqTwiningDiscDto dto) {
        return gsqTwiningDiscMapper.listTwiningDisc(dto);
    }

    /**
     * 保存缠绕盘信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveTwiningDisc(GsqTwiningDisc entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if(entity.getId()!=null){
            gsqTwiningDiscMapper.updateTGsqTwiningDisc(entity);
        }else{
            this.saveOrUpdate(entity);
        }
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteTwiningDisc(Long[] ids) {
        LambdaUpdateWrapper<GsqTwiningDisc> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据code判断缠绕盘代号是否已经存在
     */
    public String checkSerialNumberUnique(GsqTwiningDiscDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getSerialNumber())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<GsqTwiningDisc> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("SERIAL_NUMBER", dto.getSerialNumber());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<GsqTwiningDisc> list = gsqTwiningDiscMapper.selectList(queryWrapper);
        if (list.size() > 0) {
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
    public AjaxResult importData(List<GsqTwiningDiscDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqTwiningDiscDto> importList = new ArrayList<>();
        //将机台名称转为机台code
        List<GsqMachineInfo> machineInfoList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(GsqMachineInfo::getMachineCode, GsqMachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(GsqMachineInfo::getMachineName, GsqMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSerialNumber()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            GsqTwiningDiscDto twiningDiscDto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(twiningDiscDto.getSerialNumber());
            if (hasValue > 1) {
                failureNum++;
                twiningDiscDto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.twiningDisc.column.serialNumber");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, twiningDiscDto);
            String machineName = twiningDiscDto.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            if (machineId == null && StringUtil.isNotBlank(machineName)) {
                // 未查询到对应机台信息
                ImportUtil.addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                // 校验失败
                failureNum++;
                twiningDiscDto.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                // 查询到机台信息，且校验通过
                twiningDiscDto.setMachineId(machineId);
                twiningDiscDto.setBaseVale(null);
                importList.add(twiningDiscDto);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                gsqTwiningDiscMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    GsqTwiningDiscDto excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    String unique = checkSerialNumberUnique(excelItem);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        //不存在插入
                        successNum++;
                        GsqTwiningDisc gsqTwiningDisc = new GsqTwiningDisc();
                        BeanUtils.copyProperties(excelItem, gsqTwiningDisc);
                        saveTwiningDisc(gsqTwiningDisc);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.twiningDisc.alter.isSerialNumberExist"), importErrorLogs);
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
        this.gsqTwiningDiscMapper.deleteAll();
    }
}
