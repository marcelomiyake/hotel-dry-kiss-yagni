import { defineConfig, type Plugin } from "vite";
import react from "@vitejs/plugin-react";

const hotelService = process.env.HOTEL_SERVICE_URL ?? "http://localhost:8081";
const rateService = process.env.RATE_SERVICE_URL ?? "http://localhost:8082";
const reservationService = process.env.RESERVATION_SERVICE_URL ?? "http://localhost:8083";

function inlineBuiltCss(): Plugin {
  return {
    name: "inline-built-css",
    apply: "build",
    transformIndexHtml: {
      order: "post",
      handler(html, context) {
        for (const asset of Object.values(context.bundle ?? {})) {
          if (asset.type !== "asset" || !asset.fileName.endsWith(".css")) continue;
          const link = `<link rel="stylesheet" crossorigin href="/${asset.fileName}">`;
          if (!html.includes(link)) continue;
          const css = typeof asset.source === "string" ? asset.source : new TextDecoder().decode(asset.source);
          html = html.replace(link, `<style>${css}</style>`);
          delete context.bundle?.[asset.fileName];
        }
        return html;
      }
    }
  };
}

export default defineConfig({
  plugins: [react(), inlineBuiltCss()],
  server: {
    port: 5173,
    proxy: {
      "/api/hotels": hotelService,
      "/api/admin/hotels": hotelService,
      "/api/admin/room-types": hotelService,
      "/api/rates": rateService,
      "/api/admin/rates": rateService,
      "/api/search": reservationService,
      "/api/reservations": reservationService,
      "/api/reservation-journeys": reservationService,
      "/api/admin/inventory": reservationService
    }
  },
  build: {
    sourcemap: false,
    cssMinify: true
  }
});
