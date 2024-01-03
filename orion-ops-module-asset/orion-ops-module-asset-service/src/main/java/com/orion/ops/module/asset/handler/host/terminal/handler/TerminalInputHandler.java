package com.orion.ops.module.asset.handler.host.terminal.handler;

import com.orion.ops.module.asset.handler.host.terminal.manager.TerminalManager;
import com.orion.ops.module.asset.handler.host.terminal.model.request.TerminalInputRequest;
import com.orion.ops.module.asset.handler.host.terminal.session.ITerminalSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 处理输入处理器
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2023/12/29 15:32
 */
@Slf4j
@Component
public class TerminalInputHandler extends AbstractTerminalHandler<TerminalInputRequest> {

    @Resource
    private TerminalManager terminalManager;

    @Override
    public void handle(WebSocketSession session, TerminalInputRequest payload) {
        // 获取会话
        ITerminalSession terminalSession = terminalManager.getSession(session.getId(), payload.getSession());
        if (terminalSession != null) {
            // 处理输入
            terminalSession.write(payload.getCommand());
        }
    }

}