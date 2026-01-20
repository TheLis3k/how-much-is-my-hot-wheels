package pl.thelis3k.howmuchismyhotwheels.valuation.dto;

public enum ValuationStatus {
    FRESH,              // Dane są aktualne (< 7 dni)
    STALE_UPDATING,     // Dane są stare, w tle trwa aktualizacja
    NOT_FOUND_UPDATING, // Brak danych, w tle trwa pierwsza wycena
    NOT_FOUND           // Brak danych i nie udało się ich pobrać (np. błąd)

}