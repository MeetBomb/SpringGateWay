package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderDTO;
import com.example.orderservice.jpa.OrderEntity;
import com.example.orderservice.messagequeue.KafkaProducer;
import com.example.orderservice.messagequeue.OrderProducer;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.vo.RequestOrder;
import com.example.orderservice.vo.ResponseOrder;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order-service")
@Slf4j
public class OrderController {

    Environment environment;
    OrderService orderService;
    KafkaProducer kafkaProducer;
    OrderProducer orderProducer;

    @Autowired
    public OrderController(Environment environment, OrderService orderService,KafkaProducer kafkaProducer,OrderProducer orderProducer) {
        this.environment = environment;
        this.orderService = orderService;
        this.kafkaProducer = kafkaProducer;
        this.orderProducer = orderProducer;
    }

    @GetMapping("/heath_check")
    public String status(HttpServletRequest request){
        return "It's Working Order Service PORT "+request.getLocalPort();
    }

    @PostMapping("/{userId}/orders")
    public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId, @RequestBody RequestOrder orderDetails){
        log.info("Before Call ADD Orders data");
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        OrderDTO orderDTO = modelMapper.map(orderDetails, OrderDTO.class);
        orderDTO.setUserId(userId);

        OrderDTO createdOrderDTO = orderService.createOrder(orderDTO);
        ResponseOrder responseOrder = modelMapper.map(createdOrderDTO,ResponseOrder.class);

//        orderDTO.setOrderId(UUID.randomUUID().toString());
//        orderDTO.setTotalPrice(orderDetails.getQty() * orderDetails.getUnitPrice());

        // 카탈로그 현행화 카프카
        kafkaProducer.send("example-catalig-topic",orderDTO);
//        orderProducer.send("orders",orderDTO);
//        ResponseOrder responseOrder = modelMapper.map(orderDTO,ResponseOrder.class);

        log.info("After Call ADD Orders data");

        return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable("userId") String userId) throws Exception {

        log.info("Before Call Orders data");
        Iterable<OrderEntity> orderList =  orderService.getOrdersByUserId(userId);


        List<ResponseOrder> result = new ArrayList<>();
        orderList.forEach( v -> {
            result.add(new ModelMapper().map(v,ResponseOrder.class));
        });

//        try {
//            Thread.sleep(1000);
//            throw new Exception("장애 발생");
//        } catch (InterruptedException e) {
//            log.warn(e.getMessage());
//        }

        log.info("After Call Orders data");

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

}
