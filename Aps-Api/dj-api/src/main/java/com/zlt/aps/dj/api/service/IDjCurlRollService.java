package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjCurlRollDto;
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 垫胶卷曲信息对外暴露接口
 */
@FeignClient(contextId = "iNcCurlRollService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:nc}")
public interface IDjCurlRollService {

    /**
     * 根据条件查询帘布大卷信息列表
     */
    @PostMapping("/dj/curlRoll/listCurlRoll")
    TableDataInfo listCurlRoll(@RequestBody DjCurlRoll dto);

    /**
     * 根据id查询帘布大卷信息信息
     */
    @GetMapping("/dj/curlRoll/getCurlRoll/{id}")
    DjCurlRoll getCurlRoll(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/dj/curlRoll/saveCurlRoll")
    AjaxResult saveCurlRoll(@RequestBody DjCurlRoll dto);

    /**
     * 根据code判断纤维大卷代号是否已经存在
     */
    @PostMapping("/dj/curlRoll/checkCurlRollCodeUnique")
    String checkCurlRollCodeUnique(@RequestBody DjCurlRoll dto);

    /**
     * 批量删除帘布大卷信息信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/dj/curlRoll/deleteCurlRoll/{ids}")
    AjaxResult deleteCurlRoll(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/dj/curlRoll/exportData")
    List<DjCurlRoll> exportData(@RequestBody DjCurlRoll dto);

    /**
     * 导入数据
     */
    @PostMapping("/dj/curlRoll/importData")
    public AjaxResult importData(@RequestBody List<DjCurlRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/dj/curlRoll/selectCurlLengthByCode")
    public AjaxResult selectCurlLengthByCode(@RequestBody DjCurlRoll curlRoll);
}
