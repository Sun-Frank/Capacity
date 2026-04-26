package com.capics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MrpCompareAiServiceTest {

    @Mock
    private AiAgentConfigService aiAgentConfigService;

    @Test
    void analyze_WhenNoApiKey_ShouldReturnIntegratedFallbackAnalysis() {
        when(aiAgentConfigService.getRuntimeConfig())
                .thenReturn(new AiAgentConfigService.RuntimeConfig("", "https://api.openai.com/v1", "gpt-4o-mini", 45000));

        MrpCompareAiService service = new MrpCompareAiService(new ObjectMapper(), aiAgentConfigService);

        Map<String, Object> payload = Map.of(
                "viewType", "week",
                "fileLabelA", "A-202604",
                "fileLabelB", "B-202604",
                "summary", Map.of(
                        "totalQtyA", 1000,
                        "totalQtyB", 1320,
                        "totalDelta", 320,
                        "itemCount", 3
                ),
                "topItems", List.of(
                        Map.of("name", "Product X", "totalDelta", 180, "periodDeltas", List.of(60, 70, 50)),
                        Map.of("name", "Product Y", "totalDelta", -90, "periodDeltas", List.of(-40, -20, -30)),
                        Map.of("name", "Product Z", "totalDelta", 230, "periodDeltas", List.of(80, 90, 60))
                )
        );

        Map<String, Object> result = service.analyze(payload);

        assertEquals("local-rule", result.get("provider"));
        assertEquals("rule-fallback", result.get("model"));

        String analysis = String.valueOf(result.get("analysis"));
        assertTrue(analysis.contains("Integrated analysis (fallback rule mode)"));
        assertTrue(analysis.contains("Internal factory capacity risk"));
        assertTrue(analysis.contains("Inferred market risk"));
        assertTrue(analysis.contains("Inferred supply-chain risk"));
        assertTrue(analysis.contains("Conclusion:"));

        assertNotNull(result.get("externalSignals"));
        assertNotNull(result.get("internalRisk"));
    }
}
