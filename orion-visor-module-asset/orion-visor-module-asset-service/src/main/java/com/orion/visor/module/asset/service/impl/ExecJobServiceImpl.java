package com.orion.visor.module.asset.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orion.lang.define.wrapper.DataGrid;
import com.orion.lang.utils.Booleans;
import com.orion.lang.utils.Strings;
import com.orion.lang.utils.collect.Lists;
import com.orion.lang.utils.time.cron.Cron;
import com.orion.visor.framework.biz.operator.log.core.utils.OperatorLogs;
import com.orion.visor.framework.common.constant.Const;
import com.orion.visor.framework.common.constant.ErrorMessage;
import com.orion.visor.framework.common.utils.Valid;
import com.orion.visor.framework.job.core.utils.QuartzUtils;
import com.orion.visor.framework.security.core.utils.SecurityUtils;
import com.orion.visor.module.asset.convert.ExecJobConvert;
import com.orion.visor.module.asset.convert.HostConvert;
import com.orion.visor.module.asset.dao.ExecJobDAO;
import com.orion.visor.module.asset.dao.ExecLogDAO;
import com.orion.visor.module.asset.dao.HostDAO;
import com.orion.visor.module.asset.entity.domain.ExecJobDO;
import com.orion.visor.module.asset.entity.domain.ExecLogDO;
import com.orion.visor.module.asset.entity.dto.ExecCommandExecDTO;
import com.orion.visor.module.asset.entity.request.exec.*;
import com.orion.visor.module.asset.entity.vo.ExecJobVO;
import com.orion.visor.module.asset.entity.vo.ExecLogVO;
import com.orion.visor.module.asset.entity.vo.HostBaseVO;
import com.orion.visor.module.asset.enums.ExecJobStatusEnum;
import com.orion.visor.module.asset.enums.ExecSourceEnum;
import com.orion.visor.module.asset.enums.HostTypeEnum;
import com.orion.visor.module.asset.enums.ScriptExecEnum;
import com.orion.visor.module.asset.handler.host.exec.job.ExecCommandJob;
import com.orion.visor.module.asset.service.AssetAuthorizedDataService;
import com.orion.visor.module.asset.service.ExecCommandService;
import com.orion.visor.module.asset.service.ExecJobHostService;
import com.orion.visor.module.asset.service.ExecJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 计划任务 服务实现类
 *
 * @author Jiahang Li
 * @version 1.0.3
 * @since 2024-3-28 12:03
 */
@Slf4j
@Service
public class ExecJobServiceImpl implements ExecJobService {

    private static final String QUARTZ_TYPE = "Exec";

    @Resource
    private ExecJobDAO execJobDAO;

    @Resource
    private ExecLogDAO execLogDAO;

    @Resource
    private HostDAO hostDAO;

    @Resource
    private ExecJobHostService execJobHostService;

    @Resource
    private ExecCommandService execCommandService;

