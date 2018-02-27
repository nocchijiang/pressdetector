# PressDetector

[![Release](https://jitpack.io/v/nocchijiang/pressdetector.svg)](https://jitpack.io/#nocchijiang/pressdetector)

A `FrameLayout` that is able to detect changes of pressed state for any of its direct or indirect children. You can do whatever you want to the `View` being pressed, such as applying a press effect by using `ViewPropertyAnimator`.

## Usage

PressDetector theoretically supports API 4+, however it haven't been tested on devices running Android OS lower than API 16.

```groovy
// build.gradle of root directory
allprojects {
  repositories {
    // add this line, if you are not using other library deployed on jitpack
    maven { url 'https://jitpack.io' }
  }
}

// build.gradle of the project where you'd like to use PressDetector
dependencies {
  // or start with 'implementation' if you are using Android Gradle Plugin newer than 3.0
  compile 'com.github.nocchijiang:pressdetector:1.0.0'
}
```

* Use this layout as the root of the view hierarchy where you'd like to detect pressed state changes.
* Create an instance of `PressDetector.Callback` to handle the changes of pressed state.
* Set the `callback` via `PressDetector#addCallback(Callback)`.
* Exclude any `View` instances via `PressDetector#exclude(View)`. Once the detector finds a pressed view which is excluded, the "searching a pressed view" progress will be terminated immediately.

```java
PressDetector pd = findViewById(R.id.detector);
pd.addCallback(new PressDetector.Callback() {
  @Override
  public void onViewPressed(View view) {
    Log.d(TAG, "onViewPressed: " + view.toString());
  }

  @Override
  public void onViewUnpressed(View view) {
    Log.d(TAG, "onViewUnpressed: " + view.toString());
  }
});
```

## Implementation Detail

PressDetector finds pressed `View` by traversing the view tree starting from itself, after dispatching a touch event with `ACTION_DOWN` action. The searching progress doesn't take much time (usually less than 1ms, it mainly depends on the complexity of your layout) so you don't need to worry about the performance too much.

PressDetector regards a `View` with both `PFLAG_PREPRESSED` and `PFLAG_PRESSED` being set/unset as being pressed/unpressed, which strictly follows the same rules as the framework classes dispatch press state changes.

## Contribution

Feel free to open a pull request or issue if you would like to improve this library or find a bug.