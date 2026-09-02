/**
 * Builds a downloadable .ics file for a single all-day-ish reminder event
 * with an alarm, and triggers a save/open. Tapping the saved file on
 * iOS/macOS opens it straight into Calendar with an "Add" prompt - there's
 * no public API to target the Reminders app specifically from a website,
 * so this targets Calendar (with a notification via VALARM), which is the
 * reliable cross-platform option (also works with Google/Outlook).
 */
export function downloadCalendarReminder(title: string, description: string, dateIso: string): void {
  const ics = buildIcs(title, description, dateIso);
  const blob = new Blob([ics], { type: "text/calendar;charset=utf-8" });
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = "wherewear-paaminnelse.ics";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function buildIcs(title: string, description: string, dateIso: string): string {
  const [year, month, day] = dateIso.split("-").map(Number);
  const start = `${pad4(year)}${pad2(month)}${pad2(day)}T090000`;
  const end = `${pad4(year)}${pad2(month)}${pad2(day)}T091500`;
  const now = formatTimestamp(new Date());
  const uid = `${crypto.randomUUID()}@wherewear`;

  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Wherewear//Shopping Reminder//EN",
    "BEGIN:VEVENT",
    `UID:${uid}`,
    `DTSTAMP:${now}`,
    `DTSTART:${start}`,
    `DTEND:${end}`,
    `SUMMARY:${escapeIcsText(title)}`,
    `DESCRIPTION:${escapeIcsText(description)}`,
    "BEGIN:VALARM",
    "ACTION:DISPLAY",
    "DESCRIPTION:Reminder",
    "TRIGGER:-PT0S",
    "END:VALARM",
    "END:VEVENT",
    "END:VCALENDAR",
  ];
  return lines.join("\r\n");
}

function formatTimestamp(date: Date): string {
  return date.toISOString().replace(/[-:]/g, "").split(".")[0] + "Z";
}

function escapeIcsText(text: string): string {
  return text.replace(/\\/g, "\\\\").replace(/,/g, "\\,").replace(/;/g, "\\;");
}

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

function pad4(n: number): string {
  return String(n).padStart(4, "0");
}
