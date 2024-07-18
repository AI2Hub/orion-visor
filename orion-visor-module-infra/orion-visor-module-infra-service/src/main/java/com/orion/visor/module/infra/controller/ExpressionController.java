package com.orion.visor.module.infra.controller;

import com.orion.lang.utils.collect.Lists;
import com.orion.lang.utils.time.Dates;
import com.orion.lang.utils.time.cron.Cron;
import com.orion.lang.utils.time.cron.CronSupport;
import com.orion.visor.framework.web.core.annotation.RestWrapper;
import com.orion.visor.module.infra.entity.request.exoression.CronNextRequest;
import com.orion.visor.module.infra.entity.vo.CronNextVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.PermitAll;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表达式服务
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2024/4/2 16:33
 */
@Tag(name = "infra - 表达式服务")
@Slf4j
@Validated
@RestWrapper
@RestController
@RequestMapping("/infra/expression")
public class ExpressionController {

    @PermitAll
    @GetMapping("/cron-next")
    @Operation(summary = "获取 cron 下次执行时间")
    public CronNextVO getCronNextTime(@Validated CronNextRequest request) {
        CronNextVO next = new CronNextVO();
        try {
            Cron cron = Cron.of(request.getExpression());
            List<String> nextTime = CronSupport.getNextTime(cron, request.getTimes())
                    .stream()
                    .map(Dates::format)
                    .collect(Collectors.toList());
            next.setNext(nextTime);
            next.setValid(true);
        } catch (Exception e) {
            next.setNext(Lists.empty());
            next.setValid(false);
        }
        return next;
    }

}
