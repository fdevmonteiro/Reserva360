package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.InscricaoEvento;
import br.com.reservasala.api.models.InscricaoRequestDTO;
import br.com.reservasala.api.repository.EventoRepository;
import br.com.reservasala.api.repository.InscricaoRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InscricaoService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;

    public InscricaoService(InscricaoRepository inscricaoRepository, EventoRepository eventoRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
    }

    /**
     * Para séries recorrentes, resolve sempre para o evento pai.
     * Assim toda a série compartilha uma única lista de inscrições.
     */
    private Evento resolveRoot(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
        Evento parent = evento.getParentEventId();
        if (parent != null && parent.getId() != null) {
            return eventoRepository.findById(parent.getId()).orElse(evento);
        }
        return evento;
    }

    /** Inscreve um participante e retorna o link do Zoom após o cadastro. */
    @Transactional
    public Map<String, Object> inscrever(Long eventoId, InscricaoRequestDTO dto) {
        Evento evento = resolveRoot(eventoId);

        if (!evento.isRegistrationRequired()) {
            throw new IllegalStateException("Este evento não exige inscrição prévia.");
        }

        if (inscricaoRepository.existsByEventoIdAndEmail(evento.getId(), dto.getEmail())) {
            throw new IllegalStateException("Este e-mail já está inscrito neste evento.");
        }

        InscricaoEvento inscricao = new InscricaoEvento();
        inscricao.setEvento(evento);
        inscricao.setNome(dto.getNome());
        inscricao.setEmail(dto.getEmail());
        inscricao.setTelefone(dto.getTelefone());
        inscricao.setNomeUnidade(dto.getNomeUnidade());
        inscricao.setCargo(dto.getCargo());
        inscricaoRepository.save(inscricao);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mensagem", "Inscrição realizada com sucesso!");
        result.put("eventoTitulo", evento.getTitle());
        result.put("joinUrl", evento.getLink());
        return result;
    }

    /** Retorna informações públicas do evento para a página de inscrição. */
    public Map<String, Object> getEventoInfo(Long eventoId) {
        Evento evento = resolveRoot(eventoId);

        if (!evento.isRegistrationRequired()) {
            throw new IllegalStateException("Este evento não possui inscrição pelo sistema.");
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", evento.getId());
        info.put("titulo", evento.getTitle());
        info.put("descricao", evento.getDescription());
        info.put("startTime", evento.getStartTime());
        info.put("endTime", evento.getEndTime());
        info.put("organizerEmail", evento.getOrganizerEmail());
        info.put("status", evento.getStatus());
        return info;
    }

    /** Lista todos os inscritos de um evento (resolve para o pai em séries). */
    public List<InscricaoEvento> listarPorEvento(Long eventoId) {
        Evento evento = resolveRoot(eventoId);
        return inscricaoRepository.findByEventoIdOrderByCreatedAtDesc(evento.getId());
    }

    /** Conta inscritos de um evento (resolve para o pai em séries). */
    public long contarPorEvento(Long eventoId) {
        Evento evento = resolveRoot(eventoId);
        return inscricaoRepository.countByEventoId(evento.getId());
    }

    /** Gera planilha Excel com todos os inscritos do evento (resolve para o pai em séries). */
    public byte[] exportarExcel(Long eventoId) {
        Evento evento = resolveRoot(eventoId);

        List<InscricaoEvento> inscritos = inscricaoRepository.findByEventoIdOrderByCreatedAtDesc(evento.getId());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String sheetName = evento.getTitle().length() > 31
                    ? evento.getTitle().substring(0, 31)
                    : evento.getTitle();
            Sheet sheet = workbook.createSheet(sheetName);

            // Estilo do cabeçalho
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Cabeçalhos
            String[] headers = {"Nome", "E-mail", "Telefone", "Nome da Unidade", "Cargo", "Data de Inscrição"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dados
            int rowNum = 1;
            for (InscricaoEvento i : inscritos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nullSafe(i.getNome()));
                row.createCell(1).setCellValue(nullSafe(i.getEmail()));
                row.createCell(2).setCellValue(nullSafe(i.getTelefone()));
                row.createCell(3).setCellValue(nullSafe(i.getNomeUnidade()));
                row.createCell(4).setCellValue(nullSafe(i.getCargo()));
                row.createCell(5).setCellValue(i.getCreatedAt() != null ? i.getCreatedAt().format(FMT) : "");
            }

            // Ajusta largura das colunas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar planilha Excel.", e);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
