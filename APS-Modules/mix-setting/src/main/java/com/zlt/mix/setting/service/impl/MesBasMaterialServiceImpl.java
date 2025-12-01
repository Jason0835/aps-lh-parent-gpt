package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import com.zlt.mix.setting.mapper.MesBasMaterialMapper;
import com.zlt.mix.setting.service.MesBasMaterialService;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 物料Service业务层处理
 *
 * @author Joran.zhang
 * @date 2022-05-30
 */
@Service
public class MesBasMaterialServiceImpl extends ServiceImpl<MesBasMaterialMapper, MesBasMaterial> implements MesBasMaterialService {
    @Resource
    private MesBasMaterialMapper mesBasMaterialMapper;

    /**
     * 查询物料列表
     *
     * @param mesBasMaterial 物料
     * @return 物料
     */
    @Override
    public List<MesBasMaterial> selectMesBasMaterialList(MesBasMaterial mesBasMaterial) {
        return mesBasMaterialMapper.selectMesBasMaterialList(mesBasMaterial);
    }

    /**
     * 保存物料信息（id为空则新增，id不为空则修改）
     *
     * @param mesBasMaterial
     */
    @Override
    public void saveMesBasMaterial(MesBasMaterial mesBasMaterial) {
        if (ZltConstant.NOT_UNIQUE.equals(checkMesBasMaterialUnique(mesBasMaterial))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.material.database.unique" ));
        }
        mesBasMaterial.setBaseValue(mesBasMaterial.getId());
        this.saveOrUpdate(mesBasMaterial);
    }

    /**
     * 批量删除物料
     *
     * @param ids 需要删除的物料ID
     * @return 结果
     */
    @Override
    public int deleteMesBasMaterialByIds(Long[] ids)
    {
        return mesBasMaterialMapper.deleteMesBasMaterialByIds(ids);
    }


    /**
     * 校验物料唯一性
     */
    @Override
    public String checkMesBasMaterialUnique(MesBasMaterial mesBasMaterial) {
        if (mesBasMaterial == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<MesBasMaterial> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MATERIAL_CODE", mesBasMaterial.getMaterialCode());
        if (mesBasMaterial.getId() != null) {
            queryWrapper.ne("ID", mesBasMaterial.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<MesBasMaterial> list = mesBasMaterialMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入物料数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MesBasMaterial> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MesBasMaterial> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {
            if(!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.mesBasMaterialMapper.listMesBasMaterialNotUnique(list, importLogId, I18nUtil.getMessage("setting.material.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMaterialCode()+"", Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                MesBasMaterial mesBasMaterial = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupMap.get(mesBasMaterial.getMaterialCode()+"");
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    mesBasMaterial.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.material.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if(codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    mesBasMaterial.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, i + 2, mesBasMaterial); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && mesBasMaterial.getId() == null) {
                    mesBasMaterial.setBaseValue(null);
                    importList.add(mesBasMaterial);
                } else {
                    mesBasMaterial.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                for (List<MesBasMaterial> itemList : ListUtils.partition(importList, 500)) {
                    mesBasMaterialMapper.mergeSql(itemList);  //根据唯一键批量新增或修改
                }
            } else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                for (List<MesBasMaterial> itemList : ListUtils.partition(importList, 500)) {
                    mesBasMaterialMapper.batchInsertMesBasMaterialInfo(itemList);  //批量插入
                }
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
     * 根据物料大类列表查询物料名称列表
     *
     * @param majorTypes 物料大类列表
     * @return 物料名称列表
     */
    @Override
    public List<String> listMesBasMaterial(List<Integer> majorTypes) {
        List<String> list = mesBasMaterialMapper.listMesBasMaterial(majorTypes);
        return list;
    }
}
