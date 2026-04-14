package br.com.reservasala.api.models;

public class EventoDashboardStatsDTO {
    private long totalEventosFuturos;
    private long totalOrganizadores;
      private long totalEventosPassados;

    // Getters e Setters
    public long getTotalEventosFuturos() { return totalEventosFuturos; }
    public void setTotalEventosFuturos(long totalEventosFuturos) { this.totalEventosFuturos = totalEventosFuturos; }
    public long getTotalOrganizadores() { return totalOrganizadores; }
    public void setTotalOrganizadores(long totalOrganizadores) { this.totalOrganizadores = totalOrganizadores; }
      public long getTotalEventosPassados() { return totalEventosPassados; }
    public void setTotalEventosPassados(long totalEventosPassados) { this.totalEventosPassados = totalEventosPassados; }
    
}