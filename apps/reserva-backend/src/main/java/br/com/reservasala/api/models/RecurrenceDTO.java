package br.com.reservasala.api.models;


import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;


public class RecurrenceDTO {
public enum Frequency { DAILY, WEEKLY, MONTHLY }
public enum EndType { NEVER, UNTIL_DATE, AFTER_OCCURRENCES }


@NotNull
private Frequency frequency; // Para começar, implemente WEEKLY – as outras ficam como extensão


@Min(1)
private int interval = 1; // a cada X dias/semanas/meses


// WEEKLY
// 1=Seg,2=Ter,3=Qua,4=Qui,5=Sex,6=Sáb,7=Dom (padrão Java DayOfWeek.getValue())
@NotNull
@Size(min = 1)
private List<Integer> weekDays;


@NotNull
private EndType endType = EndType.UNTIL_DATE;


private LocalDate untilDate; // se endType == UNTIL_DATE


@Min(1)
private Integer maxOccurrences; // se endType == AFTER_OCCURRENCES


// getters/setters
public Frequency getFrequency() { return frequency; }
public void setFrequency(Frequency frequency) { this.frequency = frequency; }
public int getInterval() { return interval; }
public void setInterval(int interval) { this.interval = interval; }
public List<Integer> getWeekDays() { return weekDays; }
public void setWeekDays(List<Integer> weekDays) { this.weekDays = weekDays; }
public EndType getEndType() { return endType; }
public void setEndType(EndType endType) { this.endType = endType; }
public LocalDate getUntilDate() { return untilDate; }
public void setUntilDate(LocalDate untilDate) { this.untilDate = untilDate; }
public Integer getMaxOccurrences() { return maxOccurrences; }
public void setMaxOccurrences(Integer maxOccurrences) { this.maxOccurrences = maxOccurrences; }
}