from deepface import DeepFace
import tensorflow as tf


def convert_tflite(model_name: str, optimize: bool = False):
    """
    Parameters:
        model_name: face recognition or facial attribute model
            VGG-Face, Facenet, OpenFace, DeepFace, DeepID for face recognition
            Age, Gender, Emotion, Race for facial attributes
        optimize: if model should be optimized before conversion
    """
    model = DeepFace.build_model(model_name)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)

    filename = model_name + "-"
    if optimize:
        filename += "optimized"
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    else:
        filename += "regular"
    filename += ".tflite"

    tflite_model = converter.convert()

    # Save the model.
    with open(filename, 'wb') as f:
        f.write(tflite_model)

    return tflite_model


if __name__ == "__main__":
    """Convert given model to .tflite version."""
    convert_tflite("Facenet", True)
