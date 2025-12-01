package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.setting.api.domain.entity.*;
import com.zlt.mix.setting.api.domain.vo.MesPmtRecipeTemplateVo;
import com.zlt.mix.setting.mapper.*;
import com.zlt.mix.setting.service.MesPmtRecipeService;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 配方信息Service业务层处理
 *
 * @author chen
 * @date 2022-06-01
 */
@Service
public class MesPmtRecipeServiceImpl extends ServiceImpl<MesPmtRecipeMapper, MesPmtRecipe> implements MesPmtRecipeService {
    @Resource
    private MesPmtRecipeMapper mesPmtRecipeMapper;
    @Resource
    private MesPmtRecipeWeightMapper mesPmtRecipeWeightMapper;
    @Resource
    private MesBasMaterialMapper mesBasMaterialMapper;
    @Resource
    private MixMachineMapper mixMachineMapper;
    @Resource
    private RecipeTypeMapper recipeTypeMapper;

    /**
     * 查询配方信息列表
     *
     * @param mesPmtRecipe 配方信息
     * @return 配方信息
     */
    @Override
    public List<MesPmtRecipe> selectMesPmtRecipeList(MesPmtRecipe mesPmtRecipe) {
        return mesPmtRecipeMapper.selectMesPmtRecipeList(mesPmtRecipe);
    }

    /**
     * 根据机台名称和胶料名称查询配方信息
     *
     * @param mesPmtRecipe 机台名称和胶料名称
     * @return 配方集合
     */
    @Override
    public List<MesPmtRecipe> selectMesPmtRecipeByParams(MesPmtRecipe mesPmtRecipe) {
        return mesPmtRecipeMapper.selectMesPmtRecipeByParams(mesPmtRecipe);
    }

    /**
     * 根据密炼区、胶料名称，查询对应配方的机台信息
     *
     * @param mesPmtRecipe 密炼区、胶料名称
     * @return 对应配方的机台信息
     */
    @Override
    public ArrayList<MesPmtRecipe> selectMesPmtRecipeMachine(MesPmtRecipe mesPmtRecipe) {
        return mesPmtRecipeMapper.selectMesPmtRecipeMachine(mesPmtRecipe);
    }

