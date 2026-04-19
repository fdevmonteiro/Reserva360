import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";

export default defineConfig({
  server: {
    host: "0.0.0.0",
    port: 8081,
    strictPort: true,
    allowedHosts: ["reserva360hml.grupoccaa.com.br", "reserva360.grupoccaa.com.br"],
  },

  plugins: [react()],

  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
