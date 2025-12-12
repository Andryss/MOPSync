package ru.itmo.mopsync.datasimulator.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.mopsync.datasimulator.generated.api.SimulationApi;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecRequest;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecResponse;
import ru.itmo.mopsync.datasimulator.service.SimulationService;

/**
 * Controller for simulation endpoints.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SimulationApiImpl implements SimulationApi {

    private final SimulationService simulationService;

    /**
     * Sets the simulation specification.
     *
     * @param request simulation specification request
     * @return response with created device groups
     */
    @Override
    public SimulationSpecResponse setSimulationSpec(SimulationSpecRequest request) {
        log.info("POST /api/v1/simulation/spec - Setting simulation specification");
        return simulationService.setSimulationSpec(request);
    }

    /**
     * Starts the simulation.
     */
    @Override
    public void startSimulation() {
        log.info("POST /api/v1/simulation/start - Starting simulation");
        simulationService.startSimulation();
    }

    /**
     * Stops the simulation.
     */
    @Override
    public void stopSimulation() {
        log.info("POST /api/v1/simulation/stop - Stopping simulation");
        simulationService.stopSimulation();
    }
}

