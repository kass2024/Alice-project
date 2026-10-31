let models;

export function loadModels() {
  if (!models) {
    models = (async () => {
      const faceapi = await import("@vladmandic/face-api");
      await faceapi.nets.tinyFaceDetector.loadFromUri("/models");
      await faceapi.nets.faceLandmark68Net.loadFromUri("/models");
      await faceapi.nets.faceRecognitionNet.loadFromUri("/models");
      return faceapi;
    })();
  }
  return models;
}

export async function describeFace(input) {
  const faceapi = await loadModels();
  const options = new faceapi.TinyFaceDetectorOptions({ inputSize: 320, scoreThreshold: 0.4 });
  const found = await faceapi.detectSingleFace(input, options).withFaceLandmarks().withFaceDescriptor();
  if (!found) {
    throw new Error("No face found. Look straight at the camera, with your face filling the frame.");
  }
  return Array.from(found.descriptor);
}
