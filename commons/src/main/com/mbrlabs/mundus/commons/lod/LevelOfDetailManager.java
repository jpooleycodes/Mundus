package com.mbrlabs.mundus.commons.lod;

/**
 * An interface for managing Level of Detail (LoD) for an object.
 * @author JamesTKhan
 * @version September 30, 2023
 */
public interface LevelOfDetailManager {

    /**
     * Updates the LoD.
     */
    void update(float delta);

    /**
     * Marks the LoD as dirty, indicating a
     */
    void markDirty();

    /**
     * Enables LoD.
     */
    void enable();

    /**
     * Disables LoD. Implementations should set the LoD to the lowest (base) level.
     * when disabled.
     */
    void disable();
}
