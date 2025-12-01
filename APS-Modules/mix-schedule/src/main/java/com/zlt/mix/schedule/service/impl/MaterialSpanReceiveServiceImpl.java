package com.zlt.mix.schedule.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import com.zlt.mix.schedule.engine.mapper.RecipeEngineMapper;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import com.zlt.mix.schedule.mapper.MaterialSpanReceiveMapper;
import com.zlt.mix.schedule.service.MaterialSpanReceiveService;

/**
 * 硫磺辅料跨区接收Service业务层处理
 *
 * @author cxy
 * @date 2022-08-30
 */
@Service
public class MaterialSpanReceiveServiceImpl extends ServiceImpl<MaterialSpanReceiveMapper, MaterialSpanReceive> implements MaterialSpanReceiveService {
    @Resource
    private MaterialSpanReceiveMapper materialSpanReceiveMapper;
    @Resource
    private RecipeEngineMapper recipeEngineMapper;

    /**
     * 校验胶料跨区接收唯一性
     */
    @Override
    public String checkMaterialSpanReceiveUnique(MaterialSpanReceive materialSpanReceive) {
        if (materialSpanReceive == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<MaterialSpanReceive> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MaterialSpanReceive::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(MaterialSpanReceive::getScheduleDate, materialSpanReceive.getScheduleDate());
        queryWrapper.eq(MaterialSpanReceive::getEntrustMixArea, materialSpanReceive.getEntrustMixArea());
        queryWrapper.eq(MaterialSpanReceive::getEntrustedMixArea, materialSpanReceive.getEntrustedMixArea());
        queryWrapper.eq(MaterialSpanReceive::getMaterialName, materialSpanReceive.getMaterialName());
        if (materialSpanReceive.getId() != null) {
            queryWrapper.ne(MaterialSpanReceive::getId, materialSpanReceive.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<MaterialSpanReceive> list = materialSpanReceiveMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 批量新增跨区接收请求记录
     *
     * @param materialSpanReceiveList 要批量保存的记录
     * @return 影响行数
     */
    @Override
    public int batchInsertMaterialSpanReceive(List<MaterialSpanReceive> materialSpanReceiveList) {
        int result = 0;
        if (CollectionUtils.isNotEmpty(materialSpanReceiveList)) {
            /*List<ImportErrorLog> codeUniqueErrorLogs = glueSpanReceiveMapper.listGlueSpanReceiveNotUnique(materialSpanReceiveList);
            Map<Integer, Long> codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            for (int i = 0; i < materialSpanReceiveList.size(); i++) {
                if (codeUniqueErrorMap.containsKey(i)) {
                    throw new RuntimeException(I18nUtil.getMessage("schedule.glueSpanReceive.database.unique"));
                }
            }*/
            result = materialSpanReceiveMapper.batchInsertMaterialSpanReceive(materialSpanReceiveList);
        }
        return result;
    }

    /**
     * 查询跨区接收列表
     *
     * @param entity 参数
     * @return 结果
     */
    @Override
    public List<MaterialSpanReceive> listMaterialSpanReceive(MaterialSpanReceive entity) {
    	List<MaterialSpanReceive> spanList = materialSpanReceiveMapper.listMaterialSpanReceive(entity);
    	String mixArea = entity.getEntrustedMixArea();
    	if (CollectionUtils.isNotEmpty(spanList) && StringUtils.isNotEmpty(mixArea)) {
    		List<String> materialList = spanList.stream().map(MaterialSpanReceive::getMaterialName).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    		if (CollectionUtils.isEmpty(materialList)) {
    			return spanList;
    		}
    		Map<String, List<MesPmtRecipeVo>> recipeMap = recipeEngineMapper.listLhflMachineRecipe(mixArea, materialList)
    				.stream().collect(Collectors.groupingBy(MesPmtRecipeVo::getRecipeMaterialName)); // 查询物料配方机台关系
    		spanList.forEach(span -> {
    			if (StringUtils.isNotEmpty(span.getMachineCode())) { // 如果已经有机台了，则不需要重新赋值
    				return;
    			}
    			String materialName = span.getMaterialName();
    			List<MesPmtRecipeVo> recipeList = recipeMap.get(materialName);
    			if (CollectionUtil.isEmpty(recipeList)) {
    				return;
    			}
    			if (recipeList.stream().map(MesPmtRecipeVo::getRecipeEquipCode).filter(Objects::nonNull).distinct().count() > 1) {
    				return; // 判断是否有多个机台，多个机台则直接返回
    			}
    			
    			MesPmtRecipeVo recipe = CollectionUtil.firstElement(recipeList);
    			if (recipe != null) {
    				span.setRecipeType(recipe.getRecipeType());
    				span.setRecipeTypeName(recipe.getRecipeTypeName());
    				span.setMachineCode(recipe.getRecipeEquipCode());
    				span.setMachineName(recipe.getMachineName());
    				span.setRecipeVersionId(recipe.getRecipeVersionId());
    				span.setRecipeStage(recipe.getProductStage());
    			}
    		});
    	}
        return spanList;
    }

    /**
     * 根据id查询跨区接收信息
     *
     * @param entity id
     * @return 查询到的记录
     */
    @Override
    public MaterialSpanReceive getMaterialSpanReceiveInfo(MaterialSpanReceive entity) {
        return materialSpanReceiveMapper.getMaterialSpanReceiveInfo(entity);
    }

    /**
     * 批量更新跨区接收记录
     *
     * @param receiveList 批量更新的记录
     * @return 影响行数
     */
    @Override
    public int mergeMaterialSpanReceive(List<MaterialSpanReceive> receiveList) {
        return materialSpanReceiveMapper.mergeMaterialSpanReceive(receiveList);
    }

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     *
     * @param materialSpanReceive 参数
     * @return 未接收的总数
     */
    @Override
    public Integer selectUnReceiveCount(MaterialSpanReceive materialSpanReceive) {
        return materialSpanReceiveMapper.selectUnReceiveCount(materialSpanReceive);
    }

    /**
     * 根据sendIds查询已接收的记录数
     *
     * @param sendIds sendIds
     * @return 已接收记录数
     */
    @Override
    public Integer getAlreadyReceivedCount(Long[] sendIds) {
        return materialSpanReceiveMapper.getAlreadyReceivedCount(sendIds);
    }

    /**
     * 根据Id查询已接收的记录数
     *
     * @param ids ids
     * @return 已接收记录数
     */
    @Override
    public Integer getAlreadyReceivedCountByIds(Long[] ids) {
        return materialSpanReceiveMapper.getAlreadyReceivedCountByIds(ids);
    }

    /**
     * 根据send_id删除发送记录
     *
     * @param sendIds sendId
     * @return 结果
     */
    @Override
    public int deleteBySendIds(Long[] sendIds) {
        return materialSpanReceiveMapper.deleteBySendIds(sendIds);
    }

    /**
     *  删除还未接收的跨区接收记录
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     */
    public void deleteNotReceived(String mixArea, Date scheduleDate) {
        materialSpanReceiveMapper.deleteNotReceived(mixArea, scheduleDate);
    }
}
