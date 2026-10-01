export type JourneyScreen = "DETAILS" | "CHECKOUT";
export type JourneyEventType = "STARTED" | "SCREEN_VIEWED" | "ABANDONED";

export interface JourneyEvent {
  journeyId: string;
  eventType: JourneyEventType;
  screen: JourneyScreen;
}

const JOURNEY_EVENTS_PATH = "/api/reservation-journeys/events";

export function sendJourneyEvent(event: JourneyEvent): void {
  const body = JSON.stringify(event);
  if (typeof navigator !== "undefined" && typeof navigator.sendBeacon === "function") {
    const sent = navigator.sendBeacon(
      JOURNEY_EVENTS_PATH,
      new Blob([body], { type: "application/json" }),
    );
    if (sent) return;
  }
  void fetch(JOURNEY_EVENTS_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
    keepalive: true,
  }).catch(() => undefined);
}
