package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.EventoReport;
import br.com.reservasala.api.models.Reserva;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username}")
    private String senderEmail;

    // URL pública base do backend (ajuste no application.properties: app.base-url)
    @Value("${app.base-url:http://reserva360hml.grupoccaa.com.br:8081}")
    private String appBaseUrl;

    // URL base do frontend para páginas públicas (ex.: confirmação por email)
    @Value("${app.frontend-url:${app.base-url:http://reserva360hml.grupoccaa.com.br:8081}}")
    private String frontendBaseUrl;

    // ===================== RESERVAS (seu código original mantido) =====================

    @Async
    public void sendReservationConfirmationEmail(Reserva reserva) {
        try {
            String confirmUrl = appBaseUrl + "/reservas/confirmar?token=" + url(reserva.getConfirmationToken());
            String cancelUrl  = appBaseUrl + "/reservas/cancelar?token="  + url(reserva.getConfirmationToken());

            String htmlMsg = """
                <h3>Confirmação de Pré-Reserva</h3>
                <p>Olá,</p>
                <p>Uma reserva para a sala <strong>%s</strong> foi solicitada em seu nome.</p>
                <p><strong>Assunto:</strong> %s</p>
                <p><strong>Horário:</strong> de %s até %s</p>
                <hr>
                <p><strong>Você confirma esta reserva?</strong></p>
                <a href="%s" style="display:inline-block;background:#28a745;color:#fff;padding:12px 25px;margin:5px;text-decoration:none;border-radius:5px;font-weight:bold;">Sim, Confirmar</a>
                <a href="%s" style="display:inline-block;background:#dc3545;color:#fff;padding:12px 25px;margin:5px;text-decoration:none;border-radius:5px;font-weight:bold;">Não, Cancelar</a>
                <br/><br/><p>Obrigado,<br/>Sistema de Reservas CCAA</p>
            """.formatted(
                safe(reserva.getSala().getNome()),
                safe(reserva.getTitle()),
                safe(String.valueOf(reserva.getStartTime())),
                safe(String.valueOf(reserva.getEndTime())),
                confirmUrl, cancelUrl
            );

            sendHtml(reserva.getOrganizerEmail(),
                    "Ação Necessária: Confirme sua Reserva para a Sala " + safe(reserva.getSala().getNome()),
                    htmlMsg);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email de confirmação de reserva: " + e.getMessage());
        }
    }

    @Async
    public void sendReservationZoomDetailsEmail(Reserva reserva, String zoomPasscode) {
        try {
            String htmlMsg = """
                <h3>Link da Reunião Online</h3>
                <p>Olá,</p>
                <p>Conforme solicitado, uma reunião no Zoom foi criada para a sua reserva da sala <strong>%s</strong>.</p>
                <p><strong>Assunto:</strong> %s</p>
                <hr>
                <p><strong>Link para compartilhar com os participantes</strong></p>
                <p><a href="%s">%s</a></p>
                <br>
                <p><strong>Opção 2: Entrar com ID e Senha</strong></p>
                <p>ID da Reunião: <strong>%s</strong></p>
                <p>Senha de Acesso: <strong>%s</strong></p>
                <br/><p>Obrigado,<br/>Sistema de Reservas CCAA</p>
            """.formatted(
                safe(reserva.getSala().getNome()),
                safe(reserva.getTitle()),
                safe(reserva.getLink()), safe(reserva.getLink()),
                safe(String.valueOf(reserva.getZoomMeetingId())),
                safe(zoomPasscode)
            );

            sendHtml(reserva.getOrganizerEmail(),
                    "✅ Link da Reunião Zoom para: " + safe(reserva.getTitle()),
                    htmlMsg);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email com detalhes do Zoom para reserva: " + e.getMessage());
        }
    }

    // ===================== EVENTOS: NOVO FLUXO =====================

    /** Evento ÚNICO (PENDING) → envia link para confirmar evento único */
    @Async
    public void sendSingleEventConfirmationEmail(Evento evento) {
        try {
            String confirmUrl = frontendBaseUrl + "/eventos/confirmar?token=" + url(evento.getConfirmationToken());
            String cancelUrl  = frontendBaseUrl + "/eventos/cancelar?token="  + url(evento.getConfirmationToken());

            String htmlMsg = """
                <h3>Ação Necessária: Confirme seu Evento</h3>
                <p>Olá,</p>
                <p>Um evento online (<strong>%s</strong>) foi pré-agendado em seu nome.</p>
                <p>Para confirmar ou cancelar, utilize os links abaixo:</p>
                <a href="%s" style="display:inline-block;background:#28a745;color:#fff;padding:12px 25px;margin:5px;text-decoration:none;border-radius:5px;font-weight:bold;">Sim, Confirmar</a>
                <a href="%s" style="display:inline-block;background:#dc3545;color:#fff;padding:12px 25px;margin:5px;text-decoration:none;border-radius:5px;font-weight:bold;">Não, Cancelar</a>
            """.formatted(safe(evento.getTitle()), confirmUrl, cancelUrl);

            sendHtml(evento.getOrganizerEmail(),
                    "Confirme seu Evento: " + safe(evento.getTitle()),
                    htmlMsg);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email de confirmação (evento único): " + e.getMessage());
        }
    }

    /** Série RECORRENTE (PENDING no PAI) → envia link para confirmar a série inteira */
    @Async
    public void sendSeriesConfirmationEmail(Evento parent) {
        try {
            String confirmUrl = frontendBaseUrl + "/eventos/confirmar-recorrente?token=" + url(parent.getConfirmationToken());
            String cancelUrl  = frontendBaseUrl + "/eventos/cancelar?token=" + url(parent.getConfirmationToken()); // se quiser permitir cancelar a pré-reserva

            String htmlMsg = """
                <h3>Ação Necessária: Confirme sua Série de Eventos</h3>
                <p>Olá,</p>
                <p>Uma <strong>série recorrente</strong> do evento <strong>%s</strong> foi pré-agendada.</p>
                <p>Para confirmar todas as ocorrências, utilize o botão abaixo:</p>
                <a href="%s" style="display:inline-block;background:#0d6efd;color:#fff;padding:12px 25px;margin:5px;text-decoration:none;border-radius:5px;font-weight:bold;">Confirmar Série</a>
                <p style="margin-top:12px;">Se não foi você, você pode cancelar:</p>
                <a href="%s" style="display:inline-block;background:#dc3545;color:#fff;padding:10px 18px;margin:5px;text-decoration:none;border-radius:5px;font-weight:bold;">Cancelar Pré-Reserva</a>
            """.formatted(safe(parent.getTitle()), confirmUrl, cancelUrl);

            sendHtml(parent.getOrganizerEmail(),
                    "Confirme sua Série de Eventos: " + safe(parent.getTitle()),
                    htmlMsg);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email de confirmação (série): " + e.getMessage());
        }
    }

    /** Detalhes após confirmação (único ou série) */
    @Async
    public void sendEventDetailsEmail(Evento evento, String zoomPasscode) {
        try {
            String pass = (zoomPasscode == null ? "" : zoomPasscode);
            String htmlMsg = """
                <h3>Evento Agendado com Sucesso!</h3>
                <p>Olá,</p>
                <p>O seu evento <strong>%s</strong> foi confirmado.</p>
                <hr>
                <h4>Detalhes para os Participantes</h4>
                <p><strong>Link:</strong> <a href="%s">%s</a></p>
                <p><strong>ID da Reunião:</strong> %s</p>
                %s
                <br/><p>Obrigado,<br/>Sistema de Eventos CCAA</p>
            """.formatted(
                safe(evento.getTitle()),
                safe(evento.getLink()), safe(evento.getLink()),
                safe(String.valueOf(evento.getZoomMeetingId())),
                pass.isBlank() ? "" : ("<p><strong>Senha de Acesso:</strong> " + safe(pass) + "</p>")
            );

            sendHtml(evento.getOrganizerEmail(),
                    "✅ Evento Criado: " + safe(evento.getTitle()),
                    htmlMsg);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email de detalhes do evento: " + e.getMessage());
        }
    }

    // ===================== CANCELAMENTO (evento único) =====================

    @Async
    public void sendEventCancellationEmail(Evento evento) {
        try {
            String htmlMsg = """
                <h3>Evento Cancelado</h3>
                <p>Olá,</p>
                <p>O evento <strong>%s</strong> foi cancelado.</p>
                <p>Obrigado por usar nosso sistema de eventos!</p>
            """.formatted(safe(evento.getTitle()));

            sendHtml(evento.getOrganizerEmail(),
                    "Evento Cancelado: " + safe(evento.getTitle()),
                    htmlMsg);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email de cancelamento de evento: " + e.getMessage());
        }
    }

    // ===================== helpers =====================

    private void sendHtml(String to, String subject, String html) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
        h.setTo(to);
        h.setFrom(senderEmail);
        h.setSubject(subject);
        h.setText(html, true);
        mailSender.send(msg);
    }

    private static String url(String s) {
        try { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    public void sendEventReportEmail(Evento evento, EventoReport report, byte[] pdfBytes) throws MessagingException {
        if (pdfBytes == null || pdfBytes.length == 0) return;

        String htmlMsg = """
            <h3>Relatório da Reunião</h3>
            <p>Olá,</p>
            <p>Segue em anexo o relatório em PDF da reunião <strong>%s</strong>.</p>
            <p><strong>Início real:</strong> %s<br>
               <strong>Término real:</strong> %s<br>
               <strong>Duração (min):</strong> %s<br>
               <strong>Participantes (pico):</strong> %s</p>
            <p>%s</p>
        """.formatted(
                safe(evento.getTitle()),
                format(report.getActualStartTime()),
                format(report.getActualEndTime()),
                safeInt(report.getDurationMinutes()),
                safeInt(report.getPeakParticipants()),
                report.getRecordingUrl() == null ? "" : ("<strong>Gravação:</strong> <a href='" + safe(report.getRecordingUrl()) + "'>link</a>")
        );

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setTo(evento.getOrganizerEmail());
        // Envia cópia para o co-host, se houver e for diferente do organizador
        String coHost = evento.getCoHostEmail();
        if (coHost != null && !coHost.isBlank() && !coHost.equalsIgnoreCase(evento.getOrganizerEmail())) {
            h.setCc(coHost);
        }
        h.setFrom(senderEmail);
        h.setSubject("Relatório da reunião: " + safe(evento.getTitle()));
        h.setText(htmlMsg, true);
        h.addAttachment("relatorio-reuniao.pdf", () -> new java.io.ByteArrayInputStream(pdfBytes));
        mailSender.send(msg);
    }

    private String format(java.time.LocalDateTime ldt) {
        if (ldt == null) return "-";
        return ldt.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String safeInt(Integer i) { return i == null ? "-" : String.valueOf(i); }
}
