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
import com.zlt.mix.setting.api.domain.dto.GlueStockDto;
import com.zlt.mix.setting.api.domain.entity.GlueStock;
import com.zlt.mix.setting.mapper.GlueStockMapper;
import com.zlt.mix.setting.service.GlueStockService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 终炼胶库存信息Service业务层处理
 *
 * @author Gim
 * @date 2022-03-18
 */
@Service
public class GlueStockServiceImpl extends ServiceImpl<GlueStockMapper, GlueStock> implements GlueStockService {
    @Resource
    private GlueStockMapper glueStockMapper;

    /**
     * 查询库存信息列表
     *
     * @param glueStock 库存信息
     * @return 库存信息
     */
    @Override
    public List<GlueStockDto> selectGlueStockList(GlueStock glueStock) {
        return glueStockMapper.selectGlueStockList(glueStock);
    }

    /**
     * 保存库存信息信息（id为空则新增，id不为空则修改）
     *
     * @param glueStock
     */
    @Override
    public void saveGlueStock(GlueStock glueStock) {
        glueStock.setBaseValue(glueStock.getId());
        this.saveOrUpdate(glueStock);
    }

    /**
     * 批量删除库存信息
     *
     * @param ids 需要删除的库存信息ID
     * @return 结果
     */
    @Override
    public int deleteGlueStockByIds(Long[] ids) {
        return glueStockMapper.deleteGlueStockByIds(ids);
    }


    /**
     * 校验库存信息唯一性
     */
    @Override
    public String checkGlueStockUnique(GlueStock glueStock) {
        if (glueStock == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<GlueStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("STOCK_DATE", glueStock.getStockDate());
        queryWrapper.eq("BAR_CODE", glueStock.getBarCode());
        if (glueStock.getId() != null) {
            queryWrapper.ne("ID", glueStock.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueStock> list = glueStockMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入库存信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<GlueStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<GlueStock> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Integer> importRowMap = new HashMap<>();//通过校验后的数据与在原本的Excel中对应的行数

        try {
            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(String.valueOf(a.getStockDate()), a.getBarCode()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                GlueStock glueStock = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(String.valueOf(glueStock.getStockDate()), glueStock.getBarCode()));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    glueStock.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.stock.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueStock); //校验excel每个单元格长度、类型等



                if (CollectionUtils.isEmpty(validated) && glueStock.getId() == null) {
                    glueStock.setBaseValue(null);
                    importRowMap.put(importList.size(), i + 2);
                    importList.add(glueStock);
                } else {
                    glueStock.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.glueStockMapper.listGlueStockNotUnique(importList, importLogId, I18nUtil.getMessage("setting.stock.database.unique"), SecurityUtils.getUsername());

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
            //勾选更新记录，则覆盖（先删除存量数据，在插入）
            if(CollectionUtils.isNotEmpty(importList)) {
                Date stockDate = importList.get(0).getStockDate();  //库存日期
                String mixArea = importList.get(0).getMixArea();  //密炼区
                if(updateSupport) { //勾选了覆盖数据
                    this.deleteByStockDate(stockDate, mixArea);
                }
                glueStockMapper.batchInsertGlueStockInfo(importList);  //批量插入
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
     * 根据库存日期物料删除那天的数据
     * @param stockDate 库存日期
     */
    private void deleteByStockDate(Date stockDate, String mixArea) {
        Map<String, Object> params = new HashMap<>();
        params.put("stock_date", stockDate);
        params.put("mix_area", mixArea);
        glueStockMapper.deleteByMap(params);
    }
}
