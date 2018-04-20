package nick.greenwave.settings

class SettingsProvider(val view: SettingsView) : SettingsProviderApi {
    val model: SettingsModelApi = SettingsModel()

}