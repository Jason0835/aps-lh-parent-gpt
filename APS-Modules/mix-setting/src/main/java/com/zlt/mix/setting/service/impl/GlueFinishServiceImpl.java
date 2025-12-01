package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.GlueFinish;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.mapper.GlueFinishMapper;
import com.zlt.mix.setting.service.GlueFinishService;
import com.zlt.mix.setting.service.MixMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胶料完成量Service业务层处理
 *
 * @author Gim
 * @date 2022-03-29
 */
@Service
public class GlueFinishServiceImpl extends ServiceImpl<GlueFinishMapper, GlueFinish> implements GlueFinishService {
    @Resource
    private GlueFinishMapper glueFinishMapper;

    @Autowired
    private MixMachineService mixMachineService;
    @Resource
    private ISysDictDataCacheService iSysDictDataCacheService;

    /**
     * 查询炼胶时间信息列表
     *
     * @param glueFinish 炼胶时间信息
     * @return 炼胶时间信息
     */
    @Override
    public List<GlueFinish> selectGlueFinishList(GlueFinish glueFinish) {
        List<GlueFinish> list = glueFinishMapper.selectGlueFinishList(glueFinish);

        return list;
    }

    /**
     * 保存炼胶时间信息信息（id为空则新增，id不为空则修改）
     *
     * @param glueFinish
     */
    @Override
    public void saveGlueFinish(GlueFinish glueFinish) {
        glueFinish.setBaseValue(glueFinish.getId());
        this.saveOrUpdate(glueFinish);
    }


    /**
     * 校验炼胶时间信息唯一性
     */
    @Override
    public String checkGlueFinishUnique(GlueFinish glueFinish) {
        if (glueFinish == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<GlueFinish> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("ORDER_NO", glueFinish.getOrderNo());
        if (glueFinish.getId() != null) {
            queryWrapper.ne("ID", glueFinish.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueFinish> list = glueFinishMapper.selectList(queryWrapper);
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
    public AjaxResult importData(List<GlueFinish> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<GlueFinish> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Integer> importRowMap = new HashMap<>();//通过校验后的数据与在原本的Excel中对应的行数
        List<SysDictData> mixAreaList = iSysDictDataCacheService.getType("MIX_AREA");
        Map<String, String> mixAreaMap = mixAreaList.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (v1, v2) -> v1));


        try {

            // 取机台编号
            List<MixMachine> machineList = mixMachineService.selectMixMachineList(new MixMachine());
            Map<String, MixMachine> machineMap = CollectionUtil.toMap(machineList, obj -> (
                    GenerageMapKeyUtils.createMapKey(obj.getMixArea(), obj.getMachineName())
            ));


            for (int i = 0; i < list.size(); i++) {
                GlueFinish machine = list.get(i);
                // 转换机台编号
                MixMachine mixMachine = machineMap.get(GenerageMapKeyUtils.createMapKey(machine.getMixArea(), machine.getMachineName()));
                if (mixMachine != null) {
                    machine.setMachineCode(mixMachine.getMachineCode());
                } else {
                    machine.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.glueFinish.machineNoExist");
                    addImportErrorLog(importLogId, i + 2, String.format(message, mixAreaMap.getOrDefault(machine.getMixArea(), machine.getMixArea()), machine.getMachineName()), importErrorLogs);
                }
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getOrderNo()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                GlueFinish glueFinish = list.get(i);
                //excel中重复记录校验
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(glueFinish.getOrderNo()));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    glueFinish.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.glueFinish.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueFinish); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && glueFinish.getId() == null) {
                    glueFinish.setBaseValue(null);
                    importRowMap.put(importList.size(), i + 2);
                    importList.add(glueFinish);
                } else {
                    glueFinish.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.glueFinishMapper.listGlueFinishNotUnique(importList, importLogId, I18nUtil.getMessage("setting.glueFinish.database.unique"), SecurityUtils.getUsername());
                //转换对应的错误行数、标记对应的错误记录
                for (ImportErrorLog codeUniqueErrorLog : codeUniqueErrorLogs) {
                    Integer errorRow = codeUniqueErrorLog.getErrorRow();
                    importList.get(errorRow).setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    codeUniqueErrorLog.setErrorRow(importRowMap.get(errorRow));
                }
                importErrorLogs.addAll(codeUniqueErrorLogs);

                // 过滤掉未通过校验的记录
                importList = importList.stream().filter(item -> item.getId() == null || !item.getId().equals(-999L)).collect(Collectors.toList());

            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                glueFinishMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                glueFinishMapper.batchInsertGlueFinishInfo(importList);  //批量插入
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
}
