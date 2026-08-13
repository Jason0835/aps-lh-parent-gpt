package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90StorageLaneLimitRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90StorageLaneLimitRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90StorageLaneLimit/list")
    TableDataInfo list(@RequestBody Cd90StorageLaneLimit q);

    @ApiOperation("获取详情")
    @GetMapping("/cd90StorageLaneLimit/getInfo/{id}")
    Cd90StorageLaneLimit getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增")
    @PostMapping("/cd90StorageLaneLimit/add")
    AjaxResult add(@RequestBody Cd90StorageLaneLimit e);

    @ApiOperation("编辑")
    @PostMapping("/cd90StorageLaneLimit/edit")
    AjaxResult edit(@RequestBody Cd90StorageLaneLimit e);

    @ApiOperation("删除")
    @PostMapping("/cd90StorageLaneLimit/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验唯一性")
    @PostMapping("/cd90StorageLaneLimit/checkUnique")
    String checkUnique(@RequestBody Cd90StorageLaneLimit e);

    @ApiOperation("导出")
    @PostMapping("/cd90StorageLaneLimit/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90StorageLaneLimit q, @PathVariable("fileName") String n);

    @ApiOperation("导入")
    @PostMapping("/cd90StorageLaneLimit/importData")
    AjaxResult importData(@RequestBody ImportContext c, @RequestParam("updateSupport") boolean u);

    /**
     * 按工厂、日期和班次全量覆盖库排状态。
     *
     * @param factoryCode 工厂编码
     * @param laneDate 库排日期
     * @param shiftCode 班次编码
     * @param updateBy 更新人
     * @param list 库排状态
     * @return 保存结果
     */
    @ApiOperation("MES全量覆盖直裁库排状态")
    @PostMapping("/cd90StorageLaneLimit/logicDeleteAndSaveMesBatch")
    AjaxResult logicDeleteAndSaveMesBatch(@RequestParam("factoryCode") String factoryCode,
                                          @RequestParam("laneDate") String laneDate,
                                          @RequestParam("shiftCode") String shiftCode,
                                          @RequestParam("updateBy") String updateBy,
                                          @RequestBody List<Cd90StorageLaneLimit> list);
}
