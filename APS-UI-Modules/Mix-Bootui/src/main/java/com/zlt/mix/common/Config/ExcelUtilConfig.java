package com.zlt.mix.common.Config;

import com.ruoyi.file.api.service.ISimpleFileService;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class ExcelUtilConfig {

    @Autowired
    private ISimpleFileService iSimpleFileService;

    @Bean
    public ExportUtil exportUtil() {
        return new ExportUtil(iSimpleFileService);
    }

    @Bean
    public ImportUtil importUtil() {
        return new ImportUtil(iSimpleFileService);
    }

}
