package com.zlt.aps.monthplan.common;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *档案Controller抽像类
 *
 */
public abstract class CommonDocController<T extends BaseEntity> extends BaseController {

    /**
     * 获取服务类
     * @return
     */
     abstract protected ICommonService getDocService();

    /**
     * 查询分厂数据
     * @param CompanyCode
     * @param factoryCode
     * @return
     */
    @ApiOperation("查询数据")
    @GetMapping("/queryData")
    public TableDataInfo queryData(@RequestParam String CompanyCode, @RequestParam String factoryCode, @RequestParam Integer year, @RequestParam Integer month){
        List<T> docEntities = getDocService().queryData(CompanyCode, factoryCode, year, month);
        return getDataTable(docEntities);
    }

    @ApiOperation("按指定ID查询")
    @GetMapping("/get/{id}")
    public AjaxResult getById(@PathVariable Long id){
        return AjaxResult.success(getDocService().selectByPrimaryKey(id));
    }

    /**
     * 新增保存数据
     * @param entites
     * @return
     */
    @ApiOperation("保存数据")
    @PostMapping("/saveData")
    public AjaxResult save(List<T> entites){
        int rows = getDocService().saveBatch(entites);

       if (rows == entites.size()){
           return AjaxResult.success(entites);
       } else {
           return AjaxResult.error(AjaxResult.Type.ERROR.value(), "保存失败.");
       }
    }

    /**
     * 删除数据
     * @param ids
     * @return
     */
    @ApiOperation("批量删除数据")
    @PostMapping("/delete")
    public AjaxResult delete(@RequestBody List<String> ids){
        int deleteRows = getDocService().deleteBatch(ids);
        return AjaxResult.success(deleteRows);
    }


}
