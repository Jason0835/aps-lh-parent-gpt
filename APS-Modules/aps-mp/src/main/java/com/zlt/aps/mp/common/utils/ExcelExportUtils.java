package com.zlt.aps.mp.common.utils;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.zlt.common.utils.ExcelReadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * excel导出操作--公共处理业务方法
 *
 * @author ZLT
 * @date 20250702
 */
@Slf4j
public class ExcelExportUtils {
    /**
     * 填充excel数据及日志信息
     *
     * @param response
     * @param util
     * @param list
     * @param fileName
     * @param param
     * @param exportLog
     * @param procedureCode
     * @return
     * @throws IOException
     */
    public static byte[] fillExcelAndLog(HttpServletResponse response, ExcelUtil util, List list, String fileName, Object param, ExportLog exportLog, String procedureCode) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        Workbook workbook = util.exportExcel2(response, list, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        exportLog.setProcedureCode(procedureCode);
        exportLog.setExportParams(param.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        return resultBytes;
    }

    private ExcelExportUtils() {

    }
}
