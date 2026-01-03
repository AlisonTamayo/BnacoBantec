package com.arcbank.cbs.transaccion.client;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.arcbank.cbs.transaccion.dto.SwitchTransferRequest;
import com.arcbank.cbs.transaccion.dto.SwitchTransferResponse;
import com.arcbank.cbs.transaccion.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SwitchClientService {

    private final RestTemplate restTemplate;

    @Value("${app.switch.url:http://localhost:8081}")
    private String switchUrl;

    @Value("${app.switch.apikey:DEFAULT_API_KEY}")
    private String apiKey;

    public SwitchClientService(org.springframework.boot.web.client.RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public SwitchTransferResponse enviarTransferencia(SwitchTransferRequest request) {
        try {
            String url = switchUrl + "/api/switch/v1/transferir";
            log.info("Sending transfer to switch: {}", url);

            if (request.getBody().getInstructionId() == null) {
                request.getBody().setInstructionId(UUID.randomUUID().toString());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("apikey", apiKey);

            HttpEntity<SwitchTransferRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<SwitchTransferResponse> response = restTemplate.postForEntity(
                    URI.create(url),
                    entity,
                    SwitchTransferResponse.class);

            if (response.getBody() == null) {
                throw new BusinessException("Switch returned empty body");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error("Error sending transfer to switch: {}", e.getMessage());
            return SwitchTransferResponse.builder()
                    .success(false)
                    .error(SwitchTransferResponse.ErrorBody.builder()
                            .code("SYSTEM_ERROR")
                            .message(e.getMessage())
                            .build())
                    .build();
        }
    }
}
