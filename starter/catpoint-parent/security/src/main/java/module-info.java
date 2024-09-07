module com.udacity.catpoint.security {
  requires com.udacity.catpoint.image;
  requires java.desktop;
  requires miglayout.swing;
  requires com.google.common;
  requires com.google.gson;
  requires java.prefs;
  opens com.udacity.catpoint.data to com.google.gson;
}