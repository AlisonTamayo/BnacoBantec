package com.arcbank.cbs.transaccion.config;

import com.arcbank.cbs.transaccion.dto.SwitchTransferResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.lang.reflect.Type;

@Configuration
@Slf4j
public class SwitchFeignDecoderConfig {

    @Bean
    public Decoder feignDecoder() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());

        return new Decoder() {
            @Override
            public Object decode(Response response, Type type) throws IOException {
                if (response.body() == null) {
                    log.info("📥 Switch response body is null, status: {}", response.status());
                    // Si HTTP 2xx y sin body, es exitoso
                    if (response.status() >= 200 && response.status() < 300) {
                        return SwitchTransferResponse.builder()
                                .success(true)
                                .build();
                    }
                    return SwitchTransferResponse.builder()
                            .success(false)
                            .error(SwitchTransferResponse.ErrorBody.builder()
                                    .code("EMPTY_RESPONSE")
                                    .message("Switch returned empty response")
                                    .build())
                            .build();
                }

                String body = Util.toString(response.body().asReader(java.nio.charset.StandardCharsets.UTF_8));
                log.info("📥 Switch raw response - Status: {}, Body: {}", response.status(), body);

                // Si el body está vacío
                if (body == null || body.isBlank()) {
                    if (response.status() >= 200 && response.status() < 300) {
                        log.info("✅ Switch returned 2xx with empty body - treating as success");
                        return SwitchTransferResponse.builder()
                                .success(true)
                                .build();
                    }
                }

                try {
                    // Intentar parsear la respuesta
                    JsonNode rootNode = mapper.readTree(body);
                    
                    SwitchTransferResponse switchResp = mapper.treeToValue(rootNode, SwitchTransferResponse.class);
                    
                    // Si HTTP status es 2xx, marcar como exitoso
                    if (response.status() >= 200 && response.status() < 300) {
                        // Si no hay error explícito O si hay datos, es exitoso
                        if (switchResp.getError() == null || switchResp.getData() != null) {
                            log.info("✅ Switch returned 2xx - marking as success. Data: {}", switchResp.getData());
                            switchResp.setSuccess(true);
                        }
                        
                        // Verificar también si hay un campo "estado" o "status" que indique éxito
                        if (rootNode.has("estado")) {
                            String estado = rootNode.get("estado").asText();
                            if (estado.equalsIgnoreCase("COMPLETADA") || 
                                estado.equalsIgnoreCase("EXITOSA") ||
                                estado.equalsIgnoreCase("PROCESADA") ||
                                estado.equalsIgnoreCase("SUCCESS") ||
                                estado.equalsIgnoreCase("OK") ||
                                estado.equalsIgnoreCase("ACCEPTED")) {
                                log.info("✅ Switch returned estado='{}' - marking as success", estado);
                                switchResp.setSuccess(true);
                            }
                        }
                        
                        // También verificar si hay un campo "status"
                        if (rootNode.has("status")) {
                            String status = rootNode.get("status").asText();
                            if (status.equalsIgnoreCase("COMPLETED") || 
                                status.equalsIgnoreCase("SUCCESS") ||
                                status.equalsIgnoreCase("PROCESSED") ||
                                status.equalsIgnoreCase("ACCEPTED") ||
                                status.equalsIgnoreCase("OK")) {
                                log.info("✅ Switch returned status='{}' - marking as success", status);
                                switchResp.setSuccess(true);
                            }
                        }
                        
                        // Si la respuesta tiene instructionId o transactionId, probablemente sea exitosa
                        if (rootNode.has("instructionId") || rootNode.has("transactionId") || rootNode.has("id")) {
                            log.info("✅ Switch returned with transaction ID - marking as success");
                            switchResp.setSuccess(true);
                        }
                    }
                    
                    return switchResp;
                    
                } catch (Exception e) {
                    log.error("Error parsing Switch response: {} - Body was: {}", e.getMessage(), body);
                    
                    // Si es HTTP 2xx pero no pudimos parsear, aún así es éxito
                    if (response.status() >= 200 && response.status() < 300) {
                        log.info("✅ Switch returned 2xx but couldn't parse - treating as success anyway");
                        return SwitchTransferResponse.builder()
                                .success(true)
                                .build();
                    }
                    
                    return SwitchTransferResponse.builder()
                            .success(false)
                            .error(SwitchTransferResponse.ErrorBody.builder()
                                    .code("PARSE_ERROR")
                                    .message("Failed to parse Switch response: " + e.getMessage())
                                    .build())
                            .build();
                }
            }
        };
    }
}
