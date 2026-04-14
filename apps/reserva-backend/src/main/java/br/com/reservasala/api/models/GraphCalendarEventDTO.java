package br.com.reservasala.api.models;

import java.util.List;

public class GraphCalendarEventDTO {
    private String subject;
    private ItemBody body;
    private TimeSlot start;
    private TimeSlot end;
    private Location location;
    private List<Attendee> attendees;



    public static class ItemBody {
        public String contentType = "HTML";
        public String content;
    }
    public static class TimeSlot {
        public String dateTime;
        public String timeZone = "America/Sao_Paulo"; // Ajuste se necessário
    }
    public static class Location {
        public String displayName;
    }
    public static class Attendee {
        public EmailAddress emailAddress;
        public String type = "required";
    }
    public static class EmailAddress {
        public String address;
        public String name;
    }

     public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public ItemBody getBody() {
        return body;
    }

    public void setBody(ItemBody body) {
        this.body = body;
    }

    public TimeSlot getStart() {
        return start;
    }

    public void setStart(TimeSlot start) {
        this.start = start;
    }

    public TimeSlot getEnd() {
        return end;
    }

    public void setEnd(TimeSlot end) {
        this.end = end;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<Attendee> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<Attendee> attendees) {
        this.attendees = attendees;
    }

}
