import { useState, useEffect, useMemo } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Loader2, CalendarRange, RotateCw, ListChecks } from "lucide-react";
import { cn } from "@/lib/utils";

// Tipagens (ajuste se já existirem em '@/types/evento')
export type RecurrenceFrequency = "WEEKLY" | "MONTHLY"; // hoje vamos focar em WEEKLY
export type RecurrenceEndType = "UNTIL_DATE" | "AFTER_OCCURRENCES";

export interface RecurrenceDTO {
  frequency: RecurrenceFrequency;
  interval: number; // a cada X semanas
  weekDays?: number[]; // 1..7 (Seg..Dom)
  endType: RecurrenceEndType;
  untilDate?: string; // "YYYY-MM-DD"
  maxOccurrences?: number | null;
}

export interface EventoFormData {
  title: string;
  description?: string;
  startTime: string; // "YYYY-MM-DDTHH:mm"
  endTime: string;   // "YYYY-MM-DDTHH:mm"
  participantCount: number;
  organizerEmail: string;
  coHostEmail?: string;
  registrationRequired: boolean;
  isRecurring: boolean;
  frequency?: RecurrenceFrequency;
  untilDate?: string;
  // Enviar pro backend como `recurrence`
  recurrence?: RecurrenceDTO | null;
}

interface Evento {
  id: number;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  participantCount?: number;
  organizerEmail?: string;
  coHostEmail?: string;
  registrationRequired?: boolean;
}

interface NovoEventoFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: EventoFormData, id?: number) => void;
  isLoading: boolean;
  eventoToEdit: Evento | null;
}

const WEEK_DAYS: { label: string; value: number }[] = [
  { label: "Seg", value: 1 },
  { label: "Ter", value: 2 },
  { label: "Qua", value: 3 },
  { label: "Qui", value: 4 },
  { label: "Sex", value: 5 },
  { label: "Sáb", value: 6 },
  { label: "Dom", value: 7 },
];

