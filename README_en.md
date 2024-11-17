# MiCTS

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;English

Trigger Circle to Search on any Android 9–15 device

*This app only aims to triggers Circle to Search and cannot handle issues that may occur after triggering successfully*

## How to Use

1. Install the latest version of the [Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox), enable auto-start, disable background restrictions, and set Google as the default assistant app


2. Install and launch MiCTS
   - If you're lucky, Circle to Search can be triggered directly without root
   - Otherwise, activate the module in LSPosed, enable `Device Spoof for Google` in the MiCTS settings, and force restart Google
   - If it still doesn't work, try clearing Google’s data, then launch Google and force restart it


3. Set up the trigger method
   - Launching MiCTS will trigger, so you can use other apps like Quick Ball, Xposed Edge, ShortX, etc., set launching MiCTS as the action to customizing the trigger method
   - MiCTS provides a trigger tile, so you can add it to the Quick Settings panel and trigger by clicking it
   - For Xiaomi devices, MiCTS has built-in support for `Trigger by long press gesture handle` and `Trigger by long press home button`, which can be enabled in the MiCTS settings (need to activate the module and restart phone after installing MiCTS)

## Settings

### How to enter Settings
- Long press the MiCTS app icon to show the Settings option, then click to enter
- From Modules page in LSPosed, click MiCTS, then click the settings icon to enter
- Long press the Quick Settings panel tile to enter

### App Settings
- Default trigger delay: The delay when triggering by launching MiCTS
- Tile trigger delay: The delay when triggering by the Quick Settings panel tile

### Module Settings
Need to activate the module in LSPosed
- System trigger service: The system service used by triggering. Only the services supported will be shown. Need to add System Framework to the scope in LSPosed
   - VIS: Supports on Android 9-15. Need to set Google as the default assistant app and the screen edge will flash when triggering. If the module is not activated, only this service will be used
   - CSHelper: Supports on Android 14 QPR3 and above. Don’t need to set Google as the default assistant app and the screen edge will not flash when triggering


- Trigger by long press gesture handle: Only supports on Xiaomi devices. Need to add System Launcher/POCO Launcher to the scope in LSPosed


- Trigger by long press home button: Only supports on Xiaomi devices. Need to add System Framework to the scope in LSPosed


- Device Spoof for Google: Need to add Google to the scope in LSPosed
   - Manufacturer: Modify the `ro.product.manufacturer` value that Google reads
   - Brand: Modify the `ro.product.brand` value that Google reads
   - Model: Modify the `ro.product.model` value that Google reads
   - Device: Modify the `ro.product.device` value that Google reads