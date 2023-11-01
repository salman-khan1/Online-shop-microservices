package com.example.orderservice.service;

import com.example.orderservice.dto.InventoryResponse;
import com.example.orderservice.dto.OrderLineItemDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public void placeOrder(OrderRequest orderRequest){
        Order order=new Order ();
        order.setOrderNumber (UUID.randomUUID ().toString ());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList ()
                .stream ()
                .map (this::mapToDto)
                .toList ();
        order.setOrderLineItemsList (orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList ().stream ()
                .map (OrderLineItems::getSkuCode)
                .toList ();

        //call inventory Service,and place order if product is in stock
        InventoryResponse[] inventoryResponseArray= webClient.get ()
                .uri ("http://localhost:8082/api/inventory",
                        uriBuilder -> uriBuilder.queryParam ("skuCode",skuCodes).build ())
                .retrieve ()
                .bodyToMono (InventoryResponse[].class)
                .block ();

//        assert inventoryResponseArray != null;
        boolean allProductsInStocks = Arrays.stream (inventoryResponseArray)
                .allMatch (InventoryResponse::isInStock);
        if(allProductsInStocks){
            orderRepository.save (order);
        }else{
            throw new IllegalArgumentException ("Product is not in Stocks");
        }

    }

    private OrderLineItems mapToDto (OrderLineItemDto orderLineItemDto) {
       OrderLineItems orderLineItems=new OrderLineItems ();
       orderLineItems.setPrice(orderLineItemDto.getPrice ());
       orderLineItems.setQuantity (orderLineItemDto.getQuantity ());
       orderLineItems.setSkuCode (orderLineItemDto.getSkuCode ());
       return orderLineItems;

    }
}
