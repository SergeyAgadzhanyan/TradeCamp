package com.tradecamp.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradecamp.models.dto.*;
import com.tradecamp.models.exception.ApplicationException;
import com.tradecamp.models.model.RabbitRequest;
import com.tradecamp.models.model.RabbitRequestType;
import com.tradecamp.models.model.RabbitResponse;
import com.tradecamp.web.configuration.MyUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tradecamp.models.util.RabbitVar.USER_EXCHANGE;
import static com.tradecamp.models.util.RabbitVar.USER_ROUTING_KEY;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final ObjectMapper objectMapper;

    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;


    public UserDtoGet create(UserDtoCreate userDtoCreate) {
        try {
            userDtoCreate.setPassword(passwordEncoder.encode(userDtoCreate.getPassword()));
            RabbitRequest request = RabbitRequest.builder()
                    .type(RabbitRequestType.USER_CREATE)
                    .message(objectMapper.writeValueAsString(userDtoCreate))
                    .build();
            String response = (String) rabbitTemplate.convertSendAndReceive(USER_EXCHANGE, USER_ROUTING_KEY,
                    objectMapper.writeValueAsString(request));
            RabbitResponse rabbitResponse = objectMapper.readValue(response, RabbitResponse.class);
            if (StringUtils.hasText(rabbitResponse.getError())) {
                throw new ApplicationException(rabbitResponse.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return objectMapper.readValue(rabbitResponse.getBody(), UserDtoGet.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public UserDto getByName(String name) {
        return find(UserDtoGet.builder().name(name).build());
    }

    private UserDto find(UserDtoGet userDtoGet) {
        log.info("Find request {}", userDtoGet.toString());
        try {
            RabbitRequest request = RabbitRequest.builder()
                    .type(RabbitRequestType.USER_FIND)
                    .message(objectMapper.writeValueAsString(userDtoGet)).build();
            String response = (String) rabbitTemplate.convertSendAndReceive(USER_EXCHANGE, USER_ROUTING_KEY,
                    objectMapper.writeValueAsString(request));
            RabbitResponse rabbitResponse = objectMapper.readValue(response, RabbitResponse.class);
            if (StringUtils.hasText(rabbitResponse.getError())) {
                throw new ApplicationException(rabbitResponse.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return objectMapper.readValue(rabbitResponse.getBody(), UserDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByName(String name) {
        delete(UserDtoGet.builder().name(name).build());
    }

    private void delete(UserDtoGet userDtoGet) {
        try {
            RabbitRequest request = RabbitRequest.builder()
                    .type(RabbitRequestType.USER_DELETE)
                    .message(objectMapper.writeValueAsString(userDtoGet)).build();

            String response = (String) rabbitTemplate.convertSendAndReceive(USER_EXCHANGE, USER_ROUTING_KEY,
                    objectMapper.writeValueAsString(request));

            RabbitResponse rabbitResponse = objectMapper.readValue(response, RabbitResponse.class);

            if (StringUtils.hasText(rabbitResponse.getError())) {
                throw new ApplicationException(rabbitResponse.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public TradeResultResponse setTradeResult(TradeResultRequest tradeResultRequest) {
        try {
            MyUserPrincipal currentUser = (MyUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            tradeResultRequest.setUserName(currentUser.getUsername());
            RabbitRequest request = RabbitRequest.builder()
                    .type(RabbitRequestType.USER_SET_TRADE_RESULT)
                    .message(objectMapper.writeValueAsString(tradeResultRequest))
                    .build();
            String response = (String) rabbitTemplate.convertSendAndReceive(USER_EXCHANGE, USER_ROUTING_KEY,
                    objectMapper.writeValueAsString(request));
            RabbitResponse rabbitResponse = objectMapper.readValue(response, RabbitResponse.class);
            if (StringUtils.hasText(rabbitResponse.getError())) {
                throw new ApplicationException(rabbitResponse.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return objectMapper.readValue(rabbitResponse.getBody(), TradeResultResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public TradeHistoryDtoResponse getLastStat() {
        MyUserPrincipal currentUser = (MyUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            RabbitRequest request = RabbitRequest.builder()
                    .type(RabbitRequestType.USER_GET_LAST_TRADE_RESULT)
                    .message(currentUser.getUsername())
                    .build();

            String response = (String) rabbitTemplate.convertSendAndReceive(USER_EXCHANGE, USER_ROUTING_KEY,
                    objectMapper.writeValueAsString(request));
            RabbitResponse rabbitResponse = objectMapper.readValue(response, RabbitResponse.class);
            if (StringUtils.hasText(rabbitResponse.getError())) {
                throw new ApplicationException(rabbitResponse.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return objectMapper.readValue(rabbitResponse.getBody(), TradeHistoryDtoResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public UserDto getCurrentUser() {
        MyUserPrincipal currentUser = (MyUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return getByName(currentUser.getUsername());
    }
}
