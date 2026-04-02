package com.zlt.aps.cx.api.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmStructureTreadConfig;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 胎面整车配置远程服务接口
 *
 * @author zlt
 * @since 2026-04-02
 */
public interface IMdmStructureTreadConfigRemoteService {

    @PostMapping("/mdmStructureTreadConfig/list")
    TableDataInfo list(@RequestBody MdmStructureTreadConfig query);

    @GetMapping("/mdmStructureTreadConfig/{billId}")
    MdmStructureTreadConfig getInfo(@PathVariable("billId") Long billId);

    @PostMapping("/mdmStructureTreadConfig/add")
    AjaxResult add(@RequestBody MdmStructureTreadConfig entity);

    @PutMapping("/mdmStructureTreadConfig/edit")
    AjaxResult edit(@RequestBody MdmStructureTreadConfig entity);

    @PostMapping("/mdmStructureTreadConfig/remove")
    AjaxResult remove(@RequestParam String ids);

    @PostMapping("/mdmStructureTreadConfig/export")
    void export(HttpServletResponse response, @RequestBody MdmStructureTreadConfig entity) throws IOException;

    @PostMapping("/mdmStructureTreadConfig/importData")
    AjaxResult importData(@RequestPart("file") MultipartFile file,
                          @RequestParam(defaultValue = "false") boolean updateSupport) throws Exception;

    @GetMapping("/mdmStructureTreadConfig/importTemplate")
    AjaxResult importTemplate(HttpServletResponse response) throws IOException;
}
