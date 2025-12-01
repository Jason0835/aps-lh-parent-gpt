package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.dto.MixingTimeDto;
import com.zlt.mix.setting.api.domain.entity.MixingTime;
import com.zlt.mix.setting.mapper.MixingTimeMapper;
import com.zlt.mix.setting.service.MixingTimeService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 炼胶时间信息Service业务层处理
 *
 * @author Liam
 * @date 2022-03-31
 */
@Service
public class MixingTimeServiceImpl extends ServiceImpl<MixingTimeMapper, MixingTime> implements MixingTimeService {
    @Resource
    private MixingTimeMapper mixingTimeMapper;

    /**
     * 查询炼胶时间信息列表
     *
     * @param mixingTime 炼胶时间信息
     * @return 炼胶时间信息
     */
    @Override
    public List<MixingTimeDto> selectMixingTimeList(MixingTime mixingTime) {
        //拼接机台表获取机台名称,同时也要返回机台编号
        return mixingTimeMapper.selectMixingTimeList(mixingTime);
    }

    /**
     * 保存炼胶时间信息信息（id为空则新增，id不为空则修改）
     *
     * @param mixingTime
     */
    @Override
    public void saveMixingTime(MixingTime mixingTime) {
        if (ZltConstant.NOT_UNIQUE.equals(checkMixingTimeUnique(mixingTime))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.mixingTime.database.unique"));
        }
        mixingTime.setBaseValue(mixingTime.getId());
        this.saveOrUpdate(mixingTime);
    }

    /**
     * 批量删除炼胶时间信息
     *
     * @param ids 需要删除的炼胶时间信息ID
     * @return 结果
     */
    @Override
    public int deleteMixingTimeByIds(Long[] ids) {
        return mixingTimeMapper.deleteMixingTimeByIds(ids);
    }


    /**
     * 校验炼胶时间信息唯一性
     */
    @Override
    public String checkMixingTimeUnique(MixingTime mixingTime) {
        if (mixingTime == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<MixingTime> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", mixingTime.getMixArea());
        queryWrapper.eq("GLUE", mixingTime.getGlue());
        if (!StringUtils.isEmpty(mixingTime.getMachineCode())) {
            queryWrapper.eq("MACHINE_CODE", mixingTime.getMachineCode());
        } else {
            queryWrapper.isNull("MACHINE_CODE");
        }
        if (mixingTime.getId() != null) {
            queryWrapper.ne("ID", mixingTime.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<MixingTime> list = mixingTimeMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入炼胶时间信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<MixingTimeDto> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixingTimeDto> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {

            //需要在插入之前通过机台名称和密炼区获取机台编号
            List<String> machineCodes = this.mixingTimeMapper.listMixMachineCode(list);
            //设置机台编号
            for (int i = 0; i < machineCodes.size(); i++) {
                String machineCode = machineCodes.get(i);
                MixingTimeDto mixingTimeDto = list.get(i);
                //机台编号不存在，且机台名称存在
                if (StringUtils.isEmpty(machineCode) && !StringUtils.isEmpty(mixingTimeDto.getMachineName())) {
                    mixingTimeDto.setId(-999L);
                    String message = I18nUtil.getMessage("setting.mixingTime.machine.exists");
                    addImportErrorLog(importLogId, i + 2, String.format(message, mixingTimeDto.getMixArea(), mixingTimeDto.getMachineName()), importErrorLogs);
                }
                mixingTimeDto.setMachineCode(machineCode);
            }


            if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.mixingTimeMapper.listMixingTimeNotUnique(list, importLogId, I18nUtil.getMessage("setting.mixingTime.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }



            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getGlue(), a.getMachineName()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                MixingTimeDto mixingTimeDto = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(mixingTimeDto.getMixArea(), mixingTimeDto.getGlue(), mixingTimeDto.getMachineName()));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    mixingTimeDto.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.mixingTime.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if (codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    mixingTimeDto.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, mixingTimeDto); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && mixingTimeDto.getId() == null) {
                    mixingTimeDto.setBaseValue(null);
                    importList.add(mixingTimeDto);
                } else {
                    mixingTimeDto.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                mergeSqlByList(importList); //根据唯一键批量新增或修改
            } else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                mixingTimeMapper.batchInsertMixingTimeInfo(importList);  //批量插入
            }
        } catch (Exception e) {
            log.error("导入出错", e);
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = importList.size();  //成功记录数
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 有则更新，无则插入
     */
    private void mergeSqlByList(List<MixingTimeDto> importList) {
        List<String> mixAreaList = importList.stream().map(MixingTimeDto::getMixArea).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> glueList = importList.stream().map(MixingTimeDto::getGlue).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> machineCodeList = importList.stream().map(MixingTimeDto::getMachineCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        LambdaQueryWrapper<MixingTime> wrapper = Wrappers.lambdaQuery(MixingTime.class)
                .in(CollectionUtils.isNotEmpty(mixAreaList), MixingTime::getMixArea, mixAreaList)
                .in(CollectionUtils.isNotEmpty(glueList), MixingTime::getGlue, glueList)
                .in(CollectionUtils.isNotEmpty(machineCodeList), MixingTime::getMachineCode, machineCodeList)
                .eq(MixingTime::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        List<MixingTime> mixingTimeList = mixingTimeMapper.selectList(wrapper);
        Map<String, Long> oldMap = mixingTimeList.stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getMixArea(), v.getGlue(), v.getMachineCode()), MixingTime::getId, (v1, v2) -> v1));

        List<MixingTime> updateList = new ArrayList<>();
        List<MixingTime> insertList = new ArrayList<>();
        for (MixingTimeDto mixingTimeDto : importList) {
            Long id = oldMap.get(GenerageMapKeyUtils.createMapKey(mixingTimeDto.getMixArea(), mixingTimeDto.getGlue(), mixingTimeDto.getMachineCode()));
            MixingTime mixingTime = new MixingTime();
            BeanUtils.copyProperties(mixingTimeDto, mixingTime);
            if (id != null) {
                mixingTime.setId(id);
                updateList.add(mixingTime);
            } else {
                insertList.add(mixingTime);
            }
        }

        if (CollectionUtils.isNotEmpty(updateList)) {
            this.updateBatchById(updateList);
        }
        if (CollectionUtils.isNotEmpty(insertList)) {
            this.saveBatch(insertList);
        }
    }
}
