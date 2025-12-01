package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.dto.MachineGlueDecomposeDto;
import com.zlt.mix.setting.api.domain.entity.MachineGlueDecompose;
import com.zlt.mix.setting.mapper.MachineGlueDecomposeMapper;
import com.zlt.mix.setting.service.FormulaMachineService;
import com.zlt.mix.setting.service.MachineGlueDecomposeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 密炼机指定胶料分解Service业务层处理
 *
 * @author Liam
 * @date 2022-03-29
 */
@Service
public class MachineGlueDecomposeServiceImpl extends ServiceImpl<MachineGlueDecomposeMapper, MachineGlueDecompose> implements MachineGlueDecomposeService {
    @Resource
    private MachineGlueDecomposeMapper machineGlueDecomposeMapper;


    /**
     * 保存密炼机指定胶料分解信息（id为空则新增，id不为空则修改）
     *
     * @param machineGlueDecompose
     */
    @Override
    public void saveMachineGlueDecompose(MachineGlueDecompose machineGlueDecompose) {
        //校验数据唯一
        //校验机台信息是否存在（此处不需要，前端进行数据给与，保证存在）
        if (ZltConstant.NOT_UNIQUE.equals(checkMachineGlueDecomposeUnique(machineGlueDecompose))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.machineGlueDecompose.database.unique"));
        }
        machineGlueDecompose.setBaseValue(machineGlueDecompose.getId());
        this.saveOrUpdate(machineGlueDecompose);
    }

    /**
     * 批量删除密炼机指定胶料分解
     *
     * @param ids 需要删除的密炼机指定胶料分解ID
     * @return 结果
     */
    @Override
    public int deleteMachineGlueDecomposeByIds(Long[] ids) {
        return machineGlueDecomposeMapper.deleteMachineGlueDecomposeByIds(ids);
    }


    /**
     * 校验密炼机指定胶料分解唯一性
     */
    @Override
    public String checkMachineGlueDecomposeUnique(MachineGlueDecompose machineGlueDecompose) {
        if (machineGlueDecompose == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<MachineGlueDecompose> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", machineGlueDecompose.getMixArea());
        queryWrapper.eq("MACHINE_CODE", machineGlueDecompose.getMachineCode());
        queryWrapper.eq("GLUE", machineGlueDecompose.getGlue());
        if (machineGlueDecompose.getId() != null) {
            queryWrapper.ne("ID", machineGlueDecompose.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<MachineGlueDecompose> list = machineGlueDecomposeMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入密炼机指定胶料分解数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MachineGlueDecomposeDto> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MachineGlueDecomposeDto> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {

            //通过左连接，批量查询机台编号
            List<String> machineCodeList = machineGlueDecomposeMapper.selectMachineCodeList(list);

            //先判断对应密炼机台的信息是否存在
            for (int i = 0; i < machineCodeList.size(); i++) {
                MachineGlueDecomposeDto dto = list.get(i);
                String machineCode = machineCodeList.get(i);
                if (StringUtils.isEmpty(machineCode)) {
                    dto.setId(-999L);
                    String message = I18nUtil.getMessage("setting.machineGlueDecompose.machine.exists");
                    addImportErrorLog(importLogId, i + 2, String.format(message, dto.getMixArea(), dto.getGlue(), dto.getMachineName()), importErrorLogs);
                    //为了兼容性（可以同时输入机台编号）,将可能存在的旧数据设置为null
                    dto.setMachineCode("");
                } else {
                    dto.setMachineCode(machineCode);
                }
            }


            if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.machineGlueDecomposeMapper.listMachineGlueDecomposeNotUnique(list, importLogId, I18nUtil.getMessage("setting.machineGlueDecompose.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getMachineCode(), a.getGlue()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                MachineGlueDecomposeDto machineGlueDecompose = list.get(i);

                //exce中重复记录校验
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(machineGlueDecompose.getMixArea(), machineGlueDecompose.getMachineCode(), machineGlueDecompose.getGlue()));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    machineGlueDecompose.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.machineGlueDecompose.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if (codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    machineGlueDecompose.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineGlueDecompose); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && machineGlueDecompose.getId() == null) {
                    machineGlueDecompose.setBaseValue(null);
                    importList.add(machineGlueDecompose);
                } else {
                    machineGlueDecompose.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                machineGlueDecomposeMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                machineGlueDecomposeMapper.batchInsertMachineGlueDecomposeInfo(importList);  //批量插入
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
     * 查询密炼机指定胶料分解列表(级联查询机台名称)
     *
     * @param machineGlueDecompose 密炼机指定胶料分解
     * @return 密炼机指定胶料分解Dto列表
     */
    @Override
    public List<MachineGlueDecomposeDto> selectMachineGlueDecomposeListCascade(MachineGlueDecompose machineGlueDecompose) {
        return machineGlueDecomposeMapper.selectMachineGlueDecomposeListCascade(machineGlueDecompose);
    }

    /**
     * 获取密炼机指定胶料分解详细信息(级联查询机台名称)
     *
     * @param id 密炼机指定胶料分解ID
     * @return 密炼机指定胶料分解Dto
     */
    @Override
    public MachineGlueDecomposeDto getByIdCascade(Long id) {
        return machineGlueDecomposeMapper.getByIdCascade(id);
    }

}
