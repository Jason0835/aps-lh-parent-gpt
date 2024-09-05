package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysDictType;
import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典类型对外暴露接口
 */
@FeignClient(contextId = "iSysDictTypeService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysDictTypeService {

    @GetMapping("/dict/type/list")
    TableDataInfo list(@SpringQueryMap SysDictType dictType);


    @GetMapping("/dict/type/totalList")
    List<SysDictType> totalList(@SpringQueryMap SysDictType role);

    /*@PostMapping("/dict/type/export")
    void export(HttpServletResponse response, SysDictType dictType) throws IOException;*/

    @GetMapping(value = "/dict/type/{dictId}")
    AjaxResult getInfo(@PathVariable("dictId") Long dictId);

    @PostMapping("/dict/type")
    AjaxResult add(@Validated @RequestBody SysDictType dict);

    @PutMapping("/dict/type")
    AjaxResult edit(@Validated @RequestBody SysDictType dict);

    @DeleteMapping("/dict/type/{dictIds}")
    AjaxResult remove(@PathVariable("dictIds") Long[] dictIds);

    @DeleteMapping("/dict/type/clearCache")
    AjaxResult clearCache();

    @GetMapping("/dict/type/optionselect")
    AjaxResult optionselect();

    /**
     * 根据字典类型ID获取字典类型
     * @param dictId
     * @return
     */
    @PostMapping("/dict/type/selectDictTypeById")
    SysDictType selectDictTypeById(@RequestParam("dictId") Long dictId);

    /**
     * 获取所有字典类型
     * @return
     */
    @PostMapping("/dict/type/selectDictTypeAll")
    List<SysDictType> selectDictTypeAll();

    /**
     * 根据类型获取数据字典类型
     * @param dictType
     * @return
     */
    @PostMapping("/dict/type/selectDictTypeByType")
    SysDictType selectDictTypeByType(@RequestParam("dictType") String dictType);

    /**
     * 根据字典类型key获取字典项
     * @param dictType
     * @return
     */
    @PostMapping("/dict/type/selectDictDataByType")
    List<SysDictData> selectDictDataByType(@RequestParam("dictType") String dictType);

    /**
     * 校验字典类型
     * @param dictType
     * @return
     */
    @PostMapping("/dict/type/checkDictTypeUnique")
    String checkDictTypeUnique(@RequestBody SysDictType dictType);

    /**
     * 加载字典列表树
     * @return
     */
    @PostMapping("/dict/type/treeData")
    List<Ztree> treeData();
}