    /**
     * 导入配方数据
     */
    @Override
    public AjaxResult importData(List<MesPmtRecipeTemplateVo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MesPmtRecipeTemplateVo> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        Function<MesPmtRecipeTemplateVo, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getRecipeId(), String.valueOf(v.getWeightOrder()));
        List<String> recipeIdList = list.stream().map(MesPmtRecipeTemplateVo::getRecipeId).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        // 查询对应配方信息、称重信息
        Map<String, MesPmtRecipe> recipeMap = new HashMap<>();
        Map<String, MesPmtRecipeWeight> weightMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(recipeIdList)) {
            // 查询对应配方信息
            LambdaQueryWrapper<MesPmtRecipe> recipeWrapper = Wrappers.lambdaQuery(MesPmtRecipe.class);
            recipeWrapper.in(MesPmtRecipe::getRecipeId, recipeIdList);
            recipeWrapper.eq(MesPmtRecipe::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
            recipeMap = mesPmtRecipeMapper.selectList(recipeWrapper)
                    .stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getRecipeId()), Function.identity(), (v1, v2) -> v1));
            // 查询对应称重信息
            LambdaQueryWrapper<MesPmtRecipeWeight> weightWrapper = Wrappers.lambdaQuery(MesPmtRecipeWeight.class);
            weightWrapper.in(MesPmtRecipeWeight::getFatherRecipeId, recipeIdList);
            weightWrapper.eq(MesPmtRecipeWeight::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
            weightMap = mesPmtRecipeWeightMapper.selectList(weightWrapper)
                    .stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFatherRecipeId(), String.valueOf(v.getWeightOrder())), Function.identity(), (v1, v2) -> v1));
        }
        // 查询物料信息、查询机台信息、查询配方版本号
        Map<String, MesBasMaterial> materialMap = new HashMap<>();
        Set<String> machineSet = new HashSet<>();
        Set<String> recipeTypeSet = new HashSet<>();
        List<String> materialList = list.stream().flatMap(v -> Stream.of(v.getRecipeMaterialCode(), v.getRecipeMaterialCodeSub())).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(materialList)) {
            materialMap = mesBasMaterialMapper.selectList(Wrappers.lambdaQuery(MesBasMaterial.class)
                            .eq(MesBasMaterial::getDelFlag, ZltConstant.DEL_FLAG_NORMAL)
                            .in(MesBasMaterial::getMaterialCode, materialList))
                    .stream().collect(Collectors.toMap(MesBasMaterial::getMaterialCode, Function.identity(), (v1, v2) -> v1));
        }
        List<String> machineList = list.stream().map(MesPmtRecipeTemplateVo::getRecipeEquipCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(machineList)) {
            machineSet = mixMachineMapper.selectList(Wrappers.lambdaQuery(MixMachine.class)
                            .eq(MixMachine::getMixArea, ZltConstant.DEFAULT_MIX_AREA)
                            .eq(MixMachine::getDelFlag, ZltConstant.DEL_FLAG_NORMAL)
                            .in(MixMachine::getMachineCode, machineList))
                    .stream().map(MixMachine::getMachineCode).collect(Collectors.toSet());
        }
        List<String> recipeTypeList = list.stream().map(MesPmtRecipeTemplateVo::getRecipeType).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(recipeTypeList)) {
            recipeTypeSet = recipeTypeMapper.selectList(Wrappers.lambdaQuery(RecipeType.class)
                            .eq(RecipeType::getDelFlag, ZltConstant.DEL_FLAG_NORMAL)
                            .in(RecipeType::getRecipeTypeCode, recipeTypeList))
                    .stream().map(RecipeType::getRecipeTypeCode).collect(Collectors.toSet());
        }

        // 国际化错误提示
        String dataUnique = I18nUtil.getMessage("setting.MesPmtRecipe.database.unique");
        String excelUnique = I18nUtil.getMessage("setting.MesPmtRecipe.excel.unique");
        String materialNotExist = I18nUtil.getMessage("setting.MesPmtRecipe.material.notExist");
        String materialSubNotExist = I18nUtil.getMessage("setting.MesPmtRecipe.materialSub.notExist");
        String machineNotExist = I18nUtil.getMessage("setting.MesPmtRecipe.machine.notExist");
        String recipeTypeNotExist = I18nUtil.getMessage("setting.MesPmtRecipe.recipeType.notExist");

        try {
            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(keyFunc, Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                MesPmtRecipeTemplateVo item = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupMap.get(keyFunc.apply(item));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    item.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    addImportErrorLog(importLogId, i + 2, excelUnique, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if (!updateSupport && weightMap.containsKey(GenerageMapKeyUtils.createMapKey(item.getRecipeId(), String.valueOf(item.getWeightOrder())))) {
                    item.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    addImportErrorLog(importLogId, i + 2, dataUnique, importErrorLogs);
                }

                // 校验物料代号和称重物料代号
                if (!materialMap.containsKey(item.getRecipeMaterialCode())) {
                    item.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    addImportErrorLog(importLogId, i + 2, materialNotExist, importErrorLogs);
                } else {
                    MesBasMaterial basMaterial = materialMap.get(item.getRecipeMaterialCode());
                    item.setRecipeMaterialName(basMaterial.getMaterialName());
                }
                if (!materialMap.containsKey(item.getRecipeMaterialCodeSub())) {
                    item.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    addImportErrorLog(importLogId, i + 2, materialSubNotExist, importErrorLogs);
                } else {
                    MesBasMaterial basMaterial = materialMap.get(item.getRecipeMaterialCodeSub());
                    item.setRecipeMaterialNameSub(basMaterial.getMaterialName());
                }

                // 校验机台信息
                if (!machineSet.contains(item.getRecipeEquipCode())) {
                    item.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    addImportErrorLog(importLogId, i + 2, machineNotExist, importErrorLogs);
                }

                // 校验配方类型
                if (!recipeTypeSet.contains(item.getRecipeType())) {
                    item.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    addImportErrorLog(importLogId, i + 2, recipeTypeNotExist, importErrorLogs);
                }

                List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, i + 2, item); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && item.getId() == null) {
                    item.setBaseValue(null);
                    // 密炼区默认M2
                    item.setMixArea(ZltConstant.DEFAULT_MIX_AREA);
                    importList.add(item);
                } else {
                    item.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            // 拆分为配方信息和配方明细的记录
            if (CollectionUtils.isNotEmpty(importList)) {
                buildInsertBatch(importList, recipeMap, weightMap);
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
     * 构建配方信息和配方称重信息，批量插入
     */
    private void buildInsertBatch(List<MesPmtRecipeTemplateVo> importList, Map<String, MesPmtRecipe> recipeMap, Map<String, MesPmtRecipeWeight> weightMap) {
        // 配方信息，配方编号唯一
        Map<String, MesPmtRecipe> resultRecipeMap = new HashMap<>();
        // 称重信息
        List<MesPmtRecipeWeight> weightList = new ArrayList<>();
        for (MesPmtRecipeTemplateVo itemVo : importList) {
            String recipeId = itemVo.getRecipeId();
            if (!resultRecipeMap.containsKey(recipeId)) {
                // 配方记录仅添加一条
                MesPmtRecipe mesPmtRecipe = new MesPmtRecipe();
                BeanUtils.copyProperties(itemVo, mesPmtRecipe);
                MesPmtRecipe historyRecipe = recipeMap.get(GenerageMapKeyUtils.createMapKey(itemVo.getRecipeId()));
                if (historyRecipe != null) {
                    mesPmtRecipe.setId(historyRecipe.getId());
                }
                resultRecipeMap.put(recipeId, mesPmtRecipe);
            }

            // 构建明细记录
            MesPmtRecipeWeight recipeWeight = new MesPmtRecipeWeight();
            recipeWeight.setFatherRecipeId(recipeId);
            recipeWeight.setRecipeId(recipeId);
            recipeWeight.setWeightOrder(itemVo.getWeightOrder());
            recipeWeight.setRecipeMaterialCode(itemVo.getRecipeMaterialCodeSub());
            recipeWeight.setRecipeMaterialName(itemVo.getRecipeMaterialNameSub());
            recipeWeight.setSetWeight(itemVo.getSetWeight());
            recipeWeight.setAllowError(itemVo.getAllowError());
            // MesPmtRecipeWeight historyWeight = weightMap.get(GenerageMapKeyUtils.createMapKey(recipeId, String.valueOf(itemVo.getWeightOrder())));
            // if (historyWeight != null) {
            //     recipeWeight.setId(historyWeight.getId());
            // }
            recipeWeight.setBaseValue(null);
            weightList.add(recipeWeight);
        }

        Collection<MesPmtRecipe> recipeList = resultRecipeMap.values();
        if (CollectionUtils.isNotEmpty(recipeList)) {
            for (List<MesPmtRecipe> itemList : ListUtils.partition(new ArrayList<>(recipeList), 500)) {
                mesPmtRecipeMapper.insertBatchById(itemList);
            }
        }

        if (CollectionUtils.isNotEmpty(weightList)) {
            // 称重信息需要先删除历史数据
            List<String> fatherRecipeIdList = weightList.stream().map(MesPmtRecipeWeight::getFatherRecipeId).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(fatherRecipeIdList)) {
                mesPmtRecipeWeightMapper.delete(Wrappers.lambdaQuery(MesPmtRecipeWeight.class)
                        .eq(MesPmtRecipeWeight::getDelFlag, ZltConstant.DEL_FLAG_NORMAL)
                        .in(MesPmtRecipeWeight::getFatherRecipeId, fatherRecipeIdList));
            }
            for (List<MesPmtRecipeWeight> itemList : ListUtils.partition(weightList, 500)) {
                mesPmtRecipeWeightMapper.insertBatch(itemList);
            }
        }
    }
}
