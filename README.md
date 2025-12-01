# StoreChat Application Documentation

This document provides a summary of the Android application "StoreChat", based on its `AndroidManifest.xml` file.

## Overview

The application is named "StoreChat" and uses the theme `@style/Theme.StoreChat`. It supports Right-to-Left (RTL) layouts and has backup features enabled.

## Permissions

The application requests the following permissions:

*   `android.permission.REQUEST_INSTALL_PACKAGES`: Allows requesting the installation of packages. This is likely for installing apps from within StoreChat.
*   `android.permission.READ_EXTERNAL_STORAGE`: Allows reading from external storage.
*   `android.permission.WRITE_EXTERNAL_STORAGE`: Allows writing to external storage.

**Note:** For modern Android versions, you should consider using Scoped Storage and the Storage Access Framework instead of broad storage permissions.

## Screen Density Adaptation

The app uses the `AndroidAutoSize` library for screen adaptation.
*   **Design Width:** The UI is designed based on a screen width of `570dp`.
*   **Adaptation Mode:** It uses a "mixed adaptation mode" (`autosize_is_adapting_by_default` is `false`), meaning that adaptation must be explicitly enabled for specific Activities or Fragments.

## Application Components

### Activities

*   `.MainActivity`: This is the main launcher activity of the application.
*   `.ui.detail.AppDetailActivity`: This activity likely displays the details of a specific application.
*   `.ui.search.SearchActivity`: This activity provides search functionality.
*   `.ui.download.DownloadQueueActivity`: This activity is likely used to display and manage a queue of downloads.
