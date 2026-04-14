package br.com.reservasala.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CreateMeetingRequest {

    private String topic;
    private int type = 2;

    @JsonProperty("start_time")
    private String startTime;

    private int duration; 
    private String agenda;

    @JsonProperty("alternative_hosts")
    private String alternativeHosts;

    private MeetingSettings settings;

    // --- CLASSES ANINHADAS ---
    public static class MeetingSettings {

        @JsonProperty("join_before_host")
        private boolean joinBeforeHost = true;

        @JsonProperty("waiting_room")
        private boolean waitingRoom = true;

        @JsonProperty("meeting_authentication")
        private boolean meetingAuthentication = true;

        @JsonProperty("authentication_exception")
        private List<AuthenticationException> authenticationException;

        @JsonProperty("co_hosts")
        private List<CoHost> coHosts;

        @JsonProperty("waiting_room_participants")
        private int waitingRoomParticipants = 2; // 1: Todos, 2: Nenhum, 3: Apenas externos

        @JsonProperty("waiting_room_admit_users")
        private List<WaitingRoomAdmitUser> waitingRoomAdmitUsers;

        // --- Getters e Setters ---
        public boolean isJoinBeforeHost() { return joinBeforeHost; }
        public void setJoinBeforeHost(boolean joinBeforeHost) { this.joinBeforeHost = joinBeforeHost; }

        public boolean isWaitingRoom() { return waitingRoom; }
        public void setWaitingRoom(boolean waitingRoom) { this.waitingRoom = waitingRoom; }

        public boolean isMeetingAuthentication() { return meetingAuthentication; }
        public void setMeetingAuthentication(boolean meetingAuthentication) { this.meetingAuthentication = meetingAuthentication; }

        public List<AuthenticationException> getAuthenticationException() { return authenticationException; }
        public void setAuthenticationException(List<AuthenticationException> authenticationException) { this.authenticationException = authenticationException; }

        public List<CoHost> getCoHosts() { return coHosts; }
        public void setCoHosts(List<CoHost> coHosts) { this.coHosts = coHosts; }

        public int getWaitingRoomParticipants() { return waitingRoomParticipants; }
        public void setWaitingRoomParticipants(int waitingRoomParticipants) { this.waitingRoomParticipants = waitingRoomParticipants; }

        public List<WaitingRoomAdmitUser> getWaitingRoomAdmitUsers() { return waitingRoomAdmitUsers; }
        public void setWaitingRoomAdmitUsers(List<WaitingRoomAdmitUser> waitingRoomAdmitUsers) { this.waitingRoomAdmitUsers = waitingRoomAdmitUsers; }
    }

    public static class CoHost {
        @JsonProperty("email")
        private String email;

        public CoHost() {}
        public CoHost(String email) { this.email = email; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class AuthenticationException {
        @JsonProperty("email")
        private String email;
        @JsonProperty("name")
        private String name;

        public AuthenticationException() {}
        public AuthenticationException(String email, String name) {
            this.email = email;
            this.name = name;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class WaitingRoomAdmitUser {
        @JsonProperty("email")
        private String email;
        @JsonProperty("name")
        private String name;

        public WaitingRoomAdmitUser() {}
        public WaitingRoomAdmitUser(String email, String name) {
            this.email = email;
            this.name = name;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // --- Getters e Setters da classe principal ---
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getAgenda() { return agenda; }
    public void setAgenda(String agenda) { this.agenda = agenda; }

    public String getAlternativeHosts() { return alternativeHosts; }
    public void setAlternativeHosts(String alternativeHosts) { this.alternativeHosts = alternativeHosts; }

    public MeetingSettings getSettings() { return settings; }
    public void setSettings(MeetingSettings settings) { this.settings = settings; }
}
