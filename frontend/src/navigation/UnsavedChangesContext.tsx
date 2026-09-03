import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";

const CONFIRM_MESSAGE = "Du har ulagrede varer fra kvitteringsimport. Vil du forlate siden uten å lagre dem?";

interface UnsavedChangesContextValue {
  setBlocked: (blocked: boolean) => void;
  confirmLeave: () => boolean;
}

const UnsavedChangesContext = createContext<UnsavedChangesContextValue | null>(null);

export function UnsavedChangesProvider({ children }: { children: ReactNode }) {
  const [isBlocked, setIsBlocked] = useState(false);

  useEffect(() => {
    if (!isBlocked) return;
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [isBlocked]);

  const setBlocked = useCallback((blocked: boolean) => setIsBlocked(blocked), []);
  const confirmLeave = useCallback(() => !isBlocked || window.confirm(CONFIRM_MESSAGE), [isBlocked]);

  return (
    <UnsavedChangesContext.Provider value={{ setBlocked, confirmLeave }}>{children}</UnsavedChangesContext.Provider>
  );
}

export function useUnsavedChanges() {
  const ctx = useContext(UnsavedChangesContext);
  if (!ctx) throw new Error("useUnsavedChanges must be used within UnsavedChangesProvider");
  return ctx;
}
