package com.wherewear.backend.model;

// ASCII names (VAR/HOST) so the enum works unescaped as a URL path segment;
// the frontend maps these to the Norwegian display labels (Vår/Høst).
public enum Season {
    VINTER,
    VAR,
    SOMMER,
    HOST
}
