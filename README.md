# Reserva360

Plataforma corporativa de **reserva de salas e gestão de eventos** que elimina a dependência de processos manuais para criação de reuniões e controle de licenças.

## O problema que resolve

Empresas com múltiplas salas e licenças Zoom enfrentam dois gargalos:
- Criação manual de links de reunião para cada evento
- Conflito de licenças quando reuniões simultâneas excedem o número de contas disponíveis

## Como o Reserva360 resolve

- **Gerenciamento inteligente de licenças** — o sistema identifica automaticamente qual conta Zoom está livre no horário solicitado e a aloca para o evento, sem intervenção humana
- **Criação autônoma de reuniões** — ao reservar uma sala, a reunião Zoom e/ou o evento no Microsoft Calendar são criados automaticamente via API
- **Autonomia total para o usuário** — nenhum administrador precisa ser acionado para abrir uma sala virtual
- **Eventos recorrentes** — suporte a recorrência com verificação de disponibilidade de licença para todas as ocorrências
- **Relatórios automáticos** — consolidação de presença e uso de salas gerada em segundo plano via scheduler

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Spring Boot 3, Java 21, Spring Security + JWT |
| Frontend | React, TypeScript, Vite |
| Autenticação | Active Directory via LDAP |
| Integrações | Zoom API, Microsoft Graph API |
| Banco | MySQL |
| Monorepo | Nx |
