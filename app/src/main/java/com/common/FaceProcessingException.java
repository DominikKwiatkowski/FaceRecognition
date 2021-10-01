package com.common;

/**
 * Custom exception for face processing methods.
 */
public class FaceProcessingException extends Exception {
    public static final int NO_FACES = 1;
    public static final int MORE_THAN_ONE_FACE = 2;

    private final int code;

    /**
     * Class constructor.
     *
     * @param code type of error.
     */
    public FaceProcessingException(int code) {
        super("Wrong number of faces in image");
        this.code = code;
    }
}
