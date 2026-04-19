import { useState, useEffect, useMemo } from "react";
import { useAuth } from "@/contexts/AuthContext";
import {
  getEventos,
  getPastEventos,
  createEvento,
  updateEvento,
  deleteEvento,
  deleteEventoSerie,
  getMyEventos,
  getMyPastEventos,
  downloadReportPdf,
} from "@/services/eventoService";
import {
  listarInscritos,
  downloadInscritosExcel,
  type Inscricao,
} from "@/services/inscricaoService";
import { getEventDashboardStats } from "@/services/dashboardService";
import type { EventDashboardStats } from "@/services/dashboardService";
import type { Evento, EventoFormData } from "@/types/evento";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import {
  Plus,
  Calendar,
  Users,
  Clapperboard,
  History,
  Search,
  Clock,
  ChevronDown,
  ChevronRight,
  Repeat,
  Trash2,
  Loader2,
  Download,
  Copy,
  Check,
} from "lucide-react";
import EventoItem from "@/components/EventoItem";
import NovoEventoForm from "@/components/NovoEventoForm";
import { useToast } from "@/hooks/use-toast";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { parseEventoDate } from "@/utils/eventoTime";

// Util: extrai Date de diferentes formatos/campos
function getEventoDate(e: any): Date | null {
  const raw = e?.startTime || e?.inicio || e?.dataInicio || e?.data || e?.date || null;
  return parseEventoDate(raw);
}

type EventoGroup = {
  key: string;
  parentId: number | null;
  eventos: Evento[];
};

