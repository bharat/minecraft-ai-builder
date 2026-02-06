package com.aibuilder;

/**
 * Represents a single block to be placed, with relative coordinates and material.
 */
public record BlockPlacement(int x, int y, int z, String material) {
}
