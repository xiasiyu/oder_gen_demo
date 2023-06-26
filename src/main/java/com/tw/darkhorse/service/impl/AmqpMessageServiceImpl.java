package com.tw.darkhorse.service.impl;

import com.tw.darkhorse.integration.mq.OrderEventSender;
import com.tw.darkhorse.service.MessageService;
import org.springframework.stereotype.Component;

@Component
public class AmqpMessageServiceImpl implements MessageService {
    private final OrderEventSender orderEventSender;

    public AmqpMessageServiceImpl(OrderEventSender orderEventSender) {
        this.orderEventSender = orderEventSender;
    }

    @Override
    public void sendMsg(String eventJson) {
        orderEventSender.sendMessage(eventJson);
    }
}
