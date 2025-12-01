package com.zlt.mix.setting.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import javax.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import com.zlt.mix.setting.mapper.RecipeTypeMapper;
import com.zlt.mix.setting.service.RecipeTypeService;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 配方类型Service业务层处理
 *
 * @author Joran.zhang
 * @date 2022-05-31
 */
@Service
public class RecipeTypeServiceImpl extends ServiceImpl<RecipeTypeMapper, RecipeType> implements RecipeTypeService {
    @Resource
    private RecipeTypeMapper recipeTypeMapper;

    /**
     * 查询配方类型列表
     *
     * @param recipeType 配方类型
     * @return 配方类型
     */
    @Override
    public List<RecipeType> selectRecipeTypeList(RecipeType recipeType) {
        return recipeTypeMapper.selectRecipeTypeList(recipeType);
    }

    /**
     * 保存配方类型信息（id为空则新增，id不为空则修改）
     *
     * @param recipeType
     */
    @Override
    public void saveRecipeType(RecipeType recipeType) {
        if (ZltConstant.NOT_UNIQUE.equals(checkRecipeTypeUnique(recipeType))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.type.database.unique" ));
        }
        recipeType.setBaseValue(recipeType.getId());
        this.saveOrUpdate(recipeType);
    }

    /**
     * 批量删除配方类型
     *
     * @param ids 需要删除的配方类型ID
     * @return 结果
     */
    @Override
    public int deleteRecipeTypeByIds(Long[] ids)
    {
        return recipeTypeMapper.deleteRecipeTypeByIds(ids);
    }


    /**
     * 校验配方类型唯一性
     */
    @Override
    public String checkRecipeTypeUnique(RecipeType recipeType) {
        if (recipeType == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<RecipeType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        if(StringUtils.isNotEmpty(recipeType.getRecipeTypeName())){
            queryWrapper.eq("RECIPE_TYPE_NAME", recipeType.getRecipeTypeName());
        }
        if(recipeType.getRecipeTypeCode()!=null){
            queryWrapper.eq("RECIPE_TYPE_CODE", recipeType.getRecipeTypeCode());
        }

        if (recipeType.getId() != null) {
            queryWrapper.ne("ID", recipeType.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<RecipeType> list = recipeTypeMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入配方类型数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<RecipeType> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<RecipeType> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {
            if(!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.recipeTypeMapper.listRecipeTypeNotUnique(list, importLogId, I18nUtil.getMessage("setting.type.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }else if(updateSupport&&CollectionUtils.isNotEmpty(list)){//先进行名称重复校验卡控
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.recipeTypeMapper.listRecipeTypeNameNotUnique(list, importLogId, I18nUtil.getMessage("setting.type.database.recipeTypeName.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupCodeMap = list.stream().collect(Collectors.groupingBy(a ->a.getRecipeTypeCode(), Collectors.counting()));
            Map<String, Long> groupNameMap = list.stream().collect(Collectors.groupingBy(a ->a.getRecipeTypeName(), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                RecipeType recipeType = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupCodeMap.get(recipeType.getRecipeTypeCode());
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    recipeType.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.type.excel.recipeTypeCode.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                if (hasValue<=1) {
                    Long hasNameValue = groupNameMap.get(recipeType.getRecipeTypeName());
                    if(hasNameValue>1){
                        //导入的excel中的数据违反了唯一键约束
                        recipeType.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                        String message = I18nUtil.getMessage("setting.type.excel.recipeTypeName.unique");
                        addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                    }

                }

                //违反数据库唯一键的记录
                if(codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    recipeType.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, recipeType); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && recipeType.getId() == null) {
                    recipeType.setBaseValue(null);
                    importList.add(recipeType);
                } else {
                    recipeType.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                recipeTypeMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if(!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                recipeTypeMapper.batchInsertRecipeTypeInfo(importList);  //批量插入
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
