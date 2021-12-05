# Android face recognition

BSc Thesis on Gdansk University of Technology.

Android app implementing modern face recognition algorithm based on neural networks with simple preview and tool for comparing evaluation time of the selected models.

Addition of new face | Recognition in real-time camera preview
--- | ---
<img src="https://i.imgur.com/xzlQt1r.jpg" height="500"> |<img src="https://i.imgur.com/oFihtiq.jpg" height="500">
## App download

To download the target ".apk" file, go to [releases](https://github.com/DominikKwiatkowski/FaceRecognition/releases/tag/v1.0.0). App is compatible with Android OS 8.0 and above.

## Project download

To download and modify the project according to your needs, use `git clone https://github.com/DominikKwiatkowski/FaceRecognition` and open it with Android Studio. It will download all required libraries through Gradle.

## Addition of new model

To add new face recognition model, add it with `.tflite` extension to `/app/src/main/ml` directory like presented below:

To make the model visible in the app options, add new entry to `/app/src/main/res/values/arrays.xml`, like presented below:
<img src="https://i.imgur.com/J6e6PXs.png" height="500">

Norm is algorithm used to calculate distance between feature vectors generated by the model.
Threshold is the maximum distance between two vectors representing features of the same face.