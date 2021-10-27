package com.libs.benchmark;

/**
 * This interface determines being layout class. This classes are supposed to do part of layout job,
 * but stay in same process.
 */
public interface LayoutClassInterface {
    /**
     * Activate this layout.
     */
    void makeActive();
}
