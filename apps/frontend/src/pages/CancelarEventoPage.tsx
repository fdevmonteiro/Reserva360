import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "@/services/api";

export default function CancelarEventoPage() {
  const [params] = useSearchParams();
  const token = params.get("token");

  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");

  useEffect(() => {
    if (!token) {
      setStatus("error");
      return;
    }

    api.post(`/eventos/cancelar?token=${encodeURIComponent(token)}`)
      .then(() => setStatus("success"))
      .catch(() => setStatus("error"));
  }, [token]);

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <div className="bg-white p-8 rounded-xl shadow-md max-w-md text-center">
        {status === "loading" && <p>Cancelando evento...</p>}
        {status === "success" && (
          <>
            <h2 className="text-2xl font-bold text-green-600">✅ Evento cancelado!</h2>
            <p className="mt-2">Seu evento foi cancelado com sucesso.</p>
          </>
        )}
        {status === "error" && (
          <>
            <h2 className="text-2xl font-bold text-red-600">❌ Erro</h2>
            <p className="mt-2">Token inválido ou expirado.</p>
          </>
        )}
      </div>
    </div>
  );
}
