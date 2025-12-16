package com.threedfly.orderservice.service.slicer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for selecting the appropriate SlicerService implementation
 * based on the configured slicer type.
 * Follows the same pattern as PaymentProviderFactory.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlicerServiceFactory {

    private final List<SlicerService> slicerServices;

    /**
     * Gets the appropriate slicer service for the specified slicer type.
     *
     * @param slicerType The slicer type identifier (e.g., "prusa", "bambu")
     * @return The matching SlicerService implementation
     * @throws IllegalArgumentException if no slicer service supports the given type
     */
    public SlicerService getSlicer(String slicerType) {
        if (slicerType == null || slicerType.isBlank()) {
            throw new IllegalArgumentException("Slicer type cannot be null or empty");
        }

        log.debug("🔍 Looking for slicer service that supports type: {}", slicerType);

        return slicerServices.stream()
                .filter(service -> service.supports(slicerType))
                .findFirst()
                .orElseThrow(() -> {
                    String availableSlicers = slicerServices.stream()
                            .map(SlicerService::getSlicerName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("none");

                    log.error("❌ No slicer service found for type: {}. Available slicers: {}",
                            slicerType, availableSlicers);

                    return new IllegalArgumentException(
                            String.format("No slicer service found for type '%s'. Available slicers: %s",
                                    slicerType, availableSlicers)
                    );
                });
    }
}
