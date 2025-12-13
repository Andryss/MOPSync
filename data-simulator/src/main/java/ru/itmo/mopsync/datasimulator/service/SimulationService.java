package ru.itmo.mopsync.datasimulator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecRequest;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecResponse;

/**
 * Service for managing simulation lifecycle.
 * Orchestrates state management and coordinates between components.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService implements DisposableBean {

    private final SimulationStateManager stateManager;

    /**
     * Sets the simulation specification, resolving templates and creating devices.
     *
     * @param request simulation specification request
     * @return response with created device groups
     */
    public SimulationSpecResponse setSimulationSpec(SimulationSpecRequest request) {
        stateManager.lock();
        try {
            stateManager.resetState();
            return stateManager.createDevicesFromSpec(request);
        } finally {
            stateManager.unlock();
        }
    }

    /**
     * Starts the simulation by scheduling data generation tasks for all devices.
     */
    public void startSimulation() {
        stateManager.lock();
        try {
            stateManager.validateCanStart();
            stateManager.startSimulation();
        } finally {
            stateManager.unlock();
        }
    }

    /**
     * Stops the simulation by cancelling all scheduled tasks and clearing devices.
     */
    public void stopSimulation() {
        stateManager.lock();
        try {
            stateManager.stopSimulation();
        } finally {
            stateManager.unlock();
        }
    }

    @Override
    public void destroy() {
        stopSimulation();
    }
}

