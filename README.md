# SnapScrollView
Its a custom ScrollView which supports snapping the view to the top for vertical scroll and to center for horizontal scroll.

Custom viewGroup with scroll which snaps the view to start(top) of each child.

The child or inner views can be directly added to the parent. To set the margins of the child, use "setChildMargins(int,int,int,int)" or separately add each margin by using "childTopMargin(int)","childLeftMargin(int)",childBottomMargin(int)",childRightMargin(int)".

For Vertical Snapping of ScrollView, use the View "SnapScrollView" and for Horizontal snapping of ScrollView, use "HorizontalSnapScrollView".
 
To use inside the app just add the following to your root build.gradle
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  ```
  Then add the following to your dependency and compile
  ```
  dependencies {
	        compile 'com.github.santhoshkumar2794:SnapScrollView:1.0.0'
	}
```
