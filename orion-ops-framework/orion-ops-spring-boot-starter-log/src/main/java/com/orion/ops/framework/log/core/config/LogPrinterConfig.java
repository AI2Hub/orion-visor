package com.orion.ops.framework.log.core.config;

import com.orion.ops.framework.log.core.utils.Utils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 日志打印配置
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2023/6/28 22:36
 */
@Data
@ConfigurationProperties("logging.printer")
public class LogPrinterConfig {

    /**
     * 字段配置
     */
    private LogPrinterFieldConfig field;

    /**
     * 显示的请求头
     */
    private List<String> headers;

    public void setField(LogPrinterFieldConfig field) {
        this.field = field;
    }

    public void setHeaders(List<String> headers) {
        this.headers = Utils.parseStringList(headers, String::toLowerCase);
    }

}