function toNumberOrNull(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function extractParentId(evento: Evento): number | null {
  const parent = evento?.parentEventId;
  if (parent == null) return null;
  if (typeof parent === "object") return toNumberOrNull((parent as { id?: unknown }).id);
  return toNumberOrNull(parent);
}

function buildEventoGroups(list: Evento[]): EventoGroup[] {
  const grouped = new Map<string, EventoGroup>();
  const referencedParentIds = new Set<number>();

  for (const evento of list) {
    const parentId = extractParentId(evento);
    if (parentId != null) referencedParentIds.add(parentId);
  }

  for (const evento of list) {
    const parentId = extractParentId(evento);
    const ownId = toNumberOrNull(evento?.id);
    const recurringRootId = parentId ?? (ownId != null && referencedParentIds.has(ownId) ? ownId : null);
    const key = recurringRootId != null
      ? `series-${recurringRootId}`
      : (ownId != null ? `single-${ownId}` : `single-${evento.title}-${evento.startTime}`);
    const existing = grouped.get(key);
    if (existing) {
      existing.eventos.push(evento);
      continue;
    }
    grouped.set(key, { key, parentId: recurringRootId, eventos: [evento] });
  }

  return Array.from(grouped.values());
}

// Util: mapeia status -> estilos
function StatusBadge({ status }: { status?: string }) {
  if (!status) return null;
  const s = status.toUpperCase();
  const map: Record<string, { variant?: "default" | "secondary" | "destructive" | "outline"; text: string }> = {
    CONFIRMED: { variant: "default", text: "Confirmado" },
    PENDING: { variant: "secondary", text: "Pendente" },
    CANCELLED: { variant: "destructive", text: "Cancelado" },
  };
  const cfg = map[s] || { variant: "outline", text: status } as any;
  return <Badge variant={cfg.variant}>{cfg.text}</Badge>;
}

// Skeleton simples para cards
function CardSkeleton() {
  return (
    <Card className="animate-pulse">
      <CardHeader>
        <div className="h-4 w-1/3 bg-muted rounded" />
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="h-3 w-2/3 bg-muted rounded" />
        <div className="h-3 w-1/2 bg-muted rounded" />
        <div className="h-8 w-24 bg-muted rounded" />
      </CardContent>
    </Card>
  );
}

const PERIODS = [
  { value: "all", label: "Todos" },
  { value: "week", label: "Próx. 7 dias" },
  { value: "month", label: "Este mês" },
  { value: "30", label: "Próx. 30 dias" },
];

const SORTS = [
  { value: "dateAsc", label: "Data ↑" },
  { value: "dateDesc", label: "Data ↓" },
  { value: "titleAsc", label: "Título A–Z" },
];

const STATUS = [
  { value: "CONFIRMED", label: "Confirmados" },
];

export default function EventosPage() {
  const [upcomingEventos, setUpcomingEventos] = useState<Evento[]>([]);
  const [pastEventos, setPastEventos] = useState<Evento[]>([]);
  const [stats, setStats] = useState<EventDashboardStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [editingEvento, setEditingEvento] = useState<Evento | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<number | null>(null);
  const [inscritosOpen, setInscritosOpen] = useState(false);
  const [inscritosEvento, setInscritosEvento] = useState<Evento | null>(null);
  const [inscritos, setInscritos] = useState<Inscricao[]>([]);
  const [inscritosLoading, setInscritosLoading] = useState(false);
  const [downloadingExcel, setDownloadingExcel] = useState(false);
  const [linkCopiado, setLinkCopiado] = useState(false);

  // toolbar state
  const [query, setQuery] = useState("");
  const [period, setPeriod] = useState("all");
  const [sortBy, setSortBy] = useState("dateAsc");
  const [statusFilter, setStatusFilter] = useState("all");
  const [expandedRecurringGroups, setExpandedRecurringGroups] = useState<Record<string, boolean>>({});
  const [deletingSeriesKey, setDeletingSeriesKey] = useState<string | null>(null);

  const { hasRole, isAuthenticated } = useAuth();
  const isAdmin = hasRole("ADMIN") || hasRole("ROLE_ADMIN");
  const [tick, setTick] = useState(0);
useEffect(() => {
  const id = setInterval(() => setTick((t) => t + 1), 60_000);
  return () => clearInterval(id);
}, []);
  const { toast } = useToast();

  function ensureArray<T>(value: unknown): T[] {
    return Array.isArray(value) ? (value as T[]) : [];
  }

  const loadData = async () => {
    setIsLoading(true);
    try {
      let upcomingData: Evento[] = [];
      let pastData: Evento[] = [];
      let statsData: EventDashboardStats | null = null;

      if (isAdmin) {
        // Admin: visão completa + dashboard
        [upcomingData, pastData, statsData] = await Promise.all([
          getEventos(),
          getPastEventos(),
          getEventDashboardStats(),
        ]);
      } else {
        // Usuário comum: só meus eventos, sem dashboard
        [upcomingData, pastData] = await Promise.all([
          getMyEventos(),
          getMyPastEventos(),
        ]);
        statsData = null;
      }

      setUpcomingEventos(ensureArray<Evento>(upcomingData));
      setPastEventos(ensureArray<Evento>(pastData));
      setStats(statsData);
    } catch (error) {
      console.error("Erro ao carregar dados da página de eventos:", error);
      toast({
        title: "Erro",
        description: "Não foi possível carregar os dados.",
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };
  

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin]);

  const handleSaveEvento = async (formData: EventoFormData, id?: number) => {
    setIsSaving(true);
    try {
      if (id) {
        await updateEvento(id, formData);
        toast({ title: "Sucesso!", description: "Evento atualizado." });
      } else {
        await createEvento(formData);
        toast({
          title: "Quase lá!",
          description:
            "Pré-agendamento realizado. Verifique seu email para confirmar o evento.",
        });
      }
      setIsFormOpen(false);
      setEditingEvento(null);
      await loadData();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível salvar o evento.",
        variant: "destructive",
      });
    } finally {
      setIsSaving(false);
    }
  };

  const requestDeleteEvento = (id: number) => setPendingDeleteId(id);

  function getApiErrorMessage(error: unknown): string {
    const fallback = "Não foi possível excluir a série.";
    if (typeof error === "object" && error !== null) {
      const maybeAxios = error as {
        response?: { data?: { error?: string } };
        message?: string;
      };
      if (maybeAxios.response?.data?.error) return maybeAxios.response.data.error;
      if (maybeAxios.message) return maybeAxios.message;
    }
    if (error instanceof Error) return error.message || fallback;
    if (typeof error === "string") return error;
    return fallback;
  }

  const confirmDeleteEvento = async () => {
    if (!pendingDeleteId) return;
    try {
      await deleteEvento(pendingDeleteId);
      toast({ title: "Sucesso!", description: "Evento excluído." });
      await loadData();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível excluir o evento.",
        variant: "destructive",
      });
    } finally {
      setPendingDeleteId(null);
    }
  };

  const handleAddNewClick = () => {
    setEditingEvento(null);
    setIsFormOpen(true);
  };

  const handleEditClick = (evento: Evento) => {
    setEditingEvento(evento);
    setIsFormOpen(true);
  };

  const handleViewRegistrants = async (evento: Evento) => {
    setInscritosEvento(evento);
    setInscritosOpen(true);
    setInscritosLoading(true);
    setInscritos([]);
    setLinkCopiado(false);
    try {
      const data = await listarInscritos(evento.id);
      setInscritos(data);
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível carregar inscritos deste evento.",
        variant: "destructive",
      });
    } finally {
      setInscritosLoading(false);
    }
  };

  const handleDownloadExcel = async () => {
    if (!inscritosEvento) return;
    setDownloadingExcel(true);
    try {
      await downloadInscritosExcel(inscritosEvento.id, inscritosEvento.title);
    } catch {
      toast({ title: "Erro", description: "Não foi possível gerar a planilha.", variant: "destructive" });
    } finally {
      setDownloadingExcel(false);
    }
  };

  const handleCopiarLink = () => {
    if (!inscritosEvento) return;
    const url = `${window.location.origin}/inscricao/evento/${inscritosEvento.id}`;
    navigator.clipboard.writeText(url).then(() => {
      setLinkCopiado(true);
      setTimeout(() => setLinkCopiado(false), 2500);
    });
  };

  const handleDownloadReport = async (evento: Evento) => {
    await downloadReportPdf(evento.id!, evento.title);
  };

  const handleDeleteSeries = async (eventoId: number, seriesKey: string) => {
    const confirmed = window.confirm(
      "Tem certeza que deseja excluir a série recorrente inteira?\n\nTodas as ocorrências serão removidas localmente e as reuniões associadas serão deletadas no Zoom."
    );
    if (!confirmed) return;

    setDeletingSeriesKey(seriesKey);
    try {
      await deleteEventoSerie(eventoId);
      toast({ title: "Série excluída", description: "A série recorrente e as reuniões no Zoom foram removidas." });
      await loadData();
    } catch (error) {
      toast({
        title: "Erro ao excluir série",
        description: getApiErrorMessage(error),
        variant: "destructive",
      });
    } finally {
      setDeletingSeriesKey(null);
    }
  };

  // ---- filtros/ordenação/busca
  function inPeriod(date: Date | null) {
    if (!date) return true;
    const now = new Date();
    if (period === "all") return true;
    if (period === "week") {
      const end = new Date(now);
      end.setDate(end.getDate() + 7);
      return date >= now && date <= end;
    }
    if (period === "month") {
      const start = new Date(now.getFullYear(), now.getMonth(), 1);
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);
      return date >= start && date <= end;
    }
    if (period === "30") {
      const end = new Date(now);
      end.setDate(end.getDate() + 30);
      return date >= now && date <= end;
    }
    return true;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  function matchesQuery(e: any) {
    if (!query.trim()) return true;
    const q = query.toLowerCase();
    const title = (e?.titulo || e?.title || e?.nome || "").toLowerCase();
    const organizer = (e?.organizador || e?.organizer || e?.criadoPor || "").toLowerCase();
    return title.includes(q) || organizer.includes(q);
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  function matchesStatus(e: any) {
    if (statusFilter === "all") return true;
    const s = (e?.status || "").toUpperCase();
    return s === statusFilter;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  function sortEventos(list: any[]) {
    return [...list].sort((a, b) => {
      if (sortBy === "titleAsc") {
        const ta = (a?.titulo || a?.title || a?.nome || "").toString().toLowerCase();
        const tb = (b?.titulo || b?.title || b?.nome || "").toString().toLowerCase();
        return ta.localeCompare(tb);
      }
      const da = getEventoDate(a)?.getTime() || 0;
      const db = getEventoDate(b)?.getTime() || 0;
      return sortBy === "dateAsc" ? da - db : db - da;
    });
  }

  const filteredUpcoming = useMemo(() => {
    const base = ensureArray<Evento>(upcomingEventos).filter((e) => matchesQuery(e) && matchesStatus(e) && inPeriod(getEventoDate(e)));
    return sortEventos(base);
  }, [upcomingEventos, query, statusFilter, period, sortBy]);

  const filteredPast = useMemo(() => {
    const base = ensureArray<Evento>(pastEventos).filter((e) => matchesQuery(e) && matchesStatus(e));
    return sortEventos(base);
  }, [pastEventos, query, statusFilter, sortBy]);

  const groupedUpcoming = useMemo(() => {
    return buildEventoGroups(filteredUpcoming).map((group) => ({
      ...group,
      eventos: [...group.eventos].sort((a, b) => {
        const da = getEventoDate(a)?.getTime() || 0;
        const db = getEventoDate(b)?.getTime() || 0;
        return da - db;
      }),
    }));
  }, [filteredUpcoming, sortBy]);

  const groupedPast = useMemo(() => {
    return buildEventoGroups(filteredPast).map((group) => ({
      ...group,
      eventos: [...group.eventos].sort((a, b) => {
        const da = getEventoDate(a)?.getTime() || 0;
        const db = getEventoDate(b)?.getTime() || 0;
        return da - db;
      }),
    }));
  }, [filteredPast, sortBy]);

  function toggleRecurringGroup(key: string) {
    setExpandedRecurringGroups((prev) => ({ ...prev, [key]: !prev[key] }));
  }

  function renderEventoEntry(evento: Evento, key: string, isPast = false) {
    return (
      <div key={key} className="space-y-2">
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center gap-2">
            <Clock className="h-4 w-4" />
            <span>
              {(() => {
                const d = getEventoDate(evento);
                return d ? d.toLocaleString() : "Sem data";
              })()}
            </span>
          </div>
          <StatusBadge status={(evento as any)?.status} />
        </div>
        <EventoItem
          evento={evento}
          onEdit={handleEditClick}
          onDelete={() => requestDeleteEvento(evento.id!)}
          onViewRegistrants={handleViewRegistrants}
          onDownloadReport={isPast ? handleDownloadReport : undefined}
        />
      </div>
    );
  }

  function renderEventoGroups(groups: EventoGroup[], scope: "upcoming" | "past") {
    const recurringGroups = groups.filter((group) => group.eventos.length > 1 || group.parentId !== null);
    const singleGroups = groups.filter((group) => !(group.eventos.length > 1 || group.parentId !== null));

    return (
      <div className="space-y-6">
        {recurringGroups.length > 0 && (
          <section className="space-y-4">
            <div className="flex items-center gap-2 text-blue-900">
              <Repeat className="h-4 w-4" />
              <h3 className="text-sm font-semibold uppercase tracking-wide">Séries recorrentes</h3>
              <Badge variant="secondary">{recurringGroups.length}</Badge>
            </div>

            <div className="space-y-4">
              {recurringGroups.map((group) => {
                const orderedByDate = [...group.eventos].sort((a, b) => {
                  const da = getEventoDate(a)?.getTime() || 0;
                  const db = getEventoDate(b)?.getTime() || 0;
                  return da - db;
                });
                const firstDate = orderedByDate[0] ? getEventoDate(orderedByDate[0]) : null;
                const lastDate = orderedByDate[orderedByDate.length - 1]
                  ? getEventoDate(orderedByDate[orderedByDate.length - 1])
                  : null;

                const expandKey = `${scope}-${group.key}`;
                const isExpanded = !!expandedRecurringGroups[expandKey];
                const canExpand = group.eventos.length > 1;
                const visibleEventos = isExpanded || !canExpand ? group.eventos : group.eventos.slice(0, 1);
                const baseEvento = orderedByDate[0] ?? group.eventos[0];
                const canDeleteSeries = !!baseEvento?.id;
                const isDeletingSeries = deletingSeriesKey === expandKey;
                const parentEvento = group.eventos.find(e => e.id === group.parentId) ?? baseEvento;
                const seriesHasRegistration = group.eventos.some(e => e.registrationRequired);

                return (
                  <div key={expandKey} className="rounded-lg border border-blue-200/70 bg-blue-50/30 p-4">
                    <div className="mb-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                      <div className="flex items-center gap-2 text-blue-900">
                        <span className="font-semibold">Série recorrente</span>
                        <Badge variant="secondary">
                          {group.eventos.length} {group.eventos.length > 1 ? "ocorrências" : "ocorrência"}
                        </Badge>
                      </div>
                      <div className="flex items-center gap-2">
                        {seriesHasRegistration && parentEvento && (
                          <Button variant="outline" size="sm" onClick={() => handleViewRegistrants(parentEvento)}>
                            <Users className="mr-2 h-4 w-4" />
                            Ver inscritos
                          </Button>
                        )}
                        {canExpand && (
                          <Button variant="ghost" size="sm" onClick={() => toggleRecurringGroup(expandKey)}>
                            {isExpanded ? <ChevronDown className="mr-2 h-4 w-4" /> : <ChevronRight className="mr-2 h-4 w-4" />}
                            {isExpanded ? "Recolher" : "Expandir"}
                          </Button>
                        )}
                        {canDeleteSeries && (
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleDeleteSeries(baseEvento.id, expandKey)}
                            disabled={isDeletingSeries}
                          >
                            {isDeletingSeries ? (
                              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            ) : (
                              <Trash2 className="mr-2 h-4 w-4" />
                            )}
                            Excluir série
                          </Button>
                        )}
                      </div>
                    </div>

                    {(firstDate || lastDate) && (
                      <p className="mb-4 text-xs text-muted-foreground">
                        {firstDate ? `Início: ${firstDate.toLocaleString()}` : ""}
                        {firstDate && lastDate ? " • " : ""}
                        {lastDate ? `Fim da série: ${lastDate.toLocaleString()}` : ""}
                      </p>
                    )}

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                      {visibleEventos.map((evento) => renderEventoEntry(evento, `${expandKey}-${evento.id}`, scope === "past"))}
                    </div>

                    {!isExpanded && canExpand && (
                      <p className="mt-3 text-xs text-muted-foreground">
                        Mostrando a próxima ocorrência. Clique em expandir para ver todas.
                      </p>
                    )}
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {singleGroups.length > 0 && (
          <section className="space-y-4">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Calendar className="h-4 w-4" />
              <h3 className="text-sm font-semibold uppercase tracking-wide">Eventos únicos</h3>
              <Badge variant="outline">{singleGroups.length}</Badge>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {singleGroups.map((group) => {
                const single = group.eventos[0];
                return renderEventoEntry(single, `${scope}-${group.key}-${single.id}`, scope === "past");
              })}
            </div>
          </section>
        )}
      </div>
    );
  }

  // ---- UI
  return (
    <main className="container mx-auto px-4 py-8">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between mb-6">
        <h2 className="text-3xl md:text-4xl font-bold font-poppins text-gray-800">Eventos Online</h2>
        {isAuthenticated && (
          <Button onClick={handleAddNewClick} className="bg-ccaa-blue-600 hover:bg-ccaa-blue-700 text-white">
            <Plus className="h-4 w-4 mr-2" />
            Novo Evento
          </Button>
        )}
      </div>

      {/* DASHBOARD — apenas Admin */}
      {isAdmin && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Eventos Futuros</CardTitle>
              <Clapperboard className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{isLoading ? "…" : stats?.totalEventosFuturos}</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Eventos Passados</CardTitle>
              <History className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{isLoading ? "…" : stats?.totalEventosPassados}</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Organizadores Distintos</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{isLoading ? "…" : stats?.totalOrganizadores}</div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* TOOLBAR */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
            <div className="md:col-span-5">
              <Label className="mb-1 block">Buscar</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Título, organizador…"
                  className="pl-9"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
              </div>
            </div>
            <div className="md:col-span-3">
              <Label className="mb-1 block">Período</Label>
              <Select value={period} onValueChange={setPeriod}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PERIODS.map((p) => (
                    <SelectItem key={p.value} value={p.value}>{p.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="md:col-span-2">
              <Label className="mb-1 block">Ordenar</Label>
              <Select value={sortBy} onValueChange={setSortBy}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SORTS.map((s) => (
                    <SelectItem key={s.value} value={s.value}>{s.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="md:col-span-2">
              <Label className="mb-1 block">Status</Label>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos</SelectItem>
                  {STATUS.map((s) => (
                    <SelectItem key={s.value} value={s.value}>{s.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* ABAS */}
      <Tabs defaultValue="futuros" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="futuros">Próximos Eventos</TabsTrigger>
          <TabsTrigger value="passados">Histórico de Eventos</TabsTrigger>
        </TabsList>

        {/* FUTUROS */}
        <TabsContent value="futuros" className="mt-6">
          {isLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {Array.from({ length: 6 }).map((_, i) => (
                <CardSkeleton key={i} />
              ))}
            </div>
          ) : filteredUpcoming.length === 0 ? (
            <div className="text-center py-16 bg-white rounded-lg border">
              <Calendar className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-xl font-semibold">Nenhum evento encontrado</h3>
              <p className="text-muted-foreground">Tente limpar filtros ou criar um novo evento.</p>
              <div className="mt-4">
                {isAuthenticated && (
                  <Button onClick={handleAddNewClick}>Criar evento</Button>
                )}
              </div>
            </div>
          ) : (
            renderEventoGroups(groupedUpcoming, "upcoming")
          )}
        </TabsContent>

        {/* PASSADOS */}
        <TabsContent value="passados" className="mt-6">
          {isLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {Array.from({ length: 6 }).map((_, i) => (
                <CardSkeleton key={i} />
              ))}
            </div>
          ) : filteredPast.length === 0 ? (
            <div className="text-center py-16 bg-white rounded-lg border">
              <History className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-xl font-semibold">Nenhum evento passado encontrado</h3>
            </div>
          ) : (
            renderEventoGroups(groupedPast, "past")
          )}
        </TabsContent>
      </Tabs>

      <NovoEventoForm
        isOpen={isFormOpen}
        onClose={() => {
          setIsFormOpen(false);
          setEditingEvento(null);
        }}
        onSubmit={handleSaveEvento}
        isLoading={isSaving}
        eventoToEdit={editingEvento}
      />

      {/* Modal de confirmação para exclusão */}
      <AlertDialog open={!!pendingDeleteId} onOpenChange={(open) => !open && setPendingDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir evento?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta ação não poderá ser desfeita. O evento será removido definitivamente.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={confirmDeleteEvento}>Excluir</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <Dialog open={inscritosOpen} onOpenChange={setInscritosOpen}>
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>Inscrições do Sistema</DialogTitle>
            <DialogDescription>
              {inscritosEvento ? `Evento: ${inscritosEvento.title}` : ""}
            </DialogDescription>
          </DialogHeader>

          {/* Link de inscrição + ações */}
          {inscritosEvento && (
            <div className="flex flex-col gap-3 border rounded-lg p-3 bg-gray-50">
              <div className="flex items-center gap-2">
                <span className="text-xs text-muted-foreground font-medium">Link de inscrição para compartilhar:</span>
              </div>
              <div className="flex items-center gap-2">
                <code className="flex-1 text-xs bg-white border rounded px-2 py-1.5 truncate text-gray-700">
                  {`${window.location.origin}/inscricao/evento/${inscritosEvento.id}`}
                </code>
                <Button variant="outline" size="sm" onClick={handleCopiarLink}>
                  {linkCopiado ? <Check className="h-3.5 w-3.5 text-green-600" /> : <Copy className="h-3.5 w-3.5" />}
                  {linkCopiado ? "Copiado!" : "Copiar"}
                </Button>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">
                  {inscritosLoading ? "…" : `${inscritos.length} inscrito(s)`}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleDownloadExcel}
                  disabled={downloadingExcel || inscritos.length === 0}
                >
                  {downloadingExcel ? (
                    <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <Download className="mr-2 h-3.5 w-3.5" />
                  )}
                  Baixar Planilha Excel
                </Button>
              </div>
            </div>
          )}

          {inscritosLoading ? (
            <div className="py-8 text-center text-muted-foreground">Carregando inscritos...</div>
          ) : inscritos.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">Nenhum inscrito ainda.</div>
          ) : (
            <div className="overflow-auto max-h-80">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nome</TableHead>
                    <TableHead>E-mail</TableHead>
                    <TableHead>Unidade</TableHead>
                    <TableHead>Telefone</TableHead>
                    <TableHead>Cargo</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {inscritos.map((i) => (
                    <TableRow key={i.id}>
                      <TableCell className="font-medium">{i.nome || "-"}</TableCell>
                      <TableCell>{i.email || "-"}</TableCell>
                      <TableCell>{i.nomeUnidade || "-"}</TableCell>
                      <TableCell>{i.telefone || "-"}</TableCell>
                      <TableCell>{i.cargo || "-"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </main>
  );
}
