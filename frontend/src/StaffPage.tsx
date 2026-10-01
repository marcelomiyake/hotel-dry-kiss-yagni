import { useEffect, useState, type RefObject, type SubmitEvent } from "react";
import { api, errorMessage } from "./api";
import type { Hotel, RoomTypeDraft } from "./types";

function saveLabel(saving: boolean, isNew: boolean): string {
  if (saving) return "Saving…";
  return isNew ? "Add room type" : "Save room details";
}

export function StaffPage({
  adminKey,
  onAdminKeyChange,
  headingRef,
}: {
  readonly adminKey: string;
  readonly onAdminKeyChange: (value: string) => void;
  readonly headingRef: RefObject<HTMLHeadingElement | null>;
}) {
  const [hotels, setHotels] = useState<Hotel[]>([]);
  const [hotelId, setHotelId] = useState("");
  const [roomTypeId, setRoomTypeId] = useState("");
  const [roomDraft, setRoomDraft] = useState<RoomTypeDraft>({ name: "", details: "", maxGuests: 2, totalInventory: 10 });
  const [baseRate, setBaseRate] = useState("180");
  const [isNew, setIsNew] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [removeTarget, setRemoveTarget] = useState<string | null>(null);

  const selectedHotel = hotels.find((hotel) => hotel.id === hotelId) ?? null;
  const selectedRoom = selectedHotel?.roomTypes.find((room) => room.id === roomTypeId) ?? null;

  useEffect(() => {
    headingRef.current?.focus();
  }, [headingRef]);

  useEffect(() => {
    let active = true;
    api<Hotel[]>("/api/hotels?destination=")
      .then((result) => {
        if (!active) return;
        setHotels(result);
        setHotelId(result[0]?.id ?? "");
        setRoomTypeId(result[0]?.roomTypes[0]?.id ?? "");
      })
      .catch((loadError: unknown) => { if (active) setError(errorMessage(loadError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!selectedRoom || isNew) return;
    setRoomDraft({
      name: selectedRoom.name,
      details: selectedRoom.details,
      maxGuests: selectedRoom.maxGuests,
      totalInventory: selectedRoom.totalInventory,
    });
  }, [selectedRoom, isNew]);

  function startNewRoom() {
    setIsNew(true);
    setRoomTypeId("");
    setRoomDraft({ name: "", details: "", maxGuests: 2, totalInventory: 10 });
    setBaseRate("180");
    setMessage("");
    setError("");
  }

  function authHeaders(): HeadersInit {
    return { "X-Admin-Key": adminKey };
  }

  async function saveRoom(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");
    try {
      if (!adminKey.trim()) throw new Error("Enter the staff key before saving changes.");
      if (!selectedHotel) throw new Error("Choose a hotel first.");
      let roomId = roomTypeId;
      if (isNew) {
        const created = await api<{ id: string }>(`/api/admin/hotels/${selectedHotel.id}/room-types`, {
          method: "POST",
          headers: authHeaders(),
          body: JSON.stringify(roomDraft),
        });
        roomId = created.id;
        setRoomTypeId(roomId);
        setIsNew(false);
      } else if (roomId) {
        await api(`/api/admin/room-types/${roomId}`, {
          method: "PUT",
          headers: authHeaders(),
          body: JSON.stringify(roomDraft),
        });
      } else {
        throw new Error("Choose a room type or start a new one.");
      }
      if (isNew) {
        await api("/api/admin/rates/schedule", {
          method: "POST",
          headers: authHeaders(),
          body: JSON.stringify({ roomTypeId: roomId, baseRate: Number(baseRate) }),
        });
      }
      await api("/api/admin/inventory", {
        method: "PUT",
        headers: authHeaders(),
        body: JSON.stringify({ hotelId: selectedHotel.id, roomTypeId: roomId, totalInventory: roomDraft.totalInventory }),
      });
      const refreshed = await api<Hotel[]>("/api/hotels?destination=");
      setHotels(refreshed);
      setMessage(isNew ? "Room type, inventory, and nightly rates added." : "Room type and inventory updated.");
    } catch (saveError) {
      setError(errorMessage(saveError));
    } finally {
      setSaving(false);
    }
  }

  async function removeRoomType() {
    if (!removeTarget || !adminKey.trim()) return;
    setError("");
    try {
      await api(`/api/admin/room-types/${removeTarget}`, { method: "DELETE", headers: authHeaders() });
      const refreshed = await api<Hotel[]>("/api/hotels?destination=");
      setHotels(refreshed);
      const nextHotel = refreshed.find((hotel) => hotel.id === hotelId) ?? refreshed[0];
      setRoomTypeId(nextHotel?.roomTypes[0]?.id ?? "");
      setIsNew(false);
      setRemoveTarget(null);
      setMessage("Room type removed from new searches.");
    } catch (removeError) {
      setError(errorMessage(removeError));
    }
  }

  function changeRoomDraft(key: keyof RoomTypeDraft, value: string) {
    setRoomDraft((previous) => ({ ...previous, [key]: key === "name" || key === "details" ? value : Number(value) }));
  }

  function changeHotel(value: string) {
    setHotelId(value);
    setIsNew(false);
    setRoomTypeId("");
  }

  function cancelNewRoom() {
    setIsNew(false);
    setRoomTypeId(selectedHotel?.roomTypes[0]?.id ?? "");
  }

  return (
    <section className="view" aria-labelledby="staff-title">
      <p className="eyebrow">Hotel management</p>
      <h1 className="staff-title" id="staff-title" tabIndex={-1} ref={headingRef}>Staff room management</h1>
      <p className="lede staff-lede">Update room details and future inventory. A staff key is required for every change.</p>
      <div className="admin-layout">
        <StaffEditor
          adminKey={adminKey}
          onAdminKeyChange={onAdminKeyChange}
          loading={loading}
          hotels={hotels}
          hotelId={hotelId}
          onHotelChange={changeHotel}
          selectedHotel={selectedHotel}
          roomTypeId={roomTypeId}
          onRoomTypeChange={setRoomTypeId}
          roomDraft={roomDraft}
          onRoomDraftChange={changeRoomDraft}
          baseRate={baseRate}
          onBaseRateChange={setBaseRate}
          isNew={isNew}
          saving={saving}
          message={message}
          error={error}
          selectedRoom={selectedRoom}
          onSave={saveRoom}
          onStartNew={startNewRoom}
          onCancelNew={cancelNewRoom}
          onRemove={() => setRemoveTarget(selectedRoom?.id ?? null)}
        />
        <aside className="booking-summary admin-note">
          <p className="eyebrow">Internal operations</p>
          <h2 className="summary-hotel">Inventory is date based</h2>
          <p className="detail-description">The service synchronizes room capacity across future dates. The reservation service allows up to 10% overbooking and protects concurrent bookings with PostgreSQL version checks.</p>
          <p className="summary-policy"><strong>Local demo only.</strong> Keep the staff key private and use it only for authorized hotel updates.</p>
        </aside>
      </div>
      <StaffRemovalDialog
        open={removeTarget !== null}
        onCancel={() => setRemoveTarget(null)}
        onConfirm={removeRoomType}
      />
    </section>
  );
}

interface StaffEditorProps {
  readonly adminKey: string;
  readonly onAdminKeyChange: (value: string) => void;
  readonly loading: boolean;
  readonly hotels: Hotel[];
  readonly hotelId: string;
  readonly onHotelChange: (value: string) => void;
  readonly selectedHotel: Hotel | null;
  readonly roomTypeId: string;
  readonly onRoomTypeChange: (value: string) => void;
  readonly roomDraft: RoomTypeDraft;
  readonly onRoomDraftChange: (key: keyof RoomTypeDraft, value: string) => void;
  readonly baseRate: string;
  readonly onBaseRateChange: (value: string) => void;
  readonly isNew: boolean;
  readonly saving: boolean;
  readonly message: string;
  readonly error: string;
  readonly selectedRoom: Hotel["roomTypes"][number] | null;
  readonly onSave: (event: SubmitEvent<HTMLFormElement>) => void;
  readonly onStartNew: () => void;
  readonly onCancelNew: () => void;
  readonly onRemove: () => void;
}

type StaffRoomFormProps = Pick<
  StaffEditorProps,
  | "roomDraft"
  | "onRoomDraftChange"
  | "isNew"
  | "baseRate"
  | "onBaseRateChange"
  | "error"
  | "message"
  | "onSave"
  | "saving"
  | "loading"
  | "selectedRoom"
  | "onRemove"
  | "onStartNew"
  | "onCancelNew"
>;
type StaffRoomActionsProps = Pick<
  StaffEditorProps,
  "saving" | "loading" | "isNew" | "selectedRoom" | "onRemove" | "onStartNew" | "onCancelNew"
>;

function StaffEditor(props: StaffEditorProps) {
  return (
    <section className="admin-card" aria-label="Room type editor">
      <div className="field">
        <label htmlFor="admin-key">Staff key</label>
        <input className="input-control" id="admin-key" type="password" autoComplete="off" value={props.adminKey} onChange={(event) => props.onAdminKeyChange(event.target.value)} />
      </div>
      {props.loading ? <output className="status-message">Loading hotel details…</output> : null}
      {!props.loading && props.hotels.length === 0 && props.error ? <p className="checkout-error" role="alert">{props.error}</p> : null}
      {!props.loading && props.hotels.length > 0 ? (
        <>
          <StaffHotelFields
            hotels={props.hotels}
            hotelId={props.hotelId}
            onHotelChange={props.onHotelChange}
            selectedHotel={props.selectedHotel}
            roomTypeId={props.roomTypeId}
            onRoomTypeChange={props.onRoomTypeChange}
            isNew={props.isNew}
          />
          <StaffRoomForm
            roomDraft={props.roomDraft}
            onRoomDraftChange={props.onRoomDraftChange}
            isNew={props.isNew}
            baseRate={props.baseRate}
            onBaseRateChange={props.onBaseRateChange}
            error={props.error}
            message={props.message}
            onSave={props.onSave}
            saving={props.saving}
            loading={props.loading}
            selectedRoom={props.selectedRoom}
            onRemove={props.onRemove}
            onStartNew={props.onStartNew}
            onCancelNew={props.onCancelNew}
          />
        </>
      ) : null}
      {!props.loading && props.hotels.length === 0 ? <p className="status-message">No hotels are available to manage.</p> : null}
    </section>
  );
}

interface StaffHotelFieldsProps {
  readonly hotels: Hotel[];
  readonly hotelId: string;
  readonly onHotelChange: (value: string) => void;
  readonly selectedHotel: Hotel | null;
  readonly roomTypeId: string;
  readonly onRoomTypeChange: (value: string) => void;
  readonly isNew: boolean;
}

function StaffHotelFields(props: StaffHotelFieldsProps) {
  return (
    <>
      <div className="field">
        <label htmlFor="staff-hotel">Hotel</label>
        <select className="input-control" id="staff-hotel" value={props.hotelId} onChange={(event) => props.onHotelChange(event.target.value)}>
          {props.hotels.map((hotel) => <option value={hotel.id} key={hotel.id}>{hotel.name}</option>)}
        </select>
      </div>
      {!props.isNew ? (
        <div className="field">
          <label htmlFor="staff-room">Room type</label>
          <select className="input-control" id="staff-room" value={props.roomTypeId} onChange={(event) => props.onRoomTypeChange(event.target.value)}>
            {props.selectedHotel?.roomTypes.map((room) => <option value={room.id} key={room.id}>{room.name}</option>)}
          </select>
        </div>
      ) : null}
    </>
  );
}

function StaffRoomForm(props: StaffRoomFormProps) {
  return (
    <form className="admin-form" onSubmit={props.onSave}>
      <div className="field"><label htmlFor="room-name">Room name</label><input className="input-control" id="room-name" value={props.roomDraft.name} onChange={(event) => props.onRoomDraftChange("name", event.target.value)} required /></div>
      <div className="field"><label htmlFor="room-details">Room details</label><input className="input-control" id="room-details" value={props.roomDraft.details} onChange={(event) => props.onRoomDraftChange("details", event.target.value)} required /></div>
      <div className="admin-fields-row">
        <div className="field"><label htmlFor="room-guests">Guest capacity</label><input className="input-control" id="room-guests" type="number" min="1" max="8" value={props.roomDraft.maxGuests} onChange={(event) => props.onRoomDraftChange("maxGuests", event.target.value)} required /></div>
        <div className="field"><label htmlFor="room-inventory">Rooms in inventory</label><input className="input-control" id="room-inventory" type="number" min="1" value={props.roomDraft.totalInventory} onChange={(event) => props.onRoomDraftChange("totalInventory", event.target.value)} required /></div>
      </div>
      {props.isNew ? <div className="field"><label htmlFor="room-base-rate">Starting nightly rate (€)</label><input className="input-control" id="room-base-rate" type="number" min="1" step="0.01" value={props.baseRate} onChange={(event) => props.onBaseRateChange(event.target.value)} required /></div> : null}
      {props.error ? <p className="checkout-error" role="alert">{props.error}</p> : null}
      {props.message ? <output className="admin-success">{props.message}</output> : null}
      <StaffRoomActions
        saving={props.saving}
        loading={props.loading}
        isNew={props.isNew}
        selectedRoom={props.selectedRoom}
        onRemove={props.onRemove}
        onStartNew={props.onStartNew}
        onCancelNew={props.onCancelNew}
      />
    </form>
  );
}

function StaffRoomActions(props: StaffRoomActionsProps) {
  return (
    <div className="admin-actions">
      <button className="button button-primary" type="submit" disabled={props.saving || props.loading}>{saveLabel(props.saving, props.isNew)}</button>
      {!props.isNew && props.selectedRoom ? <button className="button button-danger" type="button" onClick={props.onRemove}>Remove room type</button> : null}
      {!props.isNew ? <button className="text-button" type="button" onClick={props.onStartNew}>Add another room type</button> : <button className="text-button" type="button" onClick={props.onCancelNew}>Cancel</button>}
    </div>
  );
}

interface StaffRemovalDialogProps {
  readonly open: boolean;
  readonly onCancel: () => void;
  readonly onConfirm: () => void;
}

function StaffRemovalDialog({ open, onCancel, onConfirm }: StaffRemovalDialogProps) {
  return (
    <dialog open={open} className="staff-dialog" aria-labelledby="remove-room-title">
      {open ? <>
        <p className="eyebrow">Staff action</p>
        <h2 id="remove-room-title">Remove this room type?</h2>
        <p>This hides it from new searches. Existing reservations remain in history.</p>
        <div className="dialog-actions">
          <button className="button button-secondary" type="button" onClick={onCancel}>Keep room type</button>
          <button className="button button-danger" type="button" onClick={onConfirm}>Remove room type</button>
        </div>
      </> : null}
    </dialog>
  );
}
