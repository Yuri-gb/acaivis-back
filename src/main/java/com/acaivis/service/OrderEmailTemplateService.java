package com.acaivis.service;

import com.acaivis.model.Order;
import com.acaivis.model.OrderEmailEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderEmailTemplateService {

    private final String trackingBaseUrl;

    public OrderEmailTemplateService(
            @Value("${tracking.base-url:http://localhost:5173/rastrear-pedido}") String trackingBaseUrl) {
        this.trackingBaseUrl = trackingBaseUrl;
    }

    public EmailContent build(Order order, OrderEmailEvent event) {
        String subject;
        String intro;

        switch (event) {
            case PAYMENT_PENDING -> {
                subject = "Açaívis — Pedido " + order.getTrackingCode() + " aguardando pagamento";
                intro = "Seu pedido foi recebido e está aguardando a confirmação do pagamento.";
            }
            case PAYMENT_CONFIRMED -> {
                subject = "Açaívis — Pedido " + order.getTrackingCode() + " confirmado";
                intro = "Pagamento confirmado! Seu pedido já pode seguir para produção.";
            }
            case PREPARING -> {
                subject = "Açaívis — Seu pedido está sendo preparado";
                intro = "Seu pedido entrou em preparação.";
            }
            case READY -> {
                subject = "Açaívis — Seu pedido está pronto";
                intro = "Seu pedido está pronto e aguardando a próxima etapa da entrega.";
            }
            case OUT_FOR_DELIVERY -> {
                subject = "Açaívis — Seu pedido saiu para entrega";
                intro = "Seu pedido saiu para entrega.";
            }
            case DELIVERED -> {
                subject = "Açaívis — Pedido entregue";
                intro = "Seu pedido foi marcado como entregue. Obrigado por pedir com a Açaívis!";
            }
            case CANCELLED -> {
                subject = "Açaívis — Pedido " + order.getTrackingCode() + " cancelado";
                intro = "Seu pedido foi cancelado.";
            }
            default -> throw new IllegalArgumentException("Evento de e-mail não suportado: " + event);
        }

        boolean showTracking = event == OrderEmailEvent.PAYMENT_CONFIRMED
                || event == OrderEmailEvent.PREPARING
                || event == OrderEmailEvent.READY
                || event == OrderEmailEvent.OUT_FOR_DELIVERY;
        String trackingUrl = trackingBaseUrl + "?codigo=" + order.getTrackingCode();

        StringBuilder items = new StringBuilder();
        order.getItems().forEach(item -> items
                .append("<tr><td style=\"padding:8px 0;\">")
                .append(item.getQuantity()).append("x ")
                .append(escape(item.getProductName())).append(" ")
                .append(escape(item.getSize()))
                .append("</td><td style=\"padding:8px 0;text-align:right;\">R$ ")
                .append(item.getSubtotal())
                .append("</td></tr>"));

        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <body style="margin:0;background:#111;color:#eee;font-family:Arial,sans-serif;">
                  <div style="max-width:620px;margin:0 auto;padding:32px 20px;">
                    <div style="background:#191919;border:1px solid #303030;border-radius:18px;padding:28px;">
                      <h1 style="margin:0 0 18px;color:#b66cff;">Açaívis</h1>
                      <p style="font-size:16px;">Olá, %s!</p>
                      <p style="font-size:16px;line-height:1.6;">%s</p>
                      <div style="margin:24px 0;padding:18px;background:#111;border-radius:12px;">
                        <strong>Pedido %s</strong>
                        <table style="width:100%%;border-collapse:collapse;margin-top:14px;">%s</table>
                        <div style="border-top:1px solid #333;margin-top:14px;padding-top:14px;text-align:right;">
                          <strong>Total: R$ %s</strong>
                        </div>
                      </div>
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escape(order.getCustomerName()),
                intro,
                escape(order.getTrackingCode()),
                items,
                order.getTotal(),
                showTracking
                        ? ("""<a href="%s" style="display:inline-block;background:#9b4dff;color:#fff;text-decoration:none;padding:13px 18px;border-radius:10px;font-weight:bold;">Acompanhar pedido</a>
                           <p style="font-size:13px;color:#999;margin-top:24px;">Código de acompanhamento: <strong style="color:#ddd;">%s</strong></p>""").formatted(trackingUrl, escape(order.getTrackingCode()))
                        : ""
        );

        return new EmailContent(subject, html);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record EmailContent(String subject, String html) {}
}
