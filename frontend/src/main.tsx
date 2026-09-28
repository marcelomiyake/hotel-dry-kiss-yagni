import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { HotelApp } from "./App";
import "./styles.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <HotelApp />
  </StrictMode>,
);
