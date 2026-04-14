package br.com.reservasala.api.services;

import br.com.reservasala.api.models.Evento;
import br.com.reservasala.api.models.GraphCalendarEventDTO;
import br.com.reservasala.api.models.GraphTokenResponse;
import br.com.reservasala.api.models.Reserva;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GraphApiService {

    @Value("${microsoft.graph.tenant-id}")
    private String tenantId;
    @Value("${microsoft.graph.client-id}")
    private String clientId;
    @Value("${microsoft.graph.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // Método para criar o evento no calendário
    public void createCalendarEventForEvento(String userEmail, Evento evento) throws Exception {
        String accessToken = getAccessToken();

        GraphCalendarEventDTO calendarEvent = new GraphCalendarEventDTO();
        calendarEvent.setSubject(evento.getTitle());

        GraphCalendarEventDTO.ItemBody body = new GraphCalendarEventDTO.ItemBody();
        String description = evento.getDescription() != null ? evento.getDescription() : "";
        body.content = "Evento agendado através do Sistema de Eventos CCAA.<br><br>" +
                       "<strong>Descrição:</strong> " + description + "<br><br>" +
                       "<strong>Link para entrar:</strong> <a href='" + evento.getLink() + "'>Clique aqui</a>";
        calendarEvent.setBody(body);

        GraphCalendarEventDTO.TimeSlot start = new GraphCalendarEventDTO.TimeSlot();
        start.dateTime = evento.getStartTime().toString();
        calendarEvent.setStart(start);

        GraphCalendarEventDTO.TimeSlot end = new GraphCalendarEventDTO.TimeSlot();
        end.dateTime = evento.getEndTime().toString();
        calendarEvent.setEnd(end);

        GraphCalendarEventDTO.Location location = new GraphCalendarEventDTO.Location();
        location.displayName = "Online via Zoom";
        calendarEvent.setLocation(location);

        GraphCalendarEventDTO.EmailAddress emailAddress = new GraphCalendarEventDTO.EmailAddress();
        emailAddress.address = evento.getOrganizerEmail();
        emailAddress.name = evento.getOrganizerEmail();
        GraphCalendarEventDTO.Attendee attendee = new GraphCalendarEventDTO.Attendee();
        attendee.emailAddress = emailAddress;
        calendarEvent.setAttendees(List.of(attendee));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GraphCalendarEventDTO> requestEntity = new HttpEntity<>(calendarEvent, headers);
        String url = "https://graph.microsoft.com/v1.0/users/" + userEmail + "/calendar/events";
        restTemplate.postForEntity(url, requestEntity, String.class);
    }

      public void createCalendarEventForReserva(String userEmail, Reserva reserva) throws Exception {
        String accessToken = getAccessToken();
        GraphCalendarEventDTO calendarEvent = new GraphCalendarEventDTO();
        calendarEvent.setSubject(reserva.getTitle());

        GraphCalendarEventDTO.ItemBody body = new GraphCalendarEventDTO.ItemBody();
        body.content = "Reserva de sala efetuada através do Sistema de Reservas CCAA.<br><br>" +
                       "<strong>Descrição:</strong> " + reserva.getDescription();
        calendarEvent.setBody(body);

        GraphCalendarEventDTO.TimeSlot start = new GraphCalendarEventDTO.TimeSlot();
        start.dateTime = reserva.getStartTime().toString();
        calendarEvent.setStart(start);

        GraphCalendarEventDTO.TimeSlot end = new GraphCalendarEventDTO.TimeSlot();
        end.dateTime = reserva.getEndTime().toString();
        calendarEvent.setEnd(end);

        GraphCalendarEventDTO.Location location = new GraphCalendarEventDTO.Location();
        location.displayName = reserva.getSala().getNome();
        calendarEvent.setLocation(location);

        GraphCalendarEventDTO.EmailAddress emailAddress = new GraphCalendarEventDTO.EmailAddress();
        emailAddress.address = reserva.getOrganizerEmail();
        emailAddress.name = reserva.getOrganizerEmail();
        GraphCalendarEventDTO.Attendee attendee = new GraphCalendarEventDTO.Attendee();
        attendee.emailAddress = emailAddress;
        calendarEvent.setAttendees(List.of(attendee));

        sendCalendarRequest(userEmail, accessToken, calendarEvent);
    }

    // Método privado para enviar a requisição, evitando duplicação de código
    private void sendCalendarRequest(String userEmail, String accessToken, GraphCalendarEventDTO event) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GraphCalendarEventDTO> requestEntity = new HttpEntity<>(event, headers);
        String url = "https://graph.microsoft.com/v1.0/users/" + userEmail + "/calendar/events";
        restTemplate.postForEntity(url, requestEntity, String.class);
        
    }

    

    // Método para obter o token de acesso da Microsoft
    private String getAccessToken() {
        String url = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("scope", "https://graph.microsoft.com/.default");
        map.add("client_secret", clientSecret);
        map.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<GraphTokenResponse> response = restTemplate.postForEntity(url, request, GraphTokenResponse.class);
        return response.getBody().getAccessToken();
    }
}