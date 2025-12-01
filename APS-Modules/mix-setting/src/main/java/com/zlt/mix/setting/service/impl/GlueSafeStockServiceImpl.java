package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.GlueSafeStock;
import com.zlt.mix.setting.mapper.GlueSafeStockMapper;
import com.zlt.mix.setting.service.GlueSafeStockService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 安全库存Service业务层处理
 * 
 * @author Gim
 * @date 2022-03-21
 */
@Service
public class GlueSafeStockServiceImpl extends ServiceImpl<GlueSafeStockMapper, GlueSafeStock> implements GlueSafeStockService {
    @Resource
    private GlueSafeStockMapper glueSafeStockMapper;

    /**
     * 查询安全库存列表
     * 
     * @param glueSafeStock 安全库存
     * @return 安全库存
     */
    @Override
    public List<GlueSafeStock> selectGlueSafeStockList(GlueSafeStock glueSafeStock) {
        return glueSafeStockMapper.selectGlueSafeStockList(glueSafeStock);
    }

    /**
     * 保存安全库存信息（id为空则新增，id不为空则修改）
     *
     * @param glueSafeStock
     */
    @Override
    public void saveGlueSafeStock(GlueSafeStock glueSafeStock) {
        glueSafeStock.setBaseValue(glueSafeStock.getId());
        this.saveOrUpdate(glueSafeStock);
    }

    /**
     * 批量删除安全库存
     * 
     * @param ids 需要删除的安全库存ID
     * @return 结果
     */
    @Override
    public int deleteGlueSafeStockByIds(Long[] ids)
    {
        return glueSafeStockMapper.deleteGlueSafeStockByIds(ids);
    }


    /**
     * 校验安全库存唯一性
     */
    @Override
    public String checkGlueSafeStockUnique(GlueSafeStock glueSafeStock) {
        if (glueSafeStock == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<GlueSafeStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", glueSafeStock.getMixArea());
        queryWrapper.eq("GLUE", glueSafeStock.getGlue());
        if (glueSafeStock.getId() != null) {
            queryWrapper.ne("ID", glueSafeStock.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueSafeStock> list = glueSafeStockMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入安全库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<GlueSafeStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<GlueSafeStock> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {
            if(!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.glueSafeStockMapper.listGlueSafeStockNotUnique(list, importLogId, I18nUtil.getMessage("setting.safeStock.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getGlue()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                GlueSafeStock glueSafeStock = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(glueSafeStock.getMixArea(), glueSafeStock.getGlue()));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    glueSafeStock.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.safeStock.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if(codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    glueSafeStock.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueSafeStock); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && glueSafeStock.getId() == null) {
                    glueSafeStock.setBaseValue(null);
                    importList.add(glueSafeStock);
                } else {
                    glueSafeStock.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                glueSafeStockMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if(!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                glueSafeStockMapper.batchInsertGlueSafeStockInfo(importList);  //批量插入
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

    @Override
    public BigDecimal selectGlueSafeStock(String mixArea, String glue) {
        LambdaQueryWrapper<GlueSafeStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GlueSafeStock::getMixArea, mixArea);
        wrapper.eq(GlueSafeStock::getGlue, glue);
        wrapper.eq(ZltBaseEntity::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        GlueSafeStock glueSafeStock = glueSafeStockMapper.selectOne(wrapper);
        if (glueSafeStock == null) {
            //return BigDecimal.ZERO;
            return null;
        }
        return glueSafeStock.getSafeStock();
    }

    @Override
    public void saveOrUpdateGlueSafeStock(String mixArea, String glue, BigDecimal safeStock) {
        LambdaQueryWrapper<GlueSafeStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GlueSafeStock::getMixArea, mixArea);
        wrapper.eq(GlueSafeStock::getGlue, glue);
        wrapper.eq(ZltBaseEntity::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        GlueSafeStock glueSafeStock = glueSafeStockMapper.selectOne(wrapper);
        if (glueSafeStock == null) {
            glueSafeStock = new GlueSafeStock();
            glueSafeStock.setMixArea(mixArea);
            glueSafeStock.setGlue(glue);
        }
        glueSafeStock.setSafeStock(safeStock);
        saveOrUpdate(glueSafeStock);
    }

    /**
     * 根据密炼区和胶料名称更改安全库存
     */
    @Override
    public void updateSafeStockByMixAreaAndGlue(GlueSafeStock glueSafeStock) {
        glueSafeStockMapper.updateSafeStockByMixAreaAndGlue(glueSafeStock);
    }
}
