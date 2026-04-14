package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.EventoReport;
import br.com.reservasala.api.models.EventoReportParticipant;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class ReportPdfService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] buildReportPdf(Evento evento, EventoReport report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Relatório da Reunião", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            doc.add(new Paragraph("Título: " + safe(evento.getTitle())));
            doc.add(new Paragraph("Organizador: " + safe(evento.getOrganizerEmail())));
            doc.add(new Paragraph("Host: " + (evento.getHostZoomAccount() != null ? safe(evento.getHostZoomAccount().getAccountMail()) : "-")));
            doc.add(new Paragraph("ID da Reunião: " + (evento.getZoomMeetingId() == null ? "-" : evento.getZoomMeetingId())));
            doc.add(new Paragraph("Link: " + safe(report.getRecordingUrl() == null ? evento.getLink() : report.getRecordingUrl())));
            doc.add(new Paragraph("Status gravação: " + safe(report.getRecordingStatus())));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Início real: " + format(report.getActualStartTime())));
            doc.add(new Paragraph("Término real: " + format(report.getActualEndTime())));
            doc.add(new Paragraph("Duração (min): " + safe(report.getDurationMinutes())));
            doc.add(new Paragraph("Participantes: total " + safe(report.getTotalParticipants()) + " / pico " + safe(report.getPeakParticipants())));
            doc.add(new Paragraph("Minutos totais (soma): " + safe(report.getTotalMinutes())));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            addHeader(table, "Nome");
            addHeader(table, "Email");
            addHeader(table, "Entrada");
            addHeader(table, "Saída");
            addHeader(table, "Duração (min)");

            for (EventoReportParticipant p : report.getParticipants()) {
                table.addCell(safe(p.getName()));
                table.addCell(safe(p.getEmail()));
                table.addCell(format(p.getJoinTime()));
                table.addCell(format(p.getLeaveTime()));
                table.addCell(safe(p.getDurationMinutes()));
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF do relatório", e);
        }
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setGrayFill(0.9f);
        table.addCell(cell);
    }

    private String format(java.time.LocalDateTime ldt) {
        if (ldt == null) return "-";
        return ldt.atZone(ZoneId.of("UTC")).withZoneSameInstant(DISPLAY_ZONE).format(DATE_FMT);
    }

    private String safe(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }
}
