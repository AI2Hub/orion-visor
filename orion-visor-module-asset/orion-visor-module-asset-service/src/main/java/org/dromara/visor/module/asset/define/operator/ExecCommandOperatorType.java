/*
 * Copyright (c) 2023 - present Jiahang Li (visor.orionsec.cn ljh1553488six@139.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.visor.module.asset.define.operator;

import org.dromara.visor.framework.biz.operator.log.core.annotation.Module;
import org.dromara.visor.framework.biz.operator.log.core.enums.OperatorRiskLevel;
import org.dromara.visor.framework.biz.operator.log.core.factory.InitializingOperatorTypes;
import org.dromara.visor.framework.biz.operator.log.core.model.OperatorType;

/**
 * 批量执行 操作记录类型
 *
 * @author Jiahang Li
 * @version 1.0.1
 * @since 2024-3-11 11:31
 */
@Module("asset:exec-command")
public class ExecCommandOperatorType extends InitializingOperatorTypes {

    public static final String EXEC = "exec-command:exec";

    @Override
    public OperatorType[] types() {
        return new OperatorType[]{
                new OperatorType(OperatorRiskLevel.M, EXEC, "执行主机命令"),
        };
    }

}
