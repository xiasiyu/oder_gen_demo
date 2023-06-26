package com.tw.darkhorse.service.impl;

import com.alibaba.fastjson.JSON;
import com.tw.darkhorse.common.enums.OrderStatusEnum;
import com.tw.darkhorse.common.exception.BusinessException;
import com.tw.darkhorse.common.exception.ErrorCode;
import com.tw.darkhorse.common.exception.NoMoreSeatException;
import com.tw.darkhorse.dto.CreateOrderDto;
import com.tw.darkhorse.dto.FlightDetail;
import com.tw.darkhorse.dto.Order;
import com.tw.darkhorse.dto.OrderMessage;
import com.tw.darkhorse.dto.ReleaseSeatRequest;
import com.tw.darkhorse.dto.ReserveSeatRequest;
import com.tw.darkhorse.entity.PassengerEntity;
import com.tw.darkhorse.entity.TicketOrderEntity;
import com.tw.darkhorse.entity.TicketOrderEventEntity;
import com.tw.darkhorse.integration.client.PriceSeatManagerClient;
import com.tw.darkhorse.repository.TicketOrderEventRepository;
import com.tw.darkhorse.repository.TicketOrderRepository;
import com.tw.darkhorse.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final TicketOrderRepository orderRepository;
    private final TicketOrderEventRepository ticketOrderEventRepository;
    private final PriceSeatManagerClient priceSeatManagerClient;
    private final AmqpMessageServiceImpl amqpMessageServiceImpl;

    public OrderServiceImpl(TicketOrderRepository orderRepository, TicketOrderEventRepository ticketOrderEventRepository, PriceSeatManagerClient priceSeatManagerClient, AmqpMessageServiceImpl amqpMessageServiceImpl) {
        this.orderRepository = orderRepository;
        this.ticketOrderEventRepository = ticketOrderEventRepository;
        this.priceSeatManagerClient = priceSeatManagerClient;
        this.amqpMessageServiceImpl = amqpMessageServiceImpl;
    }

    @Override
    public Order createOrder(CreateOrderDto createOrderDto) {
        ReserveSeatRequest reserveSeatRequest = new ReserveSeatRequest(createOrderDto.getFlight(), createOrderDto.getClassType(),
            createOrderDto.getPassengerDtoList().size());
        boolean reserveSuccess = priceSeatManagerClient.reserveSeat(reserveSeatRequest);
        if (!reserveSuccess) {
            throw new NoMoreSeatException();
        }
        TicketOrderEntity ticketOrderEntity = transferToOrderEntity(createOrderDto);
        List<PassengerEntity> passengerEntities = createOrderDto.getPassengerDtoList().stream().map(passenger -> {
            PassengerEntity passengerEntity = new PassengerEntity();
            BeanUtils.copyProperties(passenger, passengerEntity);
            passengerEntity.setOrder(ticketOrderEntity);
            return passengerEntity;
        }).collect(Collectors.toList());
        TicketOrderEventEntity ticketOrderEventEntity = TicketOrderEventEntity.builder().status(OrderStatusEnum.CREATED.name()).build();
        ticketOrderEventEntity.setOrder(ticketOrderEntity);
        ticketOrderEntity.setPassengers(passengerEntities);
        ticketOrderEntity.setEvents(Collections.singletonList(ticketOrderEventEntity));
        orderRepository.save(ticketOrderEntity);

//        publish(ticketOrderEntity, OrderStatusEnum.CREATED.name());
        return transferOrder(ticketOrderEntity);
    }

    private TicketOrderEntity transferToOrderEntity(CreateOrderDto createOrderDto) {
        return TicketOrderEntity.builder()
            .userId(createOrderDto.getUserId())
            .flight(createOrderDto.getFlight())
            .classType(createOrderDto.getClassType())
            .contactName(createOrderDto.getContactName())
            .contactMobile(createOrderDto.getContactMobile())
            .build();
    }

    @Override
    public void cancelOrder(Long orderId) {
        Optional<TicketOrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            TicketOrderEntity ticketOrderEntity = orderOpt.get();
            ReleaseSeatRequest releaseSeatRequest = new ReleaseSeatRequest(ticketOrderEntity.getFlight(), ticketOrderEntity.getClassType(),
                ticketOrderEntity.getPassengers().size());
            boolean releaseSuccess = priceSeatManagerClient.releaseSeat(releaseSeatRequest);
            if (releaseSuccess) {
                ticketOrderEventRepository.save(TicketOrderEventEntity.builder().status(OrderStatusEnum.USER_CANCELLED.name()).build());
                publish(ticketOrderEntity, OrderStatusEnum.USER_CANCELLED.name());
            } else {
                throw new BusinessException(ErrorCode.CANCEL_ORDER_EXCEPTION, "取消订单操作失败，请稍后再试");
            }
        }
    }

    private void publish(TicketOrderEntity ticketOrderEntity, String status) {
        OrderMessage orderMessage = OrderMessage.from(ticketOrderEntity);
        orderMessage.setPassengerList(OrderMessage.transferPassenger(ticketOrderEntity.getPassengers()));
        orderMessage.setStatus(status);
        amqpMessageServiceImpl.sendMsg(JSON.toJSONString(orderMessage));
    }

    @Override
    public List<Order> getOrders(Long userId) {
        List<TicketOrderEntity> ticketOrderEntityList = orderRepository.findByUserId(userId);
        return ticketOrderEntityList.stream().map(this::transferOrder).collect(Collectors.toList());
    }

    @Override
    public FlightDetail getFlightDetail(String flight) {
        return priceSeatManagerClient.getFlightDetail(flight);
    }

    private Order transferOrder(TicketOrderEntity ticketOrderEntity) {
        Order order = Order.from(ticketOrderEntity);
        order.setPassengerList(Order.transferPassenger(ticketOrderEntity.getPassengers()));
        order.setOrderEventList(Order.transferOrderEvent(ticketOrderEntity.getEvents()));
        return order;
    }
}