export default function NovoEventoForm({ isOpen, onClose, onSubmit, isLoading, eventoToEdit }: NovoEventoFormProps) {
  const isEditMode = Boolean(eventoToEdit);

  const getInitialState = (): EventoFormData => {
    if (isEditMode && eventoToEdit) {
      return {
        title: eventoToEdit.title,
        description: eventoToEdit.description || "",
        startTime: eventoToEdit.startTime.substring(0, 16),
        endTime: eventoToEdit.endTime.substring(0, 16),
        participantCount: eventoToEdit.participantCount || 1,
        organizerEmail: eventoToEdit.organizerEmail || "",
        coHostEmail: eventoToEdit.coHostEmail || "",
        registrationRequired: !!eventoToEdit.registrationRequired,
        isRecurring: false,
        frequency: undefined,
        untilDate: undefined,
        recurrence: null,
      };
    }
    return {
      title: "",
      description: "",
      startTime: "",
      endTime: "",
      participantCount: 1,
      organizerEmail: "",
      coHostEmail: "",
      registrationRequired: false,
      isRecurring: false,
      frequency: "WEEKLY",
      untilDate: undefined,
      recurrence: null,
    };
  };

  const [formData, setFormData] = useState<EventoFormData>(getInitialState());
  const [recWeekDays, setRecWeekDays] = useState<number[]>([]); // controle local dos botões de dias
  const [endType, setEndType] = useState<RecurrenceEndType>("UNTIL_DATE");
  const [interval, setInterval] = useState<number>(1);
  const [maxOccurrences, setMaxOccurrences] = useState<number | "">("");

  useEffect(() => {
    setFormData(getInitialState());
    setRecWeekDays([]);
    setEndType("UNTIL_DATE");
    setInterval(1);
    setMaxOccurrences("");
  }, [eventoToEdit, isOpen]);

  // Helpers de mudança
  const handleChange: React.ChangeEventHandler<HTMLInputElement | HTMLTextAreaElement> = (e) => {
    const { name, value, type } = e.target;
    const finalValue = type === "number" ? parseInt(value || "0", 10) : value;
    setFormData((prev) => ({ ...prev, [name]: finalValue } as EventoFormData));
  };

  const handleCheckboxChange = (checked: boolean) => {
    setFormData((prev) => ({ ...prev, isRecurring: checked }));
  };

  const handleFrequencyChange = (value: RecurrenceFrequency) => {
    setFormData((prev) => ({ ...prev, frequency: value }));
  };

  const toggleWeekDay = (value: number) => {
    setRecWeekDays((prev) =>
      prev.includes(value) ? prev.filter((d) => d !== value) : [...prev, value]
    );
  };

  // Monta o objeto recurrence que será enviado ao backend
  const recurrence: RecurrenceDTO | null = useMemo(() => {
    if (!formData.isRecurring) return null;
    if (formData.frequency !== "WEEKLY") {
      // Placeholder para suporte futuro a MONTHLY, etc.
      return {
        frequency: "WEEKLY",
        interval: Math.max(1, interval || 1),
        weekDays: recWeekDays.sort((a, b) => a - b),
        endType,
        untilDate: endType === "UNTIL_DATE" ? formData.untilDate : undefined,
        maxOccurrences: endType === "AFTER_OCCURRENCES" && maxOccurrences !== "" ? Number(maxOccurrences) : null,
      };
    }
    return {
      frequency: "WEEKLY",
      interval: Math.max(1, interval || 1),
      weekDays: recWeekDays.sort((a, b) => a - b),
      endType,
      untilDate: endType === "UNTIL_DATE" ? formData.untilDate : undefined,
      maxOccurrences: endType === "AFTER_OCCURRENCES" && maxOccurrences !== "" ? Number(maxOccurrences) : null,
    };
  }, [formData.isRecurring, formData.frequency, interval, recWeekDays, endType, formData.untilDate, maxOccurrences]);

  // Prévia textual da regra
  const recurrencePreview = useMemo(() => {
    if (!recurrence) return "—";
    const dias = recWeekDays
      .sort((a, b) => a - b)
      .map((d) => WEEK_DAYS.find((x) => x.value === d)?.label)
      .filter(Boolean)
      .join(", ");
    const base = `Semanal a cada ${recurrence.interval} sem. nos dias: ${dias || "—"}`;
    if (endType === "UNTIL_DATE" && recurrence.untilDate) return `${base} • até ${recurrence.untilDate}`;
    if (endType === "AFTER_OCCURRENCES" && recurrence.maxOccurrences) return `${base} • ${recurrence.maxOccurrences} ocorr.`;
    return base;
  }, [recurrence, recWeekDays, endType]);

  const handleSubmit: React.FormEventHandler<HTMLFormElement> = (e) => {
    e.preventDefault();

    const payload: EventoFormData = {
      ...formData,
      recurrence,
    };

    // Validações rápidas de recorrência
    if (formData.isRecurring) {
      if (formData.frequency === "WEEKLY" && (!recWeekDays || recWeekDays.length === 0)) {
        alert("Selecione pelo menos um dia da semana para a recorrência.");
        return;
      }
      if (endType === "UNTIL_DATE" && !formData.untilDate) {
        alert("Informe a data final da recorrência.");
        return;
      }
      if (endType === "AFTER_OCCURRENCES" && (maxOccurrences === "" || Number(maxOccurrences) <= 0)) {
        alert("Informe o número de ocorrências.");
        return;
      }
    }

    onSubmit(payload, eventoToEdit?.id);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {isEditMode ? "Editar Evento" : "Novo Evento"}
          </DialogTitle>
          <DialogDescription>Preencha os detalhes do seu evento abaixo.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-5 pt-2">
          {/* Campos principais */}
          <div className="space-y-4">
            <div>
              <Label htmlFor="title">Título do Evento *</Label>
              <Input id="title" name="title" value={formData.title} onChange={handleChange} required disabled={isLoading} />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="startTime">Início *</Label>
                <Input id="startTime" name="startTime" type="datetime-local" value={formData.startTime} onChange={handleChange} required disabled={isLoading} />
              </div>
              <div>
                <Label htmlFor="endTime">Fim *</Label>
                <Input id="endTime" name="endTime" type="datetime-local" value={formData.endTime} onChange={handleChange} required disabled={isLoading} />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <Label htmlFor="participantCount">Participantes *</Label>
                <Input id="participantCount" name="participantCount" type="number" min={1} value={formData.participantCount} onChange={handleChange} required disabled={isLoading} />
              </div>
              <div className="sm:col-span-2">
                <Label htmlFor="organizerEmail">Email do Organizador *</Label>
                <Input id="organizerEmail" name="organizerEmail" type="email" value={formData.organizerEmail} onChange={handleChange} required disabled={isLoading} />
              </div>
            </div>

            {/* <div>
              <Label htmlFor="coHostEmail">Email do Co-host (Opcional)</Label>
              <Input id="coHostEmail" name="coHostEmail" type="email" value={formData.coHostEmail} onChange={handleChange} disabled={isLoading} />
            </div> */}

            <div>
              <Label htmlFor="description">Descrição</Label>
              <Textarea id="description" name="description" value={formData.description} onChange={handleChange} disabled={isLoading} />
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="registrationRequired"
                checked={formData.registrationRequired}
                onCheckedChange={(checked) =>
                  setFormData((prev) => ({ ...prev, registrationRequired: checked === true }))
                }
                disabled={isLoading}
              />
              <Label htmlFor="registrationRequired" className="cursor-pointer">
                Exigir inscrição prévia para reunião
              </Label>
            </div>
          </div>

          {/* Recorrência */}
          {!isEditMode && (
            <div className="rounded-2xl border p-4 sm:p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Checkbox id="isRecurring" checked={formData.isRecurring} onCheckedChange={handleCheckboxChange} />
                  <Label htmlFor="isRecurring" className="cursor-pointer font-medium flex items-center gap-2">
                    <RotateCw className="h-4 w-4" /> Recorrência
                  </Label>
                </div>
                {formData.isRecurring && (
                  <div className="text-xs text-muted-foreground flex items-center gap-2">
                    <ListChecks className="h-4 w-4" /> {recurrencePreview}
                  </div>
                )}
              </div>

              {formData.isRecurring && (
                <div className="space-y-4 animate-in fade-in-0">
                  {/* Frequência (por enquanto só Weekly exposto) */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="sm:col-span-1">
                      <Label>Frequência</Label>
                      <Select value={formData.frequency} onValueChange={handleFrequencyChange}>
                        <SelectTrigger>
                          <SelectValue placeholder="Selecione" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="WEEKLY">Semanal</SelectItem>
                          {/* <SelectItem value="MONTHLY">Mensal</SelectItem> */}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="sm:col-span-1">
                      <Label>Intervalo</Label>
                      <div className="flex items-center gap-2">
                        <Input
                          type="number"
                          min={1}
                          value={interval}
                          onChange={(e) => setInterval(Math.max(1, Number(e.target.value) || 1))}
                        />
                        <span className="text-sm text-muted-foreground">semanas</span>
                      </div>
                    </div>

                    <div className="sm:col-span-1">
                      <Label>Até quando?</Label>
                      <div className="flex items-center gap-3 pt-2">
                        <label className="flex items-center gap-2 text-sm cursor-pointer">
                          <input
                            type="radio"
                            name="endType"
                            className="accent-primary"
                            checked={endType === "UNTIL_DATE"}
                            onChange={() => setEndType("UNTIL_DATE")}
                          />
                          Até data
                        </label>
                        <label className="flex items-center gap-2 text-sm cursor-pointer">
                          <input
                            type="radio"
                            name="endType"
                            className="accent-primary"
                            checked={endType === "AFTER_OCCURRENCES"}
                            onChange={() => setEndType("AFTER_OCCURRENCES")}
                          />
                          Após nº de ocorrências
                        </label>
                      </div>
                    </div>
                  </div>

                  {/* Dias da semana (Weekly) */}
                  {formData.frequency === "WEEKLY" && (
                    <div className="space-y-2">
                      <Label>Dias da semana</Label>
                      <div className="grid grid-cols-7 gap-1 sm:gap-2">
                        {WEEK_DAYS.map((d) => (
                          <button
                            key={d.value}
                            type="button"
                            className={cn(
                              "text-xs sm:text-sm rounded-xl py-2 sm:py-2.5 border transition",
                              recWeekDays.includes(d.value)
                                ? "bg-primary/10 border-primary text-primary"
                                : "hover:bg-muted"
                            )}
                            onClick={() => toggleWeekDay(d.value)}
                          >
                            {d.label}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Encerramento */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {endType === "UNTIL_DATE" ? (
                      <div>
                        <Label htmlFor="untilDate" className="flex items-center gap-2"><CalendarRange className="h-4 w-4" /> Repetir até</Label>
                        <Input id="untilDate" name="untilDate" type="date" value={formData.untilDate || ""} onChange={handleChange} />
                      </div>
                    ) : (
                      <div>
                        <Label htmlFor="maxOccurrences">Nº de ocorrências</Label>
                        <Input
                          id="maxOccurrences"
                          type="number"
                          min={1}
                          value={maxOccurrences}
                          onChange={(e) => setMaxOccurrences(e.target.value === "" ? "" : Math.max(1, Number(e.target.value)))}
                        />
                      </div>
                    )}

                    {/* Preview compacto */}
                    <div className="rounded-xl bg-muted/40 border p-3 sm:p-4 text-xs sm:text-sm text-muted-foreground">
                      <div className="font-medium text-foreground mb-1">Resumo da recorrência</div>
                      <div>{recurrencePreview}</div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : "Salvar Evento"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
