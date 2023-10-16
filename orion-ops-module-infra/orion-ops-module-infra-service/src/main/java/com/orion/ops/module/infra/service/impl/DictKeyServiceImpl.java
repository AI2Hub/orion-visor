package com.orion.ops.module.infra.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orion.lang.utils.Objects1;
import com.orion.ops.framework.common.constant.Const;
import com.orion.ops.framework.common.constant.ErrorMessage;
import com.orion.ops.framework.common.utils.Valid;
import com.orion.ops.framework.redis.core.utils.RedisMaps;
import com.orion.ops.module.infra.convert.DictKeyConvert;
import com.orion.ops.module.infra.dao.DictKeyDAO;
import com.orion.ops.module.infra.define.cache.DictCacheKeyDefine;
import com.orion.ops.module.infra.entity.domain.DictKeyDO;
import com.orion.ops.module.infra.entity.dto.DictKeyCacheDTO;
import com.orion.ops.module.infra.entity.request.dict.DictKeyCreateRequest;
import com.orion.ops.module.infra.entity.request.dict.DictKeyUpdateRequest;
import com.orion.ops.module.infra.entity.vo.DictKeyVO;
import com.orion.ops.module.infra.service.DictKeyService;
import com.orion.ops.module.infra.service.DictValueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典配置项 服务实现类
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2023-10-16 16:33
 */
@Slf4j
@Service
public class DictKeyServiceImpl implements DictKeyService {

    @Resource
    private DictKeyDAO dictKeyDAO;

    @Resource
    private DictValueService dictValueService;

    @Override
    public Long createDictKey(DictKeyCreateRequest request) {
        log.info("DictKeyService-createDictKey request: {}" , JSON.toJSONString(request));
        // 转换
        DictKeyDO record = DictKeyConvert.MAPPER.to(request);
        // 查询数据是否冲突
        this.checkDictKeyPresent(record);
        // 插入
        int effect = dictKeyDAO.insert(record);
        Long id = record.getId();
        log.info("DictKeyService-createDictKey id: {}, effect: {}" , id, effect);
        // 删除缓存
        RedisMaps.delete(DictCacheKeyDefine.DICT_KEY);
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateDictKeyById(DictKeyUpdateRequest request) {
        Long id = Valid.notNull(request.getId(), ErrorMessage.ID_MISSING);
        log.info("DictKeyService-updateDictKeyById id: {}, request: {}" , id, JSON.toJSONString(request));
        // 查询
        DictKeyDO record = dictKeyDAO.selectById(id);
        Valid.notNull(record, ErrorMessage.DATA_ABSENT);
        // 转换
        DictKeyDO updateRecord = DictKeyConvert.MAPPER.to(request);
        // 查询数据是否冲突
        this.checkDictKeyPresent(updateRecord);
        // 更新
        int effect = dictKeyDAO.updateById(updateRecord);
        // 如果修改了 key 则需要修改 dictValue.key
        if (!Objects1.eq(record.getKey(), request.getKey())) {
            dictValueService.updateKeyNameByKeyId(id, record.getKey(), request.getKey());
        }
        // 删除缓存
        RedisMaps.delete(DictCacheKeyDefine.DICT_KEY);
        log.info("DictKeyService-updateDictKeyById effect: {}" , effect);
        return effect;
    }

    @Override
    public List<DictKeyVO> getDictKeyList() {
        // 查询缓存
        List<DictKeyCacheDTO> list = RedisMaps.valuesJson(DictCacheKeyDefine.DICT_KEY);
        if (list.isEmpty()) {
            // 查询数据库
            list = dictKeyDAO.of().list(DictKeyConvert.MAPPER::toCache);
            // 添加默认值 防止穿透
            if (list.isEmpty()) {
                list.add(DictKeyCacheDTO.builder()
                        .id(Const.NONE_ID)
                        .build());
            }
            // 设置缓存
            RedisMaps.putAllJson(DictCacheKeyDefine.DICT_KEY.getKey(), s -> s.getId().toString(), list);
            RedisMaps.setExpire(DictCacheKeyDefine.DICT_KEY);
        }
        // 删除默认值
        return list.stream()
                .filter(s -> !s.getId().equals(Const.NONE_ID))
                .map(DictKeyConvert.MAPPER::to)
                .sorted(Comparator.comparing(DictKeyVO::getKey))
                .collect(Collectors.toList());
    }

    @Override
    public Integer deleteDictKeyById(Long id) {
        log.info("DictKeyService-deleteDictKeyById id: {}" , id);
        // 检查数据是否存在
        DictKeyDO record = dictKeyDAO.selectById(id);
        Valid.notNull(record, ErrorMessage.DATA_ABSENT);
        // 删除配置项
        int effect = dictKeyDAO.deleteById(id);
        // 删除配置值
        dictValueService.deleteDictValueByKeyId(id);
        // 删除缓存
        RedisMaps.delete(DictCacheKeyDefine.DICT_KEY, id);
        log.info("DictKeyService-deleteDictKeyById id: {}, effect: {}" , id, effect);
        return effect;
    }

    @Override
    public Integer deleteDictKeyByIdList(List<Long> idList) {
        log.info("DictKeyService-deleteDictKeyByIdList idList: {}" , idList);
        int effect = dictKeyDAO.deleteBatchIds(idList);
        // 删除配置值
        dictValueService.deleteDictValueByKeyIdList(idList);
        log.info("DictKeyService-deleteDictKeyByIdList effect: {}" , effect);
        // 删除缓存
        RedisMaps.delete(DictCacheKeyDefine.DICT_KEY, idList);
        return effect;
    }

    /**
     * 检查对象是否存在
     *
     * @param domain domain
     */
    private void checkDictKeyPresent(DictKeyDO domain) {
        // 构造条件
        LambdaQueryWrapper<DictKeyDO> wrapper = dictKeyDAO.wrapper()
                // 更新时忽略当前记录
                .ne(DictKeyDO::getId, domain.getId())
                // 用其他字段做重复校验
                .eq(DictKeyDO::getKey, domain.getKey());
        // 检查是否存在
        boolean present = dictKeyDAO.of(wrapper).present();
        Valid.isFalse(present, ErrorMessage.DATA_PRESENT);
    }

}