package gal.usc.etse.es.restdemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Predición de sombra dun banco hora a hora")
public record ShadeReport(
        Long benchID,
        LocalDate date,
        @Schema(example = "Europe/Madrid") String timezone,
        @Schema(description = "Orixe dos datos de nubosidade: OPEN_METEO ou ASTRONOMICAL (só posición solar, ceo despexado)")
        Source source,
        int shadedHours,
        int sunnyHours,
        List<Hour> hours
) {
    public enum Source { OPEN_METEO, ASTRONOMICAL }

    public enum Reason { COVERED, NIGHT, LOW_SUN, OVERCAST, DIRECT_SUN }

    @Schema(description = "Estado do banco nunha hora concreta (hora local)")
    public record Hour(
            @Schema(example = "16") int hour,
            @Schema(description = "Elevación do sol en graos") double sunElevation,
            @Schema(description = "Acimut do sol en graos dende o norte") double sunAzimuth,
            @Schema(description = "Porcentaxe de ceo cuberto (null se non hai predición)") Integer cloudCover,
            boolean shaded,
            Reason reason
    ) {}
}
