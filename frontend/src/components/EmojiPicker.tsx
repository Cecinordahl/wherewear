import { useState } from "react";

// A small curated set of emoji relevant to wardrobe locations (countries,
// travel, weather) - not a full unicode emoji browser (that'd need a large
// data file/dependency this app deliberately avoids), just enough to cover
// the common cases without leaving the app to search and copy-paste one.
const EMOJI_OPTIONS = [
  "🇳🇴", "🇸🇪", "🇩🇰", "🇮🇸", "🇫🇮", "🇬🇧", "🇮🇪", "🇩🇪", "🇫🇷", "🇪🇸",
  "🇵🇹", "🇮🇹", "🇬🇷", "🇳🇱", "🇨🇭", "🇦🇹", "🇺🇸", "🇹🇭", "🇯🇵", "🇦🇺",
  "🏠", "🏡", "🏔️", "⛷️", "🏖️", "🏕️", "🌴", "🏙️", "🛳️", "✈️",
  "🚗", "⛰️", "☀️", "❄️", "🌊", "🎯", "📍", "⭐", "🧳", "👜",
];

export default function EmojiPicker({
  value,
  onChange,
  placeholder = "Emoji",
}: {
  value: string;
  onChange: (emoji: string) => void;
  placeholder?: string;
}) {
  const [open, setOpen] = useState(false);

  return (
    <div style={{ position: "relative" }}>
      <div className="row">
        <input
          type="text"
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          style={{ width: "4rem", textAlign: "center" }}
        />
        <button type="button" className="btn secondary" onClick={() => setOpen((o) => !o)}>
          {open ? "Lukk" : "Velg emoji"}
        </button>
      </div>

      {open && (
        <div
          className="card"
          style={{
            position: "absolute",
            zIndex: 10,
            marginTop: "0.25rem",
            display: "grid",
            gridTemplateColumns: "repeat(8, 1fr)",
            gap: "0.2rem",
            padding: "0.5rem",
          }}
        >
          {EMOJI_OPTIONS.map((emoji) => (
            <button
              key={emoji}
              type="button"
              onClick={() => {
                onChange(emoji);
                setOpen(false);
              }}
              style={{
                fontSize: "1.3rem",
                background: "none",
                border: "none",
                cursor: "pointer",
                padding: "0.2rem",
              }}
            >
              {emoji}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
