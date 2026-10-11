package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import java.util.List;

/** F-PUBPROD-R005, inv. 3: el veredicto con que se publicó, si la compuerta pasó y los pisos sin decidir. */
@JsonPropertyOrder({"kind", "gatePassed", "undecidableFloors", "brokenFloorGoalIds", "detail"})
public record ManifestVerdict(VerdictKind kind, boolean gatePassed, List<String> undecidableFloors,
        List<String> brokenFloorGoalIds, String detail) {
}
