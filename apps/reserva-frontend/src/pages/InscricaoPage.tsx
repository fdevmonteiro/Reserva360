import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import {
  Calendar,
  Clock,
  CheckCircle2,
  Video,
  Loader2,
  AlertCircle,
  User,
  Mail,
  Phone,
  Building2,
  Briefcase,
  ExternalLink,
  MapPin,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  getEventoInfoPublico,
  inscreverNoEvento,
  type EventoInfoPublico,
  type InscricaoRequest,
} from "@/services/inscricaoService";

const schema = z.object({
  nome: z.string().min(2, "Nome deve ter pelo menos 2 caracteres"),
  email: z.string().email("E-mail inválido"),
  telefone: z.string().optional(),
  nomeUnidade: z.string().min(2, "Nome da unidade é obrigatório"),
  cargo: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

function formatDate(dateStr: string) {
  try {
    return format(new Date(dateStr), "dd 'de' MMMM 'de' yyyy", { locale: ptBR });
  } catch {
    return dateStr;
  }
}

function formatTime(dateStr: string) {
  try {
    return format(new Date(dateStr), "HH:mm", { locale: ptBR });
  } catch {
    return dateStr;
  }
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <p className="flex items-center gap-1 text-xs text-red-500 mt-1">
      <AlertCircle className="h-3 w-3 shrink-0" />
      {message}
    </p>
  );
}

/** Logo CCAA em texto estilizado */
function CcaaLogo() {
  return (
    <div className="flex items-center gap-2 select-none">
      <img src="/logo-ccaa-lp.png"/> 
      <div className="h-6 w-px bg-white/30" />
      <span className="text-white/90 text-xs font-medium uppercase tracking-wider leading-tight">
        Portal de<br />Inscrições
      </span>
    </div>
  );
}

function EventHero({ evento }: { evento: EventoInfoPublico }) {
  return (
    <div className="relative overflow-hidden rounded-2xl shadow-xl">
      {/* Gradient: vermelho CCAA → azul CCAA */}
      <div className="absolute inset-0 bg-gradient-to-br from-ccaa-red-700 via-ccaa-red-600 to-ccaa-blue-800" />

      {/* Círculos decorativos */}
      <div className="absolute -top-12 -right-12 h-56 w-56 rounded-full bg-white/5" />
      <div className="absolute -bottom-10 -left-10 h-40 w-40 rounded-full bg-white/5" />
      <div className="absolute top-1/2 right-8 h-24 w-24 -translate-y-1/2 rounded-full bg-ccaa-blue-600/20" />

      <div className="relative p-6 sm:p-8 space-y-5">
        {/* Header com logo */}
        <div className="flex items-center justify-between">
          <CcaaLogo />
          <span className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1 text-xs font-medium text-white backdrop-blur-sm border border-white/10">
            <Video className="h-3.5 w-3.5" />
            Zoom
          </span>
        </div>

        {/* Divisor */}
        <div className="h-px bg-white/15" />

        {/* Título */}
        <div className="space-y-2">
          <p className="text-ccaa-red-200 text-xs font-semibold uppercase tracking-widest">
            Evento Online
          </p>
          <h1 className="text-2xl sm:text-3xl font-bold text-white leading-tight">
            {evento.titulo}
          </h1>
          {evento.descricao && (
            <p className="text-white/75 text-sm sm:text-base leading-relaxed">
              {evento.descricao}
            </p>
          )}
        </div>

        {/* Metadados */}
        <div className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-white/80 pt-1">
          <span className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-ccaa-red-300" />
            {formatDate(evento.startTime)}
          </span>
          <span className="flex items-center gap-2">
            <Clock className="h-4 w-4 text-ccaa-red-300" />
            {formatTime(evento.startTime)}
            {evento.endTime && ` – ${formatTime(evento.endTime)}`}
          </span>
          <span className="flex items-center gap-2">
            <MapPin className="h-4 w-4 text-ccaa-red-300" />
            Online
          </span>
        </div>
      </div>
    </div>
  );
}

export default function InscricaoPage() {
  const { eventoId } = useParams<{ eventoId: string }>();
  const id = Number(eventoId);

  const [evento, setEvento] = useState<EventoInfoPublico | null>(null);
  const [loadingEvento, setLoadingEvento] = useState(true);
  const [eventoError, setEventoError] = useState<string | null>(null);

  const [joinUrl, setJoinUrl] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!id) return;
    setLoadingEvento(true);
    getEventoInfoPublico(id)
      .then(setEvento)
      .catch((err) => {
        const msg =
          err?.response?.data?.error ?? "Evento não encontrado ou inscrições encerradas.";
        setEventoError(msg);
      })
      .finally(() => setLoadingEvento(false));
  }, [id]);

  const onSubmit = async (data: FormData) => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload: InscricaoRequest = {
        nome: data.nome,
        email: data.email,
        telefone: data.telefone || undefined,
        nomeUnidade: data.nomeUnidade,
        cargo: data.cargo || undefined,
      };
      const result = await inscreverNoEvento(id, payload);
      setJoinUrl(result.joinUrl);
      setSubmitted(true);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { error?: string } } };
      const msg =
        error?.response?.data?.error ??
        "Não foi possível realizar a inscrição. Tente novamente.";
      setSubmitError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  /* ── Loading ── */
  if (loadingEvento) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4 bg-gradient-to-br from-ccaa-red-50 to-ccaa-blue-50">
        <div className="relative h-16 w-16">
          <div className="absolute inset-0 rounded-full border-4 border-ccaa-red-100" />
          <Loader2 className="absolute inset-0 h-16 w-16 animate-spin text-ccaa-red-600" />
        </div>
        <p className="text-sm text-gray-500 animate-pulse">Carregando evento...</p>
      </div>
    );
  }

  /* ── Error ── */
  if (eventoError || !evento) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-ccaa-red-50 to-ccaa-blue-50 px-4">
        <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center space-y-4">
          <div className="mx-auto h-16 w-16 rounded-full bg-ccaa-red-50 flex items-center justify-center">
            <AlertCircle className="h-8 w-8 text-ccaa-red-500" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-800">Evento não disponível</h2>
            <p className="text-gray-500 text-sm mt-1">
              {eventoError ?? "Este evento não está disponível para inscrição."}
            </p>
          </div>
          <div className="flex justify-center pt-2">
            <div className="flex gap-1">
              <span className="text-ccaa-red-600 font-extrabold text-lg">CC</span>
              <span className="text-ccaa-blue-700 font-extrabold text-lg">AA</span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  /* ── Success ── */
  if (submitted) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-ccaa-red-50 to-ccaa-blue-50 px-4 py-10">
        <div className="max-w-lg w-full space-y-4">
          <div className="bg-white rounded-2xl shadow-lg overflow-hidden">
            {/* Faixa CCAA */}
            <div className="h-2 bg-gradient-to-r from-ccaa-red-600 to-ccaa-blue-700" />

            <div className="p-8 text-center space-y-5">
              {/* Ícone */}
              <div className="mx-auto h-20 w-20 rounded-full bg-green-50 flex items-center justify-center">
                <CheckCircle2 className="h-10 w-10 text-green-500" />
              </div>

              <div>
                <h2 className="text-2xl font-bold text-gray-800">Inscrição confirmada!</h2>
                <p className="text-gray-500 mt-1 text-sm">
                  Você está inscrito em{" "}
                  <span className="font-semibold text-gray-700">{evento.titulo}</span>.
                </p>
              </div>

              {/* Resumo do evento */}
              <div className="text-left bg-gray-50 rounded-xl p-4 space-y-2 text-sm text-gray-600 border border-gray-100">
                <div className="flex items-center gap-2">
                  <Calendar className="h-4 w-4 text-ccaa-red-500 shrink-0" />
                  <span>{formatDate(evento.startTime)}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4 text-ccaa-red-500 shrink-0" />
                  <span>
                    {formatTime(evento.startTime)}
                    {evento.endTime && ` – ${formatTime(evento.endTime)}`}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <Video className="h-4 w-4 text-ccaa-red-500 shrink-0" />
                  <span>Reunião Online · Zoom</span>
                </div>
              </div>

              {/* Link Zoom */}
              {joinUrl ? (
                <div className="space-y-3">
                  <div className="bg-ccaa-blue-50 border border-ccaa-blue-100 rounded-xl p-4 space-y-3">
                    <p className="text-sm font-medium text-ccaa-blue-800">
                      Seu link de acesso está pronto:
                    </p>
                    <a
                      href={joinUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-2 bg-ccaa-blue-700 hover:bg-ccaa-blue-800 active:scale-95 text-white text-sm font-medium px-5 py-2.5 rounded-lg transition-all w-full justify-center"
                    >
                      <Video className="h-4 w-4" />
                      Entrar na Reunião Zoom
                      <ExternalLink className="h-3.5 w-3.5 opacity-70" />
                    </a>
                  </div>
                  <p className="text-xs text-gray-400">
                    Salve este link para acessar no dia do evento.
                  </p>
                </div>
              ) : (
                <p className="text-sm text-gray-500 bg-gray-50 rounded-xl p-4 border border-gray-100">
                  O link de acesso será enviado pelo organizador próximo à data do evento.
                </p>
              )}

              {/* Logo rodapé */}
              <div className="flex justify-center pt-1">
                <div className="flex gap-0.5">
                  <span className="text-ccaa-red-600 font-extrabold text-base">CC</span>
                  <span className="text-ccaa-blue-700 font-extrabold text-base">AA</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  /* ── Formulário ── */
  return (
    <div className="min-h-screen bg-gradient-to-br from-ccaa-red-50 via-white to-ccaa-blue-50 py-10 px-4">
      <div className="max-w-lg mx-auto space-y-5">
        {/* Hero */}
        <EventHero evento={evento} />

        {/* Card do formulário */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          {/* Faixa de cor CCAA */}
          <div className="h-1 bg-gradient-to-r from-ccaa-red-600 to-ccaa-blue-700" />

          <div className="px-6 py-5 border-b border-gray-100">
            <h2 className="text-lg font-semibold text-gray-800">Formulário de Inscrição</h2>
            <p className="text-sm text-gray-500 mt-0.5">
              Preencha seus dados para garantir sua vaga.
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-5">
            {/* Nome */}
            <div className="space-y-1.5">
              <Label htmlFor="nome" className="flex items-center gap-1.5 text-sm font-medium text-gray-700">
                <User className="h-3.5 w-3.5 text-ccaa-red-400" />
                Nome completo <span className="text-ccaa-red-500">*</span>
              </Label>
              <Input
                id="nome"
                placeholder="Seu nome completo"
                className={errors.nome ? "border-red-300 focus-visible:ring-red-400" : "focus-visible:ring-ccaa-blue-400"}
                {...register("nome")}
              />
              <FieldError message={errors.nome?.message} />
            </div>

            {/* Email */}
            <div className="space-y-1.5">
              <Label htmlFor="email" className="flex items-center gap-1.5 text-sm font-medium text-gray-700">
                <Mail className="h-3.5 w-3.5 text-ccaa-red-400" />
                E-mail <span className="text-ccaa-red-500">*</span>
              </Label>
              <Input
                id="email"
                type="email"
                placeholder="seu@email.com"
                className={errors.email ? "border-red-300 focus-visible:ring-red-400" : "focus-visible:ring-ccaa-blue-400"}
                {...register("email")}
              />
              <FieldError message={errors.email?.message} />
            </div>

            {/* Unidade */}
            <div className="space-y-1.5">
              <Label htmlFor="nomeUnidade" className="flex items-center gap-1.5 text-sm font-medium text-gray-700">
                <Building2 className="h-3.5 w-3.5 text-ccaa-red-400" />
                Nome da Unidade / Franquia <span className="text-ccaa-red-500">*</span>
              </Label>
              <Input
                id="nomeUnidade"
                placeholder="Ex: CCAA Centro, CCAA Jardins..."
                className={errors.nomeUnidade ? "border-red-300 focus-visible:ring-red-400" : "focus-visible:ring-ccaa-blue-400"}
                {...register("nomeUnidade")}
              />
              <FieldError message={errors.nomeUnidade?.message} />
            </div>

            {/* Telefone + Cargo */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="telefone" className="flex items-center gap-1.5 text-sm font-medium text-gray-700">
                  <Phone className="h-3.5 w-3.5 text-ccaa-red-400" />
                  Telefone / WhatsApp
                </Label>
                <Input
                  id="telefone"
                  placeholder="(11) 99999-9999"
                  className="focus-visible:ring-ccaa-blue-400"
                  {...register("telefone")}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="cargo" className="flex items-center gap-1.5 text-sm font-medium text-gray-700">
                  <Briefcase className="h-3.5 w-3.5 text-ccaa-red-400" />
                  Cargo
                </Label>
                <Input
                  id="cargo"
                  placeholder="Ex: Diretor, Professor..."
                  className="focus-visible:ring-ccaa-blue-400"
                  {...register("cargo")}
                />
              </div>
            </div>

            {/* Legenda */}
            <p className="text-xs text-gray-400">
              <span className="text-ccaa-red-500">*</span> Campos obrigatórios
            </p>

            {/* Erro de envio */}
            {submitError && (
              <div className="flex items-start gap-2.5 text-sm text-red-600 bg-red-50 border border-red-100 rounded-xl p-3.5">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span>{submitError}</span>
              </div>
            )}

            {/* Botão */}
            <Button
              type="submit"
              disabled={submitting}
              className="w-full h-11 bg-ccaa-red-600 hover:bg-ccaa-red-700 active:scale-[0.98] text-white font-semibold rounded-xl transition-all shadow-sm"
            >
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Inscrevendo...
                </>
              ) : (
                "Confirmar Inscrição"
              )}
            </Button>
          </form>
        </div>

        {/* Rodapé */}
        <div className="flex flex-col items-center gap-1 pb-4">
          
          <p className="text-xs text-gray-400">
            Ao se inscrever, você concorda em receber informações sobre este evento.
          </p>
        </div>
      </div>
    </div>
  );
}