    @Resource
    private AssetAuthorizedDataService assetAuthorizedDataService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExecJob(ExecJobCreateRequest request) {
        log.info("ExecJobService-createExecJob request: {}", JSON.toJSONString(request));
        // 验证表达式是否正确
        Cron.of(request.getExpression());
        Valid.valid(ScriptExecEnum::of, request.getScriptExec());
        // 转换
        ExecJobDO record = ExecJobConvert.MAPPER.to(request);
        // 查询数据是否冲突
        this.checkExecJobPresent(record);
        // 查询主机是否有权限
        this.checkHostPermission(request.getHostIdList());
        // 插入任务
        record.setStatus(ExecJobStatusEnum.DISABLED.getStatus());
        int effect = execJobDAO.insert(record);
        Long id = record.getId();
        // 设置任务主机
        execJobHostService.setHostIdByJobId(id, request.getHostIdList());
        // 设置 quartz 状态
        this.setQuartzJobStatus(record, false, true);
        log.info("ExecJobService-createExecJob id: {}, effect: {}", id, effect);
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateExecJobById(ExecJobUpdateRequest request) {
        Long id = Valid.notNull(request.getId(), ErrorMessage.ID_MISSING);
        log.info("ExecJobService-updateExecJobById id: {}, request: {}", id, JSON.toJSONString(request));
        // 验证表达式是否正确
        Cron.of(request.getExpression());
        Valid.valid(ScriptExecEnum::of, request.getScriptExec());
        // 查询
        ExecJobDO record = execJobDAO.selectById(id);
        Valid.notNull(record, ErrorMessage.DATA_ABSENT);
        // 转换
        ExecJobDO updateRecord = ExecJobConvert.MAPPER.to(request);
        // 查询数据是否冲突
        this.checkExecJobPresent(updateRecord);
        // 查询主机是否有权限
        this.checkHostPermission(request.getHostIdList());
        // 更新任务
        int effect = execJobDAO.updateById(updateRecord);
        // 设置任务主机
        execJobHostService.setHostIdByJobId(id, request.getHostIdList());
        // 设置日志参数
        OperatorLogs.add(OperatorLogs.BEFORE, record.getName());
        // 设置 quartz 状态
        this.setQuartzJobStatus(updateRecord, true, ExecJobStatusEnum.ENABLED.getStatus().equals(record.getStatus()));
        log.info("ExecJobService-updateExecJobById effect: {}", effect);
        return effect;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateExecJobStatus(ExecJobUpdateStatusRequest request) {
        Long id = request.getId();
        ExecJobStatusEnum status = ExecJobStatusEnum.of(request.getStatus());
        Valid.notNull(status, ErrorMessage.PARAM_ERROR);
        log.info("ExecJobService-updateExecJobStatus id: {}, status: {}", id, status);
        // 查询任务
        ExecJobDO record = execJobDAO.selectById(id);
        Valid.notNull(record, ErrorMessage.DATA_ABSENT);
        // 更新状态
        ExecJobDO update = new ExecJobDO();
        update.setId(id);
        update.setStatus(status.getStatus());
        int effect = execJobDAO.updateById(update);
        // 设置日志参数
        OperatorLogs.add(OperatorLogs.NAME, record.getName());
        OperatorLogs.add(OperatorLogs.STATUS_NAME, status.getStatusName());
        // 设置 quartz 状态
        this.setQuartzJobStatus(record, true, ExecJobStatusEnum.ENABLED.equals(status));
        return effect;
    }

    @Override
    public ExecJobVO getExecJobById(Long id) {
        // 查询任务
        ExecJobDO record = execJobDAO.selectById(id);
        Valid.notNull(record, ErrorMessage.DATA_ABSENT);
        // 转换
        ExecJobVO vo = ExecJobConvert.MAPPER.to(record);
        // 查询任务主机
        List<Long> hostIdList = execJobHostService.getHostIdByJobId(id);
        vo.setHostIdList(hostIdList);
        // 查询主机列表
        if (!Lists.isEmpty(hostIdList)) {
            List<HostBaseVO> hosts = hostDAO.selectBaseByIdList(hostIdList)
                    .stream()
                    .map(HostConvert.MAPPER::toBase)
                    .collect(Collectors.toList());
            vo.setHostList(hosts);
        } else {
            vo.setHostList(Lists.empty());
        }
        return vo;
    }

    @Override
    public List<ExecJobVO> getExecJobList() {
        return execJobDAO.of()
                .createWrapper()
                .select(ExecJobDO::getId, ExecJobDO::getName)
                .then()
                .list(ExecJobConvert.MAPPER::to);
    }

    @Override
    public DataGrid<ExecJobVO> getExecJobPage(ExecJobQueryRequest request) {
        // 条件
        LambdaQueryWrapper<ExecJobDO> wrapper = this.buildQueryWrapper(request);
        // 查询任务
        DataGrid<ExecJobVO> dataGrid = execJobDAO.of(wrapper)
                .page(request)
                .dataGrid(ExecJobConvert.MAPPER::to);
        if (!Booleans.isTrue(request.getQueryRecentLog())) {
            return dataGrid;
        }
        // 查询最近执行任务
        List<Long> logIdList = dataGrid.stream()
                .map(ExecJobVO::getRecentLogId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!logIdList.isEmpty()) {
            List<ExecLogDO> logList = execLogDAO.selectBatchIds(logIdList);
            Map<Long, ExecLogDO> logMap = logList.stream()
                    .collect(Collectors.toMap(ExecLogDO::getId, Function.identity()));
            // 设置任务状态
            dataGrid.forEach(s -> {
                Long logId = s.getRecentLogId();
                if (logId == null) {
                    return;
                }
                ExecLogDO execLog = logMap.get(logId);
                if (execLog == null) {
                    return;
                }
                s.setRecentLogTime(execLog.getStartTime());
                s.setRecentLogStatus(execLog.getStatus());
            });
        }
        return dataGrid;
    }

    @Override
    public Integer getNextExecSeq(Long id) {
        // 自增
        execJobDAO.incrExecSeq(id);
        // 获取
        return execJobDAO.of()
                .createWrapper()
                .select(ExecJobDO::getExecSeq)
                .eq(ExecJobDO::getId, id)
                .then()
                .getOne(ExecJobDO::getExecSeq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteExecJobById(Long id) {
        return this.deleteExecJobByIdList(Lists.singleton(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteExecJobByIdList(List<Long> idList) {
        log.info("ExecJobService-deleteExecJobByIdList idList: {}", idList);
        // 检查数据是否存在
        List<ExecJobDO> jobList = execJobDAO.selectBatchIds(idList);
        Valid.notEmpty(jobList, ErrorMessage.DATA_ABSENT);
        // 删除任务
        int effect = execJobDAO.deleteBatchIds(idList);
        // 删除任务主机
        effect += execJobHostService.deleteByJobIdList(idList);
        // 设置日志参数
        String name = jobList.stream()
                .map(ExecJobDO::getName)
                .collect(Collectors.joining(Const.COMMA));
        OperatorLogs.add(OperatorLogs.NAME, name);
        // 设置 quartz 状态
        for (ExecJobDO job : jobList) {
            this.setQuartzJobStatus(job, true, false);
        }
        log.info("ExecJobService-deleteExecJobByIdList idList: {}, effect: {}", idList, effect);
        return effect;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualTriggerExecJob(Long id) {
        log.info("ExecJobService.manualTriggerExecJob start id: {}", id);
        ExecJobTriggerRequest request = new ExecJobTriggerRequest();
        request.setId(id);
        // 设置执行用户
        Optional.ofNullable(SecurityUtils.getLoginUser())
                .ifPresent(s -> {
                    request.setUserId(s.getId());
                    request.setUsername(s.getUsername());
                });
        // 触发任务
        this.triggerExecJob(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerExecJob(ExecJobTriggerRequest request) {
        Long id = request.getId();
        // 查询任务
        ExecJobDO job = execJobDAO.selectById(id);
        Valid.notNull(job, ErrorMessage.DATA_ABSENT);
        // 查询任务主机
        List<Long> hostIdList = execJobHostService.getHostIdByJobId(id);
        if (hostIdList.isEmpty()) {
            log.info("ExecJobService.triggerExecJob host empty id: {}", id);
            return;
        }
        // 获取执行序列
        Integer execSeq = this.getNextExecSeq(id);
        // 设置执行参数
        OperatorLogs.add(OperatorLogs.ID, id);
        OperatorLogs.add(OperatorLogs.NAME, job.getName());
        OperatorLogs.add(OperatorLogs.SEQ, execSeq);
        // 执行命令
        ExecCommandExecDTO exec = ExecCommandExecDTO.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .source(ExecSourceEnum.JOB.name())
                .sourceId(id)
                .execSeq(execSeq)
                .description(job.getName())
                .timeout(job.getTimeout())
                .scriptExec(job.getScriptExec())
                .command(job.getCommand())
                .parameterSchema(job.getParameterSchema())
                .hostIdList(hostIdList)
                .build();
        ExecLogVO execResult = execCommandService.execCommandWithSource(exec);
        // 更新最近执行的任务id
        ExecJobDO updateRecent = new ExecJobDO();
        updateRecent.setId(id);
        updateRecent.setRecentLogId(execResult.getId());
        execJobDAO.updateById(updateRecent);
    }

    /**
     * 检查对象是否存在
     *
     * @param domain domain
     */
    private void checkExecJobPresent(ExecJobDO domain) {
        // 构造条件
        LambdaQueryWrapper<ExecJobDO> wrapper = execJobDAO.wrapper()
                // 更新时忽略当前记录
                .ne(ExecJobDO::getId, domain.getId())
                // 用其他字段做重复校验
                .eq(ExecJobDO::getName, domain.getName());
        // 检查是否存在
        boolean present = execJobDAO.of(wrapper).present();
        Valid.isFalse(present, ErrorMessage.DATA_PRESENT);
    }

    /**
     * 构建查询 wrapper
     *
     * @param request request
     * @return wrapper
     */
    private LambdaQueryWrapper<ExecJobDO> buildQueryWrapper(ExecJobQueryRequest request) {
        return execJobDAO.wrapper()
                .eq(ExecJobDO::getId, request.getId())
                .like(ExecJobDO::getName, request.getName())
                .like(ExecJobDO::getCommand, request.getCommand())
                .eq(ExecJobDO::getStatus, request.getStatus())
                .orderByDesc(ExecJobDO::getId);
    }

    /**
     * 设置 quartz 任务状态
     *
     * @param record record
     * @param delete 是否删除
     * @param add    是否新增
     */
    private void setQuartzJobStatus(ExecJobDO record, boolean delete, boolean add) {
        Long id = record.getId();
        // 删除 quartz job
        if (delete) {
            QuartzUtils.deleteJob(QUARTZ_TYPE, id);
        }
        // 启动 quartz job
        if (add) {
            QuartzUtils.addJob(QUARTZ_TYPE, id, record.getExpression(), record.getName(), ExecCommandJob.class);
        }
    }

    /**
     * 检查主机权限
     *
     * @param hostIdList hostIdList
     */
    private void checkHostPermission(List<Long> hostIdList) {
        // 查询有权限的主机
        List<Long> authorizedHostIdList = assetAuthorizedDataService.getUserAuthorizedEnabledHostId(SecurityUtils.getLoginUserId(), HostTypeEnum.SSH);
        for (Long hostId : hostIdList) {
            Valid.isTrue(authorizedHostIdList.contains(hostId), Strings.format(ErrorMessage.PLEASE_CHECK_HOST_SSH, hostId));
        }
    }

}
