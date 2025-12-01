package com.zlt.aps.controller.system;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.domain.ImportLogVo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 导入日志管理接口
 */
@RestController
@RequestMapping("/system/importLog")
@Api(tags = {"导入日志管理接口"})
public class ImportLogUIController extends BaseUIController<ImportLog> {



    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iimportErrorLogService;

    /**
     * 查询导入日志管理列表
     */
    @ApiOperation("查询导入日志管理列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ImportLogVo dto) {

        if(dto.getDataArray() == null){
            TableDataInfo tableDataInfo = new TableDataInfo();
            List<ImportLogVo> importLogVoList = new ArrayList<>();
            tableDataInfo.setRows(importLogVoList);
            tableDataInfo.setCode(200);
            tableDataInfo.setTotal(0);
            return tableDataInfo;
        }else {
            Date array1 = dto.getDataArray()[0];
            Date array2 = dto.getDataArray()[1];
            dto.getParams().put("startDate",array1);
            dto.getParams().put("finallyDate",array2);
            return iImportLogService.list(dto);
        }


    }

    /**
     * 错误日志详情列表
     */
    @ApiOperation("错误日志详情列表")
    @PostMapping("/errorDetailList")
    @ResponseBody
    public TableDataInfo detailList(ImportErrorLog dto) {
        return iimportErrorLogService.list(dto);
    }


    @GetMapping("/download")
    public void resourceDownload(String name, String url, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
//        response.setCharacterEncoding("utf-8");
//        response.setContentType("multipart/form-data");
//        response.setHeader("Content-Disposition", "attachment;fileName=" + FileUtils4UI.setFileDownloadHeader(request, name));
////        byte[] data = iDmsFileService.downloadByteFile(url, "import");
//        OutputStream outputStream = response.getOutputStream();
//        IOUtils.write(data, outputStream);
//        outputStream.close();
    }

}
