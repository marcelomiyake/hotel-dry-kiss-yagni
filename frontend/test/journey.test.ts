import { afterEach, describe, expect, it, vi } from "vitest";
import { sendJourneyEvent, type JourneyEvent } from "../src/journey";

const event: JourneyEvent = {
  journeyId: "journey-123",
  eventType: "STARTED",
  screen: "DETAILS",
};

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("reservation journey telemetry", () => {
  it("sends small events through sendBeacon", () => {
    const sendBeacon = vi.fn().mockReturnValue(true);
    Object.defineProperty(navigator, "sendBeacon", { configurable: true, value: sendBeacon });

    sendJourneyEvent(event);

    expect(sendBeacon).toHaveBeenCalledWith(
      "/api/reservation-journeys/events",
      expect.objectContaining({ type: "application/json" }),
    );
  });

  it("uses a keepalive fetch when sendBeacon cannot queue the event", async () => {
    const sendBeacon = vi.fn().mockReturnValue(false);
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    Object.defineProperty(navigator, "sendBeacon", { configurable: true, value: sendBeacon });
    vi.stubGlobal("fetch", fetchMock);

    sendJourneyEvent(event);
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/reservation-journeys/events",
      expect.objectContaining({ method: "POST", keepalive: true, body: JSON.stringify(event) }),
    ));
  });

  it("uses keepalive fetch when the browser does not support sendBeacon and ignores network failure", async () => {
    Object.defineProperty(navigator, "sendBeacon", { configurable: true, value: undefined });
    const fetchMock = vi.fn().mockRejectedValue(new Error("offline"));
    vi.stubGlobal("fetch", fetchMock);

    sendJourneyEvent(event);

    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledOnce());
    await expect(fetchMock.mock.results[0].value).rejects.toThrow("offline");
  });
});
