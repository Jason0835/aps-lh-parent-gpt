package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixMasterMaterialStockMapper;
import com.zlt.aps.lh.api.domain.entity.MixMasterMaterialStock;
import com.zlt.aps.lh.service.MixMasterMaterialStockService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 母炼胶小料库存Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixMasterMaterialStockServiceImpl implements MixMasterMaterialStockService
{
    @Autowired
    private MixMasterMaterialStockMapper mixMasterMaterialStockMapper;

    /**
     * 查询母炼胶小料库存
     * 
     * @param id 母炼胶小料库存ID
     * @return 母炼胶小料库存
     */
    @Override
    public MixMasterMaterialStock selectMixMasterMaterialStockById(Long id)
    {
        return mixMasterMaterialStockMapper.selectMixMasterMaterialStockById(id);
    }

    /**
     * 查询母炼胶小料库存列表
     * 
     * @param mixMasterMaterialStock 母炼胶小料库存
     * @return 母炼胶小料库存
     */
    @Override
    public List<MixMasterMaterialStock> selectMixMasterMaterialStockList(MixMasterMaterialStock mixMasterMaterialStock)
    {
        return mixMasterMaterialStockMapper.selectMixMasterMaterialStockList(mixMasterMaterialStock);
    }

    /**
     * 新增母炼胶小料库存
     * 
     * @param mixMasterMaterialStock 母炼胶小料库存
     * @return 结果
     */
    @Override
    public int insertMixMasterMaterialStock(MixMasterMaterialStock mixMasterMaterialStock)
    {
        mixMasterMaterialStock.setBaseVale(null);
        return mixMasterMaterialStockMapper.insertMixMasterMaterialStock(mixMasterMaterialStock);
    }

    /**
     * 修改母炼胶小料库存
     * 
     * @param mixMasterMaterialStock 母炼胶小料库存
     * @return 结果
     */
    @Override
    public int updateMixMasterMaterialStock(MixMasterMaterialStock mixMasterMaterialStock)
    {
        mixMasterMaterialStock.setBaseVale(mixMasterMaterialStock.getId());
        return mixMasterMaterialStockMapper.updateMixMasterMaterialStock(mixMasterMaterialStock);
    }

    /**
     * 批量删除母炼胶小料库存
     * 
     * @param ids 需要删除的母炼胶小料库存ID
     * @return 结果
     */
    @Override
    public int deleteMixMasterMaterialStockByIds(Long[] ids)
    {
        return mixMasterMaterialStockMapper.deleteMixMasterMaterialStockByIds(ids);
    }

    /**
     * 删除母炼胶小料库存信息
     * 
     * @param id 母炼胶小料库存ID
     * @return 结果
     */
    @Override
    public int deleteMixMasterMaterialStockById(Long id)
    {
        return mixMasterMaterialStockMapper.deleteMixMasterMaterialStockById(id);
    }

    /**
     * 校验母炼胶小料库存唯一性
     */
    @Override
    public String checkMixMasterMaterialStockUnique(MixMasterMaterialStock mixMasterMaterialStock) {
        if (mixMasterMaterialStock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixMasterMaterialStock> list = mixMasterMaterialStockMapper.selectMixMasterMaterialStockList(mixMasterMaterialStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入母炼胶小料库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixMasterMaterialStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixMasterMaterialStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixMasterMaterialStock mixMasterMaterialStock = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixMasterMaterialStock);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixMasterMaterialStock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixMasterMaterialStock.setBaseVale(null);
                importList.add(mixMasterMaterialStock);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixMasterMaterialStockMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixMasterMaterialStock mixMasterMaterialStock = list.get(i);
                    // 错误记录跳过
                    if (mixMasterMaterialStock.getId() != null && mixMasterMaterialStock.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixMasterMaterialStockUnique(mixMasterMaterialStock);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixMasterMaterialStock(mixMasterMaterialStock);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
