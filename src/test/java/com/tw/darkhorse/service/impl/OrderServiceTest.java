package com.tw.darkhorse.service.impl;

import com.tw.darkhorse.common.exception.BusinessException;
import com.tw.darkhorse.common.exception.NoMoreSeatException;
import com.tw.darkhorse.dto.Order;
import com.tw.darkhorse.dto.ReleaseSeatRequest;
import com.tw.darkhorse.dto.ReserveSeatRequest;
import com.tw.darkhorse.entity.TicketOrderEntity;
import com.tw.darkhorse.integration.client.PriceSeatManagerClient;
import com.tw.darkhorse.repository.TicketOrderEventRepository;
import com.tw.darkhorse.repository.TicketOrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Optional;

import static com.tw.darkhorse.OrderFixture.getCreateOrderDto;
import static com.tw.darkhorse.OrderFixture.getOrder;
import static com.tw.darkhorse.OrderFixture.getOrderEntity;
import static com.tw.darkhorse.OrderFixture.getOrderEventEntity;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Spy
    TicketOrderRepository orderRepositorySpy;
    @Spy
    TicketOrderEventRepository orderEventRepositorySpy;
    @Spy
    PriceSeatManagerClient priceSeatManagerClient;
    @Mock
    AmqpMessageServiceImpl amqpMessageServiceImpl;
    @InjectMocks
    OrderServiceImpl orderServiceImpl;

    @Captor
    ArgumentCaptor<ReserveSeatRequest> reserveRequestCaptor;

    @Captor
    ArgumentCaptor<ReleaseSeatRequest> releaseRequestCaptor;

    @Captor
    ArgumentCaptor<TicketOrderEntity> order;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void should_create_order_successful() {
        when(priceSeatManagerClient.reserveSeat(any())).thenReturn(true);
        when(orderRepositorySpy.save(any())).thenReturn(any());

        Order result = orderServiceImpl.createOrder(getCreateOrderDto());
        Assertions.assertEquals(getOrder(), result);

        verify(priceSeatManagerClient).reserveSeat(reserveRequestCaptor.capture());
        Assertions.assertEquals("MU2151", reserveRequestCaptor.getValue().getFlight());
        Assertions.assertEquals(1, reserveRequestCaptor.getValue().getNumber());

        verify(orderRepositorySpy).save(order.capture());
        Assertions.assertEquals("张三", order.getValue().getContactName());
    }

    @Test
    void should_cancel_order_successful() {
        when(orderRepositorySpy.findById(123L)).thenReturn(Optional.of(getOrderEntity()));
        when(orderEventRepositorySpy.save(getOrderEventEntity())).thenReturn(getOrderEventEntity());
        when(priceSeatManagerClient.releaseSeat(any())).thenReturn(true);
        doNothing().when(amqpMessageServiceImpl).sendMsg(any());

        orderServiceImpl.cancelOrder(123L);

        verify(orderEventRepositorySpy, times(1)).save(any());
        verify(amqpMessageServiceImpl, times(1)).sendMsg(any());
        verify(priceSeatManagerClient).releaseSeat(releaseRequestCaptor.capture());
        Assertions.assertEquals("MU2151", releaseRequestCaptor.getValue().getFlight());
        Assertions.assertEquals(1, releaseRequestCaptor.getValue().getNumber());
    }

    @Test
    void should_throw_exception_when_order_not_found() {
        when(orderRepositorySpy.findById(123L)).thenReturn(Optional.empty());
        when(orderEventRepositorySpy.save(getOrderEventEntity())).thenReturn(getOrderEventEntity());
        when(priceSeatManagerClient.releaseSeat(any())).thenReturn(true);
        doNothing().when(amqpMessageServiceImpl).sendMsg(any());

        Assertions.assertThrows(BusinessException.class, () -> orderServiceImpl.cancelOrder(123L));
        verify(orderEventRepositorySpy, never()).save(any());
        verify(priceSeatManagerClient, never()).releaseSeat(any());
        verify(amqpMessageServiceImpl, never()).sendMsg(any());
    }

    @Test
    void should_throw_exception_when_seat_manager_fails_to_release_seat() {
        when(orderRepositorySpy.findById(123L)).thenReturn(Optional.of(getOrderEntity()));
        when(orderEventRepositorySpy.save(getOrderEventEntity())).thenReturn(getOrderEventEntity());
        when(priceSeatManagerClient.releaseSeat(any())).thenReturn(false);
        doNothing().when(amqpMessageServiceImpl).sendMsg(any());

        Assertions.assertThrows(BusinessException.class, () -> orderServiceImpl.cancelOrder(123L));
    }
}
