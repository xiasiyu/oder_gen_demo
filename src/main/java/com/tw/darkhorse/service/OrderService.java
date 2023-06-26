package com.tw.darkhorse.service;

import com.tw.darkhorse.dto.CreateOrderDto;
import com.tw.darkhorse.dto.FlightDetail;
import com.tw.darkhorse.dto.Order;

import java.util.List;

public interface OrderService {

    Order createOrder(CreateOrderDto createOrderDto);

    void cancelOrder(Long orderId);

    List<Order> getOrders(Long userId);

    FlightDetail getFlightDetail(String flight);
}
