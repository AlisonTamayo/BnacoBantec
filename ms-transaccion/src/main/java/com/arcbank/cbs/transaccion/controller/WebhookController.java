package com.arcbank.cbs.transaccion.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arcbank.cbs.transaccion.service.TransaccionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/transacciones/webhook")
@RequiredArgsConstructor
public class WebhookController {

        private final TransaccionService transaccionService;

        @SuppressWarnings("unchecked")
        @PostMapping
        public ResponseEntity<?> recibirTransferenciaEntrante(@RequestBody Map<String, Object> payload) {
                log.info("Webhook recibido (Nexus Standard): {}", payload);

                try {
                        Map<String, Object> header = (Map<String, Object>) payload.get("header");
                        Map<String, Object> body = (Map<String, Object>) payload.get("body");

                        if (header == null || body == null) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "status", "NACK",
                                                "error", "Formato de mensaje inválido"));
                        }

                        String instructionId = body.get("instructionId") != null
                                        ? body.get("instructionId").toString()
                                        : null;

                        Map<String, Object> creditor = (Map<String, Object>) body.get("creditor");
                        String cuentaDestino = (creditor != null && creditor.get("accountId") != null)
                                        ? creditor.get("accountId").toString()
                                        : null;

                        String bancoOrigen = header.get("originatingBankId") != null
                                        ? header.get("originatingBankId").toString()
                                        : "DESCONOCIDO";

                        BigDecimal monto = BigDecimal.ZERO;
                        Map<String, Object> amount = (Map<String, Object>) body.get("amount");
                        if (amount != null && amount.get("value") != null) {
                                monto = new BigDecimal(amount.get("value").toString());
                        }

                        if (instructionId == null || cuentaDestino == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "status", "NACK",
                                                "error", "Datos críticos faltantes"));
                        }

                        transaccionService.procesarTransferenciaEntrante(instructionId, cuentaDestino, monto,
                                        bancoOrigen);

                        return ResponseEntity.ok(Map.of(
                                        "status", "ACK",
                                        "message", "Transferencia procesada exitosamente",
                                        "instructionId", instructionId));

                } catch (Exception e) {
                        log.error("Error procesando webhook: {}", e.getMessage());
                        return ResponseEntity.status(422).body(Map.of(
                                        "status", "NACK",
                                        "error", e.getMessage()));
                }
        }
}